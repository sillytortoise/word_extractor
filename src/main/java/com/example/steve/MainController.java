package com.example.steve;

import ch.qos.logback.classic.db.names.DBNameResolver;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.ibatis.jdbc.SQL;
import org.apache.juli.logging.Log;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Null;
import java.sql.*;
import java.util.*;
import java.util.Date;

@Controller
public class MainController {

    @RequestMapping(value="/",method = RequestMethod.GET)
    public String root(){
        return "redirect:index.html";
    }

    @RequestMapping(value="index.html",method = RequestMethod.GET)
    public String index(Model model,HttpServletRequest request) throws SQLException{
        if(Login.isLogin(request)!=null) {
            model.addAttribute("iscookies", new ArrayList<>().add(request.getCookies()[0]));
            model.addAttribute("user_name",request.getCookies()[0].getName());

        }
        else{
            model.addAttribute("iscookies", new ArrayList<>());
            model.addAttribute("user_name",new ArrayList<>());
        }
        return "index.html";
    }

    @RequestMapping(value = "field_info", produces = "application/json;charset=UTF-8",method = RequestMethod.GET)
    public @ResponseBody JSONObject getField(HttpServletRequest request) throws SQLException{
        String user=Login.isLogin(request);
        Connection conn=DBConnection.getConn();
        String search_field="select * from `field` where uid='"+user+"'";
        Statement st=conn.createStatement();
        ResultSet rs=st.executeQuery(search_field);
        JSONObject jsonObject=new JSONObject();
        List<JSONObject> list=new ArrayList<>();
        while(rs.next()){
            JSONObject json=new JSONObject();
            String field=rs.getString("domain");
            if(DBConnection.validateTableExist(user+"_"+field)){
                json.put("isextracted",true);
            }
            else{
                json.put("isextracted", false);
            }
            if(DBConnection.validateTableExist(user+"_"+field+"_concept_lib")) {
                json.put("isexpanded",true);
            }
            else{
                json.put("isexpanded",false);
            }
            json.put("fieldname", field);
            list.add(json);
        }
        jsonObject.put("field",list);
        conn.close();
        return jsonObject;
    }

    @RequestMapping(value="model.html",method = RequestMethod.GET)
    public String model(HttpServletRequest request,Model model) throws SQLException{
        if(Login.isLogin(request)==null){
            return "redirect:login.html";
        }
        model.addAttribute("iscookies",new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "model.html";
    }


    @RequestMapping(value="modify",produces = "application/json;charset=UTF-8",method = RequestMethod.GET)
    public @ResponseBody JSONObject get_model(HttpServletRequest request) throws SQLException{
        Connection conn=DBConnection.getConn();
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        if(user==null)
            return null;
        //json
        JSONObject json=new JSONObject();
        List<JSONObject> concepts=new ArrayList<>();

        String sql1="select * from `field` where `domain`='"+field+"'";
        Statement stmt1=conn.createStatement();
        ResultSet rs1=stmt1.executeQuery(sql1);
        if(rs1.next()){
            json.put("field",field);
            String[] seeds= rs1.getString("seed").trim().split(" ");
            json.put("field_seed",seeds);
        }
        stmt1.close();
        String sql2="select * from "+user+"_"+field+"_concept";
        Statement stmt2=conn.createStatement();
        ResultSet rs2=stmt2.executeQuery(sql2);
        while(rs2.next()){
            JSONObject c=new JSONObject();
            c.put("concept",rs2.getString("concept_word"));
            String[] c_seeds=rs2.getString("seed_word").split(" ");
            c.put("seeds",c_seeds);
            concepts.add(c);
        }
        json.put("concepts",concepts);
        stmt2.close();
        conn.close();
        return json;
    }

    @RequestMapping(value="model.html",method = RequestMethod.POST)
    public void build_model(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        Connection conn=DBConnection.getConn();
        String user_id=Login.isLogin(request);
        if(user_id==null)
            return;
        response.setContentType("text/html; charset=utf-8");
        PrintWriter out=response.getWriter();
        String new_field_name = request.getParameter("field_name_input");
        //领域名为空
        if (new_field_name.equals("")) {
            out.print("<script>alert('领域名不能为空！');</script>");
            return;
        }
        if (request.getParameter("field") == null) {       //新建
            //获取领域种子词
            int count1 = 1;
            String seed_list = "";
            for (; count1 <= 100; count1++) {
                if (request.getParameter("field_seed_input" + count1) != null) {
                    String seed_word = request.getParameter("field_seed_input" + count1);
                    seed_list += " " + seed_word;
                }
            }
            seed_list = seed_list.trim();
            String s2 = "insert into `field` values(?,?,?)";
            PreparedStatement p2 = conn.prepareStatement(s2);
            p2.setString(1, user_id);
            p2.setString(2, new_field_name);
            p2.setString(3, seed_list);
            int r1 = p2.executeUpdate();
            if (r1 == 0) {
                out.print("<script>alert('构建失败！');</script>");
                return;
            }
            //创建领域概念表
            String table_name = user_id + "_" + new_field_name + "_concept";
            String s3 = "create table " + table_name + "(" +
                    "concept_word varchar(50)," +
                    "seed_word varchar(200)," +
                    "unique(concept_word,seed_word)" +
                    ")";
            Statement stmt1 = conn.createStatement();
            stmt1.executeUpdate(s3);
            String s4 = "create table " + user_id + "_" + new_field_name + "_corpus(" +
                    "fname varchar(200) primary key," +
                    "fsize varchar(20)," +
                    "uptime char(19))";
            Statement stmt2 = conn.createStatement();
            stmt2.execute(s4);
            //处理概念
            int count2 = 1;
            int count_concept = 0;
            for (; count2 <= 100; count2++) {
                if (request.getParameter("concept_name_input" + count2) != null) {
                    count_concept++;
                    String concept = request.getParameter("concept_name_input" + count2);
                    String concept_seed_list = "";
                    int count3 = 1;
                    for (; count3 <= 100; count3++) {
                        if (request.getParameter("concept" + count2 + "_seed" + count3) != null) {
                            concept_seed_list += " " + request.getParameter("concept" + count2 + "_seed" + count3);
                        }
                    }
                    concept_seed_list = concept_seed_list.trim();
                    String insert_concept = "insert into " + table_name + " values(?,?)";
                    PreparedStatement p3 = conn.prepareStatement(insert_concept);
                    p3.setString(1, concept);
                    p3.setString(2, concept_seed_list);
                    int result = p3.executeUpdate();
                    if (result == 0) {
                        out.print("<script>alert('概念" + count_concept + "构建失败！');</script>");
                    }
                }
            }
            conn.close();
            new File(SteveApplication.rootdir + "/" + user_id + "/" + new_field_name + "/row_doc").mkdirs();
            out.print("<script>alert('构建成功！');</script>");
        } else {
            //获取领域种子词
            String field_name = request.getParameter("field");
            int count1 = 1;
            String seed_list = "";
            for (; count1 <= 100; count1++) {
                if (request.getParameter("field_seed_input" + count1) != null) {
                    String seed_word = request.getParameter("field_seed_input" + count1);
                    seed_list += " " + seed_word;
                }
            }
            seed_list = seed_list.trim();
            String s2 = "update `field` set `seed`=? where `uid`=? and `domain`=?";
            PreparedStatement p2 = conn.prepareStatement(s2);
            p2.setString(1, seed_list);
            p2.setString(2, user_id);
            p2.setString(3, field_name);
            int r1 = p2.executeUpdate();
            if (r1 == 0) {
                out.print("<script>alert('修改失败！');</script>");
                return;
            }
            int count2 = 1;
            int count_concept = 0;
            String table_name = user_id + "_" + field_name + "_concept";
            String stop_safe = "SET SQL_SAFE_UPDATES = 0";
            String del = "delete from " + table_name;
            try {
                //Assume a valid connection object conn
                conn.setAutoCommit(false);
                Statement stmt = conn.createStatement();
                stmt.execute(stop_safe);
                stmt.executeUpdate(del);
                for (; count2 <= 100; count2++) {
                    if (request.getParameter("concept_name_input" + count2) != null) {
                        count_concept++;
                        String concept = request.getParameter("concept_name_input" + count2);
                        String concept_seed_list = "";
                        int count3 = 1;
                        for (; count3 <= 100; count3++) {
                            if (request.getParameter("concept" + count2 + "_seed" + count3) != null) {
                                concept_seed_list += " " + request.getParameter("concept" + count2 + "_seed" + count3);
                            }
                        }
                        concept_seed_list = concept_seed_list.trim();
                        String insert_concept = "insert into " + table_name + " values(?,?)";
                        PreparedStatement p3 = conn.prepareStatement(insert_concept);
                        p3.setString(1, concept);
                        p3.setString(2, concept_seed_list);
                        int result = p3.executeUpdate();
                        if (result == 0) {
                            out.print("<script>alert('概念" + count_concept + "构建失败！');</script>");
                        }
                    }
                }

                conn.commit();
            } catch (SQLException se) {
                se.printStackTrace();
                out.print("<script>alert('概念" + count_concept + "构建失败！');</script>");
                conn.rollback();
            }

            conn.close();
            out.print("<script>alert('修改成功！');</script>");
        }
    }

    @RequestMapping(value="corpus.html",method = RequestMethod.GET)
    public String corpus(HttpServletRequest request,Model model) throws SQLException {
        if(Login.isLogin(request)==null){
            return "redirect:login.html";
        }
        model.addAttribute("iscookies",new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "corpus.html";
    }

    @RequestMapping(value="corpus.html",method = RequestMethod.POST)
    public String corpus(HttpServletRequest request, HttpServletResponse response) throws IOException,SQLException {
        Connection conn=DBConnection.getConn();
        String uid=Login.isLogin(request);
        if(uid!=null) {
            List<MultipartFile> new_files = ((MultipartHttpServletRequest) request).getFiles("corpus");
            for (MultipartFile file : new_files) {
                String name = file.getOriginalFilename();
                long size = file.getSize();
                String file_path = SteveApplication.rootdir + "/" + uid + "/" + request.getParameter("field") + "/row_doc/" + name;
                File new_file = new File(file_path);
                if (!new_file.exists()) {
                    new_file.createNewFile();
                }
                file.transferTo(new_file);
                String sd = getTime();
                String add_corpus = "insert into " + uid + "_" + request.getParameter("field") + "_corpus" + " values(?,?,?)";
                PreparedStatement ptmt = conn.prepareStatement(add_corpus);
                ptmt.setString(1, name);
                ptmt.setString(2, getFileSize(size));
                ptmt.setString(3, sd);
                ptmt.executeUpdate();
            }
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.print("<script>alert('上传成功！');</script>");
        }
        conn.close();
        return "corpus.html";
    }

    @RequestMapping(value = "row_corpus",produces = "application/json;charset=UTF-8",method = RequestMethod.GET)
    public @ResponseBody JSONObject getCorpus(HttpServletRequest request) throws SQLException {
        Connection conn=DBConnection.getConn();
        String user=Login.isLogin(request);
        if(user!=null) {
            String sql = "select * from " + user + "_" + request.getParameter("field") + "_corpus";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            JSONObject jsonObject = new JSONObject();
            List<JSONObject> list = new ArrayList<>();
            while (rs.next()) {
                JSONObject json = new JSONObject();
                json.put("fname", rs.getString("fname"));
                json.put("fsize", rs.getString("fsize"));
                json.put("time", rs.getString("uptime"));
                list.add(json);
            }
            jsonObject.put("corpus", list);
            return jsonObject;
        }
        return null;
    }

    @RequestMapping(value = "task.html",method = RequestMethod.GET)
    public String task(HttpServletRequest request,Model model) throws SQLException{
        if(Login.isLogin(request)==null){
            return "redirect:login.html";
        }
        model.addAttribute("iscookies",new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "task.html";
    }

    @RequestMapping(value="task",produces = "application/json;charset=UTF-8",method = RequestMethod.GET)
    public @ResponseBody JSONObject getTasks(HttpServletRequest request) throws SQLException{
        Connection conn=DBConnection.getConn();
        JSONObject jsonObject=new JSONObject();
        String user=Login.isLogin(request);
        if(user!=null) {
            String sql = "select * from " + user + "_task where domain='" + request.getParameter("field") + "' order by task_time desc";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            List<JSONObject> task_list = new ArrayList<>();
            while (rs.next()) {
                JSONObject task = new JSONObject();
                String files = rs.getString("corpus").replace('+', '\n');
                task.put("domain", rs.getString("domain"));
                task.put("task_name", rs.getString("task_name"));
                task.put("files", files);
                task.put("task_time", rs.getString("task_time"));
                task.put("statu", rs.getString("statu"));
                task.put("finish_time", rs.getString("finish_time"));
                task_list.add(task);
            }
            jsonObject.put("tasks", task_list);
            conn.close();
            return jsonObject;
        }
        return null;
    }

    @RequestMapping(value="task_corpus",produces = "application/json;charset=UTF-8", method = RequestMethod.GET)
    public @ResponseBody JSONObject getTaskCorpus(HttpServletRequest request) throws SQLException{
        Connection conn= DBConnection.getConn();
        String user=Login.isLogin(request);
        if(user!=null) {
            String sql = "select * from " + user + "_" + request.getParameter("field") + "_corpus";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            JSONObject jsonObject = new JSONObject();
            List<JSONObject> list = new ArrayList<>();
            while (rs.next()) {
                JSONObject json = new JSONObject();
                json.put("fname", rs.getString("fname"));
                json.put("fsize", rs.getString("fsize"));
                json.put("time", rs.getString("uptime"));
                list.add(json);
            }
            jsonObject.put("corpus", list);
            conn.close();
            return jsonObject;
        }
        return null;
    }


    @RequestMapping(value = "task.html", produces="application/json;charset=UTF-8", method = RequestMethod.POST)
    public @ResponseBody JSONObject create_task(HttpServletRequest request, @RequestBody String data) throws UnsupportedEncodingException, SQLException {
        Connection conn=DBConnection.getConn();
        String user=Login.isLogin(request);
        JSONObject json=new JSONObject();
        if(user!=null) {
            String field = request.getParameter("field");
            String files = JSONObject.parseObject(data).get("choose").toString().replace(',', '+');
            String task_type = JSONObject.parseObject(data).get("task").toString();
            String symbol;
            if (Integer.parseInt(task_type) == 1) {
                symbol = "领域词抽取";
            } else if (Integer.parseInt(task_type) == 2) {
                symbol = "基于百科的抽取";
            } else {
                symbol = "实体扩充";
            }
            String search_task = "select * from " + user + "_task where domain='" + field + "' and task_name like '" + symbol + "%' order by `task_name` desc limit 1";  //选最后一条数据
            Statement stmt1 = conn.createStatement();
            ResultSet r1 = stmt1.executeQuery(search_task);
            int num = 1;    //任务编号
            if (r1.next()) {
                if (task_type.equals("1")) {
                    num = Integer.parseInt(r1.getString("task_name").substring(5)) + 1;
                } else if (task_type.equals("2")) {
                    num = Integer.parseInt(r1.getString("task_name").substring(7)) + 1;
                } else {
                    num = Integer.parseInt(r1.getString("task_name").substring(4)) + 1;
                }
            }
            if(symbol.equals("基于百科的抽取")){
                String select_seed="select * from `field` where `uid`='"+user+"' and `domain`='"+field+"'";
                r1=stmt1.executeQuery(select_seed);
                r1.next();
                String seed=r1.getString("seed");
                if(seed.length()==0 && !DBConnection.validateTableExist(user+"_"+field)){
                    json.put("stat",1);
                    return json;
                }
                json.put("stat",2);
            }
            if(symbol.equals("实体扩充")){
                String select_concept="select * from `"+user+"_"+field+"_concept`";
                r1=stmt1.executeQuery(select_concept);
                if(!r1.next()){                 //no concepts
                    json.put("stat",3);
                    return json;
                }
                if(!DBConnection.validateTableExist(user+"_"+field)){       //no field library
                    json.put("stat",4);
                    return json;
                }
                json.put("stat",5);
            }
            String sql = "insert into " + user + "_task values(?,?,?,?,?,?,?)";
            PreparedStatement ptmt = conn.prepareStatement(sql);
            ptmt.setString(1, field);
            ptmt.setString(2, symbol + num);
            ptmt.setString(3, files);
            ptmt.setString(4, getTime());
            ptmt.setString(5, "");      //finish time
            ptmt.setString(6, "待处理");
            ptmt.setString(7, "");      //result
            ptmt.executeUpdate();
            ptmt.close();
            conn.close();
            if (SteveApplication.taskPool.containsKey(user)) {
                SteveApplication.taskPool.get(user).add(field + ":" + symbol + num);
            } else {
                SteveApplication.taskPool.put(user, new ArrayList<>());
                SteveApplication.taskPool.get(user).add(field + ":" + symbol + num);
            }
            return json;
        }
        json.put("stat",0);
        return json;
    }

    @RequestMapping(value="result.html",method = RequestMethod.GET)
    public String result(HttpServletRequest request, Model model) throws SQLException{
        if(Login.isLogin(request)==null){
            return "redirect:login.html";
        }

        String user=Login.isLogin(request);
        model.addAttribute("iscookies",new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "result.html";
    }

    @RequestMapping(value="result-baike.html",method = RequestMethod.GET)
    public String resultBaike(HttpServletRequest request, Model model) throws SQLException{
        if(Login.isLogin(request)==null){
            return "redirect:login.html";
        }

        String user=Login.isLogin(request);
        model.addAttribute("iscookies",new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "result-baike.html";
    }

    @RequestMapping(value="result_data",produces="application/json;charset=UTF-8", method = RequestMethod.POST)
    public @ResponseBody JSONObject result_table(HttpServletRequest request) throws SQLException {
        String user=Login.isLogin(request);
        ArrayList<JSONObject> list = new ArrayList<>();
        JSONObject json=new JSONObject();
        try {
            Connection conn = DBConnection.getConn();
            String search_result = "select `result` from `" + user + "_task` where `domain`='" + request.getParameter("field")
                    + "' and `task_name`='" + request.getParameter("name") + "'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(search_result);
            rs.next();
            String result_file = rs.getString("result");
            FileReader fr = new FileReader(result_file);
            BufferedReader bf = new BufferedReader(fr);
            String str;
            // 按行读取字符串
            if (!DBConnection.validateTableExist(user + "_" + request.getParameter("field"))) {        //field entity library doesn't exist
                int count=0;
                while ((str = bf.readLine()) != null && count<=100000) {
                    String[] s = str.split("\t");
                    JSONObject item = new JSONObject();
                    item.put("point", Double.parseDouble(s[0]));
                    item.put("entity", s[1]);
                    item.put("selected", false);
                    item.put("isnew", true);
                    list.add(item);
                    count++;
                }
            } else {//field entity library exists
                int count=0;
                while ((str = bf.readLine()) != null && count<=100000) {
                    String[] s = str.split("\t");
                    JSONObject item = new JSONObject();
                    item.put("point", Double.parseDouble(s[0]));
                    item.put("entity", s[1]);
                    item.put("selected", false);
                    String search_entity = "select * from `" + user + "_" + request.getParameter("field") + "` where entity='" + s[1] + "'";
                    rs = stmt.executeQuery(search_entity);
                    if (rs.next()) {                              //already in entity library
                        item.put("isnew", false);
                    } else {
                        item.put("isnew", true);
                    }
                    list.add(item);
                    count++;
                }
            }
            bf.close();
            fr.close();
            stmt.close();
            conn.close();
            json.put("item",list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
    }

    @RequestMapping(value = "result.html", produces = {"text/html;charset=UTF-8;", "application/json;"}, method = RequestMethod.POST)
    public void saveEntityLib(HttpServletRequest request, @RequestBody String s) throws SQLException {
        String user = Login.isLogin(request);
        JSONObject result = JSONObject.parseObject(s);
        Connection conn = DBConnection.getConn();
        if (!DBConnection.validateTableExist(user + "_" + request.getParameter("field"))) {
            String add_table = "create table " + user + "_" + request.getParameter("field") + "(" +
                    "entity varchar(20) primary key," +
                    "point double)";
            Statement st = conn.createStatement();
            st.execute(add_table);
        }
        String add_lib = "insert ignore into `" + user + "_" + request.getParameter("field") + "` values(?,?)";
        PreparedStatement ptmt = conn.prepareStatement(add_lib);
        JSONArray array = result.getJSONArray("item");
        for (int i = 0; i < array.size(); i++) {
            String pointstr = array.getJSONObject(i).getString("point");
            double point = Double.parseDouble(pointstr);
            String entity = array.getJSONObject(i).getString("entity");
            try {
                ptmt.setString(1, entity);
                ptmt.setDouble(2, point);
                ptmt.executeUpdate();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        ptmt.close();
        conn.close();
    }

    @RequestMapping(value = "fieldlib.html", method = RequestMethod.GET)
    public String fieldlib(HttpServletRequest request, Model model) throws SQLException {
        if (Login.isLogin(request) == null) {
            return "redirect:login.html";
        }
        model.addAttribute("iscookies", new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name", request.getCookies()[0].getName());
        return "fieldlib.html";
    }

    @RequestMapping(value = "fieldlib", produces = "application/json;charset=UTF-8", method = RequestMethod.POST)
    public @ResponseBody JSONObject getFieldLib(HttpServletRequest request) throws SQLException {
        String user = Login.isLogin(request);
        JSONObject jsonObject = new JSONObject();
        List<String> list = new ArrayList<>();
        Connection conn = DBConnection.getConn();
        String search_lib = "select * from `" + user + "_" + request.getParameter("field") + "`";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(search_lib);
        while (rs.next()) {
            list.add(rs.getString("entity"));
        }
        jsonObject.put("item", list);
        st.close();
        conn.close();
        return jsonObject;
    }

    @RequestMapping(value="clearlib",produces="application/json;charset=UTF-8",method=RequestMethod.GET)
    public @ResponseBody JSONObject clearLib(HttpServletRequest request) throws SQLException{
        String user = Login.isLogin(request);
        String field = request.getParameter("field");
        Connection conn = DBConnection.getConn();
        try {
            Statement st = conn.createStatement();
            st.execute("SET SQL_SAFE_UPDATES=0");
            st.execute("drop table `" + user + "_" + request.getParameter("field") + "`");
            st.close();
            conn.close();
            JSONObject json=new JSONObject();
            json.put("statu",1);
            return json;
        } catch(Exception e){
            e.printStackTrace();
            conn.close();
            JSONObject json=new JSONObject();
            json.put("statu",0);
            return json;
        }

    }

    @GetMapping(value = "/download/fieldlib")
    public void downloadFile(HttpServletRequest request,HttpServletResponse response) throws SQLException,IOException{
        String user= Login.isLogin(request);
        JSONObject json=new JSONObject();
        if(user==null){
            json.put("statu",0);
            //return json;
        }
        String field=request.getParameter("field");
        File field_lib=new File(SteveApplication.rootdir+"/"+user+"/"+field+"/domain_entity_dictionary");
        if(!field_lib.exists()){
            field_lib.createNewFile();
        }
        FileWriter fw=new FileWriter(field_lib);
        Connection conn=DBConnection.getConn();
        String select_entity="select entity from `"+user+"_"+field+"` order  by point desc";
        Statement st=conn.createStatement();
        ResultSet rs=st.executeQuery(select_entity);
        while(rs.next()){
            fw.write(rs.getString(1)+"\n");
        }
        fw.close();
        st.close();
        conn.close();
        response.setHeader("content-type", "application/octet-stream");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(field+"_领域词.txt", "UTF-8"));
        byte[] buffer=new byte[1024];
        FileInputStream fis=null;
        BufferedInputStream bis=null;
        try{
            fis=new FileInputStream(field_lib);
            bis=new BufferedInputStream(fis);
            OutputStream os=response.getOutputStream();
            int i=bis.read(buffer);
            while(i!=-1){
                os.write(buffer,0,i);
                i=bis.read(buffer);
            }
            json.put("statu",1);
            //return json;
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        json.put("statu",2);
        //return json;
    }

    @RequestMapping(value = "result-entity.html",method = RequestMethod.GET)
    public String entity_result(HttpServletRequest request, Model model) throws SQLException{
        String user=Login.isLogin(request);
        if(user==null){
            return "login.html";
        }
        model.addAttribute("iscookies", new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "result-entity.html";
    }

    @RequestMapping(value = "result-data-entity",produces = "application/json; charset=UTF-8",method = RequestMethod.POST)
    public @ResponseBody JSONObject result_data_entity(HttpServletRequest request) throws SQLException, IOException {
        String user=Login.isLogin(request);
        JSONObject json=new JSONObject();
        if(user==null){                     //need login
            json.put("statu",0);
            return json;
        }
        json.put("statu",1);
        String field=request.getParameter("field");
        String task_name=request.getParameter("name");
        boolean flag=true;
        if(!DBConnection.validateTableExist(user+"_"+field+"_concept_lib")) {
            flag=false;
        }
        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();
        ResultSet rs=st.executeQuery("select concept_word from `"+user+"_"+field+"_concept`");
        while(rs.next()){
            String concept=rs.getString(1);
            json.put(concept,new JSONArray());
            File concept_file=new File(SteveApplication.rootdir+"/"+user+"/"+field+"/mission/"+task_name+"/"+field+"_"+concept+"_rank.txt");
            FileInputStream fin=new FileInputStream(concept_file);
            InputStreamReader isr=new InputStreamReader(fin);
            BufferedReader br=new BufferedReader(isr);
            String line=null;
            while((line=br.readLine())!=null){
                JSONObject item_json=new JSONObject();
                line=line.trim();
                String[] ss=line.split("  ");
                String entity=ss[0];
                String point=ss[1];
                item_json.put("concept",concept);
                item_json.put("point",point);
                item_json.put("entity",entity);
                item_json.put("selected",false);
                if(flag==false) {
                    item_json.put("isnew", true);
                }
                else{
                    Statement st1=conn.createStatement();
                    ResultSet r=st1.executeQuery("select * from `"+user+"_"+field+"_concept_lib` where concept='"+concept+"' and entity='"+entity+"'");
                    if(r.next()){
                        item_json.put("isnew",false);
                    }
                    else item_json.put("isnew",true);
                    st1.close();
                }
                json.getJSONArray(concept).add(item_json);
            }
        }
        st.close();
        conn.close();
        return json;
    }

    @RequestMapping(value="result-entity.html",produces="application/json;charset=UTF-8", method = RequestMethod.POST)
    public @ResponseBody JSONObject saveConceptLib(HttpServletRequest request,@RequestBody String data) throws SQLException{
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        String task_name=request.getParameter("name");
        JSONObject json=new JSONObject();
        if(user==null){
            json.put("statu",0);
            return json;
        }
        json.put("statu",1);
        Connection conn = DBConnection.getConn();
        if (!DBConnection.validateTableExist(user + "_" +field+"_concept_lib")) {
            String add_table = "create table " + user + "_" + request.getParameter("field")+"_concept_lib" + "(" +
                    "concept varchar(20)," +
                    "entity varchar(20)," +
                    "point double,"+
                    "primary key(concept,entity))";
            Statement st = conn.createStatement();
            st.execute(add_table);
        }
        String add_lib = "insert ignore into `" + user + "_" + request.getParameter("field") + "_concept_lib` values(?,?,?)";
        PreparedStatement ptmt = conn.prepareStatement(add_lib);
        JSONObject result=JSONObject.parseObject(data);
        JSONArray array = result.getJSONArray("item");
        for (int i = 0; i < array.size(); i++) {
            String concept=array.getJSONObject(i).getString("concept");
            String pointstr = array.getJSONObject(i).getString("point");
            double point = Double.parseDouble(pointstr);
            String entity = array.getJSONObject(i).getString("entity");
            try {
                ptmt.setString(1, concept);
                ptmt.setString(2, entity);
                ptmt.setDouble(3, point);
                ptmt.executeUpdate();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        ptmt.close();
        conn.close();
        return json;
    }

    @RequestMapping(value="conceptlib.html",method = RequestMethod.GET)
    public String conceptlib(HttpServletRequest request,Model model) throws SQLException{
        String user=Login.isLogin(request);
        if(user==null){
            return "login.html";
        }
        model.addAttribute("iscookies", new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "conceptlib.html";
    }

    @RequestMapping(value = "conceptlib",produces = "application/json;charset=UTF-8",method = RequestMethod.POST)
    public @ResponseBody JSONObject getConceptLib(HttpServletRequest request) throws SQLException {
        String user=Login.isLogin(request);
        JSONObject json=new JSONObject();
        if(user==null){                     //need login
            json.put("statu",0);
            return json;
        }
        json.put("statu",1);
        String field=request.getParameter("field");
        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();
        ResultSet rs=st.executeQuery("select distinct `concept` from `"+user+"_"+field+"_concept_lib`");
        while(rs.next()){
            String concept=rs.getString(1);
            json.put(concept,new JSONArray());
            Statement st1=conn.createStatement();
            ResultSet concept_items=st1.executeQuery("select entity from `"+user+"_"+field+"_concept_lib` where concept='"+concept+"'");
            while(concept_items.next()){
                JSONObject item=new JSONObject();
                item.put("entity",concept_items.getString("entity"));
                json.getJSONArray(concept).add(item);
            }
            st1.close();
        }
        st.close();
        conn.close();
        return json;
    }

    @GetMapping(value = "/download/conceptlib")
    public void downloadConcept(HttpServletRequest request,HttpServletResponse response) throws SQLException,IOException{
        String user= Login.isLogin(request);
        JSONObject json=new JSONObject();
        if(user==null){
            json.put("statu",0);
            //return json;
        }
        String field=request.getParameter("field");
        String concept=request.getParameter("concept");
        File concept_lib=new File(SteveApplication.rootdir+"/"+user+"/"+field+"/"+concept+"_概念.txt");
        if(!concept_lib.exists()){
            concept_lib.createNewFile();
        }
        FileWriter fw=new FileWriter(concept_lib);
        Connection conn=DBConnection.getConn();
        String select_entity=null;
        if(concept.equals("全选")){
            select_entity="select entity from `"+user+"_"+field+"_concept_lib`";
        }
        else{
            select_entity="select entity from `"+user+"_"+field+"_concept_lib` where concept='"+concept+"'";
        }
        Statement st=conn.createStatement();
        ResultSet rs=st.executeQuery(select_entity);
        while(rs.next()){
            fw.write(rs.getString(1)+"\n");
        }
        fw.close();
        st.close();
        conn.close();
        response.setHeader("content-type", "application/octet-stream");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(concept_lib.getName(), "UTF-8"));
        byte[] buffer=new byte[1024];
        FileInputStream fis=null;
        BufferedInputStream bis=null;
        try{
            fis=new FileInputStream(concept_lib);
            bis=new BufferedInputStream(fis);
            OutputStream os=response.getOutputStream();
            int i=bis.read(buffer);
            while(i!=-1){
                os.write(buffer,0,i);
                i=bis.read(buffer);
            }
            json.put("statu",1);
            //return json;
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        json.put("statu",2);
        //return json;
    }

    @RequestMapping(value="clear_concept_lib",produces = "application/json;charset=UTF-8",method = RequestMethod.GET)
    public @ResponseBody JSONObject clear_concept_lib(HttpServletRequest request){
        Connection conn=DBConnection.getConn();
        JSONObject json=new JSONObject();
        try {
            String user=Login.isLogin(request);
            String field=request.getParameter("field");
            Statement st = conn.createStatement();
            st.execute("SET SQL_SAFE_UPDATES=0");
            st.execute("DROP TABLE `"+user+"_"+field+"_concept_lib`");
            st.close();
            conn.close();
            json.put("statu",1);
            return json;
        } catch(SQLException e){
            e.printStackTrace();
            json.put("statu",0);
            return json;
        }
    }

    public String getFileSize(long size_str){
        String size = "";
        DecimalFormat df = new DecimalFormat("#.00");
        if (size_str < 1024) {
            size = df.format((double) size_str) + "B";
        } else if (size_str < 1048576) {
            size = df.format((double) size_str / 1024) + "KB";
        } else if (size_str < 1073741824) {
            size = df.format((double) size_str / 1048576) + "MB";
        } else {
            size = df.format((double) size_str / 1073741824) +"GB";
        }
        return size;
    }

    public static String getTime() {
        Long time_stamp=System.currentTimeMillis();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(Long.parseLong(String.valueOf(time_stamp))));      // 时间戳转换成时间
    }
}
