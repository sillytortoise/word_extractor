package com.example.steve;

import ch.qos.logback.classic.db.names.DBNameResolver;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.ibatis.jdbc.SQL;
import org.apache.juli.logging.Log;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.io.*;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Null;
import javax.xml.transform.Result;
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
            for (; count1 <= 200; count1++) {
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
            for (; count2 <= 200; count2++) {
                if (request.getParameter("concept_name_input" + count2) != null) {
                    count_concept++;
                    String concept = request.getParameter("concept_name_input" + count2);
                    String concept_seed_list = "";
                    int count3 = 1;
                    for (; count3 <= 200; count3++) {
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
        } else {                        //修改领域信息
            //获取领域种子词
            String field_name = request.getParameter("field");
            int count1 = 1;
            String seed_list = "";
            for (; count1 <= 200; count1++) {
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
                for (; count2 <= 200; count2++) {
                    if (request.getParameter("concept_name_input" + count2) != null) {
                        count_concept++;
                        String concept = request.getParameter("concept_name_input" + count2);
                        String concept_seed_list = "";
                        int count3 = 1;
                        for (; count3 <= 200; count3++) {
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

    @RequestMapping(value="corpus.html",method = RequestMethod.POST,produces = "text/html; charset=UTF-8")
    public @ResponseBody String submit_corpus(HttpServletRequest request,HttpServletRequest response) throws IOException,SQLException {
        Connection conn=DBConnection.getConn();
        String uid=Login.isLogin(request);
        try {
            if (uid != null) {
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

                //out.print("<script>location.assign('corpus.html?field=" + request.getParameter("field")+"');</script>");
            }
            return "<script>alert('上传成功！');</script>";
        } catch(Exception e){
            e.printStackTrace();
            return "<script>alert('上传失败！');</script>";
        }


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
    public @ResponseBody JSONObject create_task(HttpServletRequest request, @RequestBody String data) throws IOException, SQLException, InterruptedException {
        Connection conn=DBConnection.getConn();
        String user=Login.isLogin(request);
        JSONObject json=new JSONObject();
        if(user!=null) {
            String field = request.getParameter("field");
            String task_type = JSONObject.parseObject(data).get("task").toString();
            String symbol;
            String files="";
            String search_task=null;
            Statement stmt1 = conn.createStatement();
            if (Integer.parseInt(task_type) == 1) {
                symbol = "领域词抽取";
                search_task="select * from " + user + "_task where domain='" + field + "' and task_name like '" + symbol + "%' order by CAST(substring(task_name,6) AS SIGNED) desc limit 1";  //选最后一条数据
            } else if (Integer.parseInt(task_type) == 2) {
                symbol = "基于百科的抽取";
                search_task="select * from " + user + "_task where domain='" + field + "' and task_name like '" + symbol + "%' order by CAST(substring(task_name,8) AS SIGNED) desc limit 1";  //选最后一条数据
            } else {
                symbol = "实体扩充";
                search_task="select * from " + user + "_task where domain='" + field + "' and task_name like '" + symbol + "%' order by CAST(substring(task_name,5) AS SIGNED) desc limit 1";  //选最后一条数据
            }
            if(symbol.equals("领域词抽取"))
                files= JSONObject.parseObject(data).get("choose").toString().replace(',', '+');
            else if(symbol.equals("基于百科的抽取") && JSONObject.parseObject(data).containsKey("choose_type"))
                files= JSONObject.parseObject(data).get("choose_type").toString().replace(',', '+');

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
            if(symbol.equals("基于百科的抽取") && !JSONObject.parseObject(data).containsKey("choose_type")){
                String select_seed="select * from `field` where `uid`='"+user+"' and `domain`='"+field+"'";
                r1=stmt1.executeQuery(select_seed);
                r1.next();
                String seed=r1.getString("seed");
                if(seed.length()==0 && !DBConnection.validateTableExist(user+"_"+field)){
                    json.put("stat",1);
                    return json;
                }

                File seed_file = new File(SteveApplication.baikedir + field + "_seed_entity.txt");
                if (!seed_file.exists()) {
                    seed_file.createNewFile();
                }
                FileWriter fw = new FileWriter(seed_file);
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(select_seed);
                rs.next();
                String seeds = rs.getString("seed");
                if (!seeds.equals("")) {
                    seeds = seeds.trim();
                    seeds = seeds.replace(' ', '\n');
                    fw.write(seeds);
                } else {
                    String select_lib = "select `entity` from `" + user + "_" + field + "` order by `point` desc limit 50";
                    rs = st.executeQuery(select_lib);
                    while (rs.next()) {
                        fw.write(rs.getString(1) + "\n");
                    }
                }
                fw.close();
                rs.close();
                st.close();

                Runtime.getRuntime().exec("sh /datamore/cc/knowledge/category.sh " + field + " " + user).waitFor();
                FileReader fr=new FileReader("/datamore/cc/corpus/"+user+"/"+field+"/mission/"+field+"_seed_type.txt");
                BufferedReader br=new BufferedReader(fr);
                String line=null;
                JSONArray array=new JSONArray();
                while((line=br.readLine())!=null){
                    JSONObject e=new JSONObject();
                    String[] l=line.split("  ");
                    e.put("type",l[0]);
                    e.put("num",l[1]);
                    array.add(e);
                }
                json.put("stat",2);
                json.put("seed_type",array);
                return json;
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
        Thread t=new Thread(new ReadItems(request.getParameter("field"),request.getParameter("name"),user));    //把条目读进缓冲区
        t.start();
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
        Thread t=new Thread(new ReadItems(request.getParameter("field"),request.getParameter("name"),user));    //把条目读进缓冲区
        t.start();
        return "result-baike.html";
    }

    @RequestMapping(value="result_data",produces="application/json;charset=UTF-8", method = RequestMethod.POST)
    public @ResponseBody JSONObject result_table(HttpServletRequest request) throws SQLException {
        String user=Login.isLogin(request);
        ArrayList<JSONObject> list = new ArrayList<>();
        JSONObject json=new JSONObject();

        Connection conn=DBConnection.getConn();
        String get_file_name="select `result` from `"+user+"_task` where domain=? and task_name=?";
        String field=request.getParameter("field");
        String name=request.getParameter("name");

        try {
            String search_item="select * from `"+user+"_"+field+"` where entity=?";
            PreparedStatement ptmt=conn.prepareStatement(get_file_name);
            ptmt.setString(1,field);
            ptmt.setString(2,name);
            ResultSet rs=ptmt.executeQuery();
            rs.next();
            String result_file=rs.getString("result");
            rs.close();
            ptmt.close();
            InputStreamReader isr=new InputStreamReader(new FileInputStream(result_file));
            BufferedReader br=new BufferedReader(isr);
            String line=null;
            int count=0;
            while((line=br.readLine())!=null && count<=10){
                String [] ss=line.split("\t");
                //如果词库表不存在，所有条目都是新的
                if (!DBConnection.validateTableExist(user + "_" + request.getParameter("field"))) {
                        JSONObject item = new JSONObject();
                        item.put("point", Double.parseDouble(ss[0]));
                        item.put("entity", ss[1]);
                        item.put("selected", false);
                        item.put("isnew", true);
                        list.add(item);
                } else {        //词库表存在
                    PreparedStatement ptmt1=conn.prepareStatement(search_item);
                    JSONObject item = new JSONObject();
                    item.put("point", Double.parseDouble(ss[0]));
                    item.put("entity", ss[1]);
                    item.put("selected", false);
                    ptmt1.setString(1,ss[1]);
                    ResultSet rs1=ptmt1.executeQuery();
                    if(rs1.next())
                        item.put("isnew", false);
                    else
                        item.put("isnew", true);
                    list.add(item);
                    rs1.close();
                    ptmt1.close();
                }
                count++;
            }


            json.put("item",list);
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
            conn.close();
        }
        return json;
    }

    @RequestMapping(value = "result.html", produces = {"text/html;charset=UTF-8;", "application/json;"}, method = RequestMethod.POST)
    public void saveEntityLib(HttpServletRequest request) throws SQLException {
        String user = Login.isLogin(request);
        String field=request.getParameter("field");
        String name=request.getParameter("name");
        int num=0;
        String table_name=null;
        if(name.charAt(0)=='领') {
            num = Integer.parseInt(name.substring(5));
            table_name=user+"_"+field+"_领_"+num;
        }
        else if(name.charAt(0)=='基') {
            num = Integer.parseInt(name.substring(7));
            table_name=user+"_"+field+"_基_"+num;
        }
        Connection conn = DBConnection.getConn();
        if (!DBConnection.validateTableExist(user + "_" + field)) {
            String add_table = "create table " + user + "_" + field + "(" +
                    "entity varchar(50) primary key," +
                    "point double,"+
                    "isselected bit)";
            Statement st = conn.createStatement();
            st.execute(add_table);
        }
        String add_lib = "insert ignore into `" + user + "_" + field + "` (`entity`,`point`,`isselected`) select entity,`point`,0 from "+table_name+" where isselected=1";
        Statement st=conn.createStatement();
        st.executeUpdate(add_lib);
        st.execute("set SQL_SAFE_UPDATES=0");
        st.executeUpdate("update "+table_name+" set isselected=0");
        st.close();
        conn.close();
    }

    @RequestMapping(value="result-select",produces = "application/json; charset=UTF-8",method=RequestMethod.POST)
    public @ResponseBody JSONObject result_select(HttpServletRequest request) throws SQLException {
        String  user=Login.isLogin(request);
        String field=request.getParameter("field");
        String name=request.getParameter("name");
        String entity=request.getParameter("entity");
        String concept=request.getParameter("concept");
        int selected=Integer.parseInt(request.getParameter("selected"));
        String table_name=null;
        String update=null;
        if(name!=null && concept!=null){          //entity extraction
            table_name=user+"_"+field+"_实_"+name.substring(4)+"_"+concept;
            update="update "+table_name+" set isselected="+selected+" where entity='"+entity+"'";
        }
        else if(name==null && concept==null){             //field lib
            table_name=user+"_"+field;
            update="update "+table_name+" set isselected="+selected+" where entity='"+entity+"'";
        }
        else if(name==null && concept!=null){             //concept_lib
            table_name=user+"_"+field+"_concept_lib";
            update="update "+table_name+" set isselected="+selected+" where concept='"+concept+"' and entity='"+entity+"'";

        }
        else if(name.charAt(0)=='领' && name!=null) {     //field extraction
            table_name = user + "_" + field + "_领_" + name.substring(5);
            update="update "+table_name+" set isselected="+selected+" where entity='"+entity+"'";
        }
        else if(name.charAt(0)=='基' && name!=null) {       //baike extraction
            table_name = user + "_" + field + "_基_" + name.substring(7);
            update="update "+table_name+" set isselected="+selected+" where entity='"+entity+"'";
        }
        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();
        st.executeUpdate(update);
        st.close();
        conn.close();
        return new JSONObject();
    }

    @RequestMapping(value = "getPage",produces = "application/json; charset=UTF-8",method = RequestMethod.POST)
    public @ResponseBody JSONObject sendPage(HttpServletRequest request,@RequestBody String data) throws SQLException, IOException {
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        String name=request.getParameter("name");
        String concept=request.getParameter("concept");
        int page=Integer.parseInt(request.getParameter("page"));
        JSONObject json=JSONObject.parseObject(data);
        JSONArray array=json.getJSONArray("item");
        JSONObject filter=json.getJSONObject("filter");
        JSONObject result_json = new JSONObject();
        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();

        String table_name=null;
        String join_table=null;
        int num=0;
        if(name.startsWith("领")){
            num=Integer.parseInt(name.substring(5));
            table_name=user+"_"+field+"_领_"+num;
            join_table=user+"_"+field;
        }
        else if(name.startsWith("基")){
            num=Integer.parseInt(name.substring(7));
            table_name=user+"_"+field+"_基_"+num;
            join_table=user+"_"+field;
        }
        else{
            table_name=user+"_"+field+"_实_"+name.substring(4)+"_"+concept;
            join_table=user+"_"+field+"_concept_lib";
        }


        //select
        String select=null;
        if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(join_table)){
            if(Double.parseDouble(filter.getString("num"))<0){
                select="select * from "+table_name+" order by point desc limit "+(page-1)*10+",10";
            }
            else if(filter.getString("rankorpoint").equals("point")){
                double point=Double.parseDouble(filter.getString("num"));
                select="select * from "+table_name+" where point>"+point+" order by point desc limit "+(page-1)*10+",10";
            }
            else if(filter.getString("rankorpoint").equals("rank")){
                int rank=(int)Math.floor(Double.parseDouble(filter.getString("num")));
                select="select * from "+table_name +" order by point desc limit "+(page-1)*10+","+((page*10)<=rank?10:rank%10);
            }
        }
        else{
            if(Double.parseDouble(filter.getString("num"))<0){
                if(name.startsWith("实"))
                    select="select * from "+table_name+" left join (select * from "+join_table+" where concept='"+concept+"') as t1 on "+table_name+".entity=t1.entity where t1.entity is null order by "+table_name+".point desc limit "+(page-1)*10+",10";
                else select="select * from "+table_name+" left join "+join_table+" on "+table_name+".entity="+join_table+".entity where "+join_table+".entity is null order by "+table_name+".point desc limit "+(page-1)*10+",10";
            }
            else if(filter.getString("rankorpoint").equals("point")){
                double point=Double.parseDouble(filter.getString("num"));
                if(name.startsWith("实"))
                    select="select * from "+table_name+" left join (select * from "+join_table+" where concept='"+concept+"') as t1 on "+table_name+".entity=t1.entity where t1.entity is null and "+table_name+".point>"+point+" order by "+table_name+".point desc limit "+(page-1)*10+",10";
                else select="select * from "+table_name+" left join "+join_table+" on "+table_name+".entity="+join_table+".entity where "+join_table+".entity is null and "+table_name+".point>"+point+" order by "+table_name+".point desc limit "+(page-1)*10+",10";

            }
            else if(filter.getString("rankorpoint").equals("rank")){
                int rank=(int)Math.floor(Double.parseDouble(filter.getString("num")));
                if(name.startsWith("实"))
                    select="select * from "+table_name+" left join (select * from "+join_table+" where concept='"+concept+"') as t1 on "+table_name+".entity=t1.entity where t1.entity is null order by "+table_name+".point desc limit "+(page-1)*10+","+((page*10)<=rank?10:rank%10);
                else select="select * from "+table_name+" left join "+join_table+" on "+table_name+".entity="+join_table+".entity where "+join_table+".entity is null order by "+table_name+".point desc limit "+(page-1)*10+","+((page*10)<=rank?10:rank%10);
            }
        }
        System.out.println(select);
        ResultSet rs=st.executeQuery(select);
        JSONArray a=new JSONArray();
        boolean flag;
        if(filter.getString("neworall").equals("new"))
            flag=true;
        else if(name.startsWith("实") && !DBConnection.validateTableExist(user+"_"+field+"_concept_lib"))
            flag=true;
        else if((name.startsWith("领") || name.startsWith("基")) && !DBConnection.validateTableExist(user+"_"+field))
            flag=true;
        else flag=false;
        while(rs.next()){
            JSONObject json_temp=new JSONObject();
            json_temp.put("entity",rs.getString(table_name+".entity"));
            json_temp.put("point",rs.getDouble(table_name+".point"));
            json_temp.put("selected",rs.getInt("isselected"));
            if(flag==true)
                json_temp.put("isnew",true);
            else{
                String search=null;
                if(name.startsWith("领") || name.startsWith("基"))
                    search="select count(*) from "+user+"_"+field+" where entity='"+rs.getString(table_name+".entity")+"'";
                else search="select count(*) from "+user+"_"+field+"_concept_lib"+" where concept='"+concept+"' and entity='"+rs.getString(table_name+".entity")+"'";

                ResultSet r=conn.createStatement().executeQuery(search);
                r.next();
                if(r.getInt("count(*)")>0)
                    json_temp.put("isnew",false);
                else json_temp.put("isnew",true);
                r.close();
            }
            a.add(json_temp);
        }
        rs.close();
        st.close();
        conn.close();
        result_json.put("item",a);
        return result_json;
    }

    @RequestMapping(value="getPageNum",produces = "application/json; charset=UTF-8",method = RequestMethod.POST)
    public @ResponseBody JSONObject getPageNum(HttpServletRequest request,@RequestBody String data) throws SQLException {
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        if(JSONObject.parseObject(data).isEmpty()) {     //fieldlib
            String table_name=user+"_"+field;
            Connection c=DBConnection.getConn();
            Statement statement=c.createStatement();
            ResultSet resultSet=statement.executeQuery("select count(*) from "+table_name);
            resultSet.next();
            int page_num=resultSet.getInt("count(*)");
            page_num=page_num%10==0?page_num/10:page_num/10+1;
            resultSet.close();
            statement.close();
            c.close();
            JSONObject result=new JSONObject();
            result.put("page_num",page_num);
            return result;
        }
        String name=request.getParameter("name");
        int num=0;
        if(name.charAt(0)=='领')
            num=Integer.parseInt(name.substring(5));
        else if(name.charAt(0)=='基')
            num=Integer.parseInt(name.substring(7));
        String table_name=user+"_"+field+"_"+name.charAt(0)+"_"+num;
        JSONObject filter=JSONObject.parseObject(data);
        JSONObject result=new JSONObject();
        JSONArray array=null;
        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();
        int page_num=0;
        String select=null;
        ResultSet rs;

        if(filter.getString("rankorpoint").equals("all") ||filter.getDouble("num")<=0){
            if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(user+"_"+field)){
                select="select count(*) from "+table_name;
                rs=st.executeQuery(select);
                rs.next();
                page_num=rs.getInt("count(*)")%10==0?rs.getInt("count(*)")/10:rs.getInt("count(*)")/10+1;
            }
            else {
                select="select count(*) from "+table_name+" left join "+user+"_"+field +" on "+table_name+".entity="+user+"_"+field+".entity where "+user+"_"+field+".entity is null";
                rs=st.executeQuery(select);
                rs.next();
                page_num=rs.getInt("count(*)")%10==0?rs.getInt("count(*)")/10:rs.getInt("count(*)")/10+1;
            }
        }
        else if(filter.getString("rankorpoint").equals("rank")){
            int rank=(int)Math.floor(filter.getInteger("num"));
            if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(user+"_"+field)){
                select="select count(*) from "+table_name;
                rs=st.executeQuery(select);
                rs.next();
                page_num=Math.min(rs.getInt("count(*)"),rank);
                page_num=page_num%10==0?page_num/10:page_num/10+1;
            }
            else{
                select="select count(*) from "+table_name+" left join "+user+"_"+field +" on "+table_name+".entity="+user+"_"+field+".entity where "+user+"_"+field+".entity is null";
                rs=st.executeQuery(select);
                rs.next();
                page_num=Math.min(rs.getInt("count(*)"),rank);
                page_num=page_num%10==0?page_num/10:page_num/10+1;
            }
        }
        else{
            double point=filter.getDouble("num");
            if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(user+"_"+field)){
                select="select count(*) from "+table_name+" where point>"+point;
                rs=st.executeQuery(select);
                rs.next();
                page_num=rs.getInt("count(*)")%10==0?rs.getInt("count(*)")/10:rs.getInt("count(*)")/10+1;
            }
            else{
                select="select count(*) from "+table_name+" left join "+user+"_"+field +" on "+table_name+".entity="+user+"_"+field+".entity where "+table_name+".point>"+point+" and "+user+"_"+field+".entity is null";
                rs=st.executeQuery(select);
                rs.next();
                page_num=rs.getInt("count(*)")%10==0?rs.getInt("count(*)")/10:rs.getInt("count(*)")/10+1;
            }
        }
        result.put("page_num",page_num);
        return result;
    }

    @RequestMapping(value="getConceptPageNum",produces="application/json; charset=UTF-8",method=RequestMethod.POST)
    public @ResponseBody JSONObject getConceptPageNum(HttpServletRequest request,@RequestBody String data) throws SQLException {
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        String name=request.getParameter("name");
        String concept=request.getParameter("concept");
        String select=null;
        int page_num;

        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();
        ResultSet rs=null;

        if(name==null){             //concept_lib
            rs=st.executeQuery("select count(*) from "+user+"_"+field+"_concept_lib where concept='"+concept+"'");
            rs.next();
            page_num=rs.getInt("count(*)");
            page_num=page_num%10==0?page_num/10:page_num/10+1;
            JSONObject result=new JSONObject();
            result.put("page_num",page_num);
            return result;
        }

        String table_name=user+"_"+field+"_实_"+Integer.parseInt(name.substring(4))+"_"+concept;
        JSONObject filter=JSONObject.parseObject(data);



        if(filter.getString("rankorpoint").equals("all") ||filter.getDouble("num")<=0){
            if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(user+"_"+field+"_concept_lib")){
                select="select count(*) from "+table_name;
                rs=st.executeQuery(select);
                rs.next();
                page_num=rs.getInt("count(*)")%10==0?rs.getInt("count(*)")/10:rs.getInt("count(*)")/10+1;
            }
            else {
                select="select count(*) from "+table_name+" left join (select entity from "+user+"_"+field +"_concept_lib where concept='"+concept+"') as t1 on "+table_name+".entity=t1.entity where t1.entity is null";
                rs=st.executeQuery(select);
                rs.next();
                page_num=rs.getInt("count(*)")%10==0?rs.getInt("count(*)")/10:rs.getInt("count(*)")/10+1;
            }
        }
        else if(filter.getString("rankorpoint").equals("rank")){
            int rank=(int)Math.floor(filter.getInteger("num"));
            if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(user+"_"+field+"_concept_lib")){
                select="select count(*) from "+table_name;
                rs=st.executeQuery(select);
                rs.next();
                page_num=Math.min(rs.getInt("count(*)"),rank);
                page_num=page_num%10==0?page_num/10:page_num/10+1;
            }
            else{
                select="select count(*) from "+table_name+" left join (select entity from "+user+"_"+field +"_concept_lib where concept='"+concept+"') as t1 on "+table_name+".entity=t1.entity where t1.entity is null";
                rs=st.executeQuery(select);
                rs.next();
                page_num=Math.min(rs.getInt("count(*)"),rank);
                page_num=page_num%10==0?page_num/10:page_num/10+1;
            }
        }
        else{
            double point=filter.getDouble("num");
            if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(user+"_"+field+"_concept_lib")){
                select="select count(*) from "+table_name+" where point>"+point;
                rs=st.executeQuery(select);
                rs.next();
                page_num=rs.getInt("count(*)")%10==0?rs.getInt("count(*)")/10:rs.getInt("count(*)")/10+1;
            }
            else{
                select="select count(*) from "+table_name+" left join (select entity from "+user+"_"+field +"_concept_lib where concept='"+concept+"') as t1 on "+table_name+".entity=t1.entity where "+table_name+".point>"+point+" and t1.entity is null";
                rs=st.executeQuery(select);
                rs.next();
                page_num=rs.getInt("count(*)")%10==0?rs.getInt("count(*)")/10:rs.getInt("count(*)")/10+1;
            }
        }
        JSONObject result=new JSONObject();
        result.put("page_num",page_num);
        return result;
    }

    @RequestMapping(value = "select_filtered",produces = "application/json; charset=UTF-8",method = RequestMethod.POST)
    public @ResponseBody JSONObject select_filtered(HttpServletRequest request,@RequestBody String data) throws SQLException {
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        String name=request.getParameter("name");
        String concept=request.getParameter("concept");
        JSONObject filter=JSONObject.parseObject(data);
        String update_sql=null;
        String table_name=null;
        String lib_name=null;
        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();
        if(name.charAt(0)=='领') {
            table_name = user + "_" + field + "_领_" + name.substring(5);
            lib_name=user+"_"+field;
        }
        else if(name.charAt(0)=='基') {
            table_name = user + "_" + field + "_基_" + name.substring(7);
            lib_name=user+"_"+field;
        }
        else {
            table_name=user+"_"+field+"_实_"+name.substring(4)+"_"+concept;
            lib_name=user+"_"+field+"_concept_lib";
        }


        if(Double.parseDouble(filter.getString("num"))<0){
            if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(lib_name)){
                update_sql="update "+table_name+" set isselected="+request.getParameter("flag");
            }
            else{
                if(name.charAt(0)=='实')
                    update_sql="update "+table_name+" set isselected="+request.getParameter("flag")+" where "+
                            table_name+".entity in (select entity from (select "+table_name+".entity from "+
                            table_name+" left join (select entity from "+user+"_"+field+"_concept_lib where concept='"+concept+"') as t1 on "+
                            table_name+".entity=t1.entity where t1.entity is null) t2)";
                else update_sql="update "+table_name+" set isselected="+request.getParameter("flag")+" where "+
                        table_name+".entity in (select entity from (select "+table_name+".entity from "+
                        table_name+" left join "+user+"_"+field+" on "+table_name+".entity="+user+"_"+field+".entity where "+
                        user+"_"+field+".entity is null) t1)";
            }
        }
        else if(filter.getString("rankorpoint").equals("rank")){
            int num=(int)Math.floor(Double.parseDouble(filter.getString("num")));
            if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(lib_name)){
                update_sql="update "+table_name+" set isselected="+request.getParameter("flag")+" order by point desc limit "+num;
            }
            else{
                if(name.charAt(0)=='实')
                    update_sql="update "+table_name+" set isselected="+request.getParameter("flag")+" where "+
                        table_name+".entity in (select entity from (select "+table_name+".entity from "+
                        table_name+" left join (select entity from "+user+"_"+field+"_concept_lib where concept='"+concept+"') as t1 on "+
                        table_name+".entity=t1.entity where t1.entity is null order by "+table_name+".point desc limit "+num+") t2)";
                else update_sql="update "+table_name+" set isselected="+request.getParameter("flag")+" where "+
                        table_name+".entity in (select entity from (select "+table_name+".entity from "+
                        table_name+" left join "+user+"_"+field+" on "+table_name+".entity="+user+"_"+field+".entity where "+
                        user+"_"+field+".entity is null order by "+table_name+".point desc limit "+num+") t1)";
            }
        }
        else if(filter.getString("rankorpoint").equals("point")){
            double num=Double.parseDouble(filter.getString("num"));
            if(filter.getString("neworall").equals("all") || !DBConnection.validateTableExist(lib_name)){
                update_sql="update "+table_name+" set isselected="+request.getParameter("flag")+" where point>"+num+" order by point";
            }
            else{
                if(name.charAt(0)=='实')
                    update_sql="update "+table_name+" set isselected="+request.getParameter("flag")+" where "+
                        table_name+".entity in (select * from (select "+table_name+".entity from "+
                        table_name+" left join (select entity from "+user+"_"+field+"_concept_lib where concept='"+concept+"')as t1 on "+
                        table_name+".entity=t1.entity where t1.entity is null and "+table_name+".point>"+num+") t2)";
                else update_sql="update "+table_name+" set isselected="+request.getParameter("flag")+" where "+
                        table_name+".entity in (select entity from (select "+table_name+".entity from "+
                        table_name+" left join "+user+"_"+field+" on "+table_name+".entity="+user+"_"+field+".entity where "+
                        user+"_"+field+".entity is null and "+table_name+".point>"+num+" order by "+table_name+".point desc) t1)";
            }
        }
        System.out.println(update_sql);
        st.execute("set SQL_SAFE_UPDATES=0");
        st.executeUpdate(update_sql);
        return new JSONObject();
    }

    @RequestMapping(value="select_table",method=RequestMethod.POST)
    public @ResponseBody JSONObject select_table(HttpServletRequest request) throws SQLException {
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        String name=request.getParameter("name");
        int flag=Integer.parseInt(request.getParameter("flag"));
        String table_name=null;

        String num=null;
        if(name.startsWith("领")) {
            table_name="_领_";
            num = name.substring(5);
        }
        else if(name.startsWith("基")) {
            table_name="_基_";
            num = name.substring(7);
        }
        else {
            table_name="_实_";
            num=name.substring(4);
        }

        table_name=user+"_"+field+table_name+num;

        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();

        if(name.startsWith("实")) {
            String table;
            ResultSet rs=st.executeQuery("select concept_word from "+user+"_"+field+"_concept");
            Statement statement=conn.createStatement();
            statement.execute("set SQL_SAFE_UPDATES=0");
            while(rs.next()){
                String c=rs.getString("concept_word");
                table=table_name + "_" + c;
                statement.executeUpdate("update "+table+" set isselected="+flag);
            }
            statement.close();
            rs.close();
        }
        else{
            st.execute("set SQL_SAFE_UPDATES=0");
            st.executeUpdate("update "+table_name+" set isselected="+flag);
        }
        st.close();
        conn.close();

        return new JSONObject();
    }

    @RequestMapping(value = "fieldlib.html", method = RequestMethod.GET)
    public String fieldlib(HttpServletRequest request, Model model) throws SQLException {
        String user=Login.isLogin(request);
        if (user == null) {
            return "redirect:login.html";
        }
        model.addAttribute("iscookies", new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name", request.getCookies()[0].getName());
        String field=request.getParameter("field");
        if(!DBConnection.validateTableExist(user+"_"+field))
            return "index.html";
        return "fieldlib.html";
    }

    @RequestMapping(value = "fieldlib", produces = "application/json;charset=UTF-8", method = RequestMethod.POST)
    public @ResponseBody JSONObject getFieldLib(HttpServletRequest request) throws SQLException {
        String user = Login.isLogin(request);
        JSONObject jsonObject = new JSONObject();
        List<JSONObject> list = new ArrayList<>();
        Connection conn = DBConnection.getConn();
        String search_lib = "select * from `" + user + "_" + request.getParameter("field") + "`";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(search_lib);
        while (rs.next()) {
            JSONObject json=new JSONObject();
            json.put("entity",rs.getString("entity"));
            json.put("selected",false);
            list.add(json);
        }
        jsonObject.put("item", list);
        st.close();
        conn.close();
        return jsonObject;
    }

    @RequestMapping(value = "getFieldLibPage",produces = "application/json; charset=UTF-8",method = RequestMethod.POST)
    public @ResponseBody JSONObject fieldLibPage(HttpServletRequest request,@RequestBody String data) throws SQLException, IOException {
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        int page=Integer.parseInt(request.getParameter("page"));
        JSONObject json=JSONObject.parseObject(data);
        JSONArray array=json.getJSONArray("item");
        JSONObject result_json = new JSONObject();
        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();
        String table_name=user+"_"+field;
        String update=null;
        st.execute("set SQL_SAFE_UPDATES=0");
        for(int i=0;i<array.size();i++){
            update="update "+table_name+" set isselected="+array.getJSONObject(i).getString("selected")+" where entity='"+array.getJSONObject(i).getString("entity")+"'";
            st.executeUpdate(update);
        }

        //select
        String select="select * from "+table_name+" order by ascii(entity) limit "+(page-1)*10+",10";
        ResultSet rs=st.executeQuery(select);
        JSONArray a=new JSONArray();
        while(rs.next()){
            JSONObject json_temp=new JSONObject();
            json_temp.put("entity",rs.getString(table_name+".entity"));
            json_temp.put("selected",rs.getInt("isselected"));
            a.add(json_temp);
        }
        rs.close();
        st.close();
        conn.close();
        result_json.put("item",a);
        return result_json;
    }

    @RequestMapping(value = "getConceptLibPage",produces = "application/json; charset=UTF-8",method = RequestMethod.POST)
    public @ResponseBody JSONObject getConceptLibPage(HttpServletRequest request,@RequestBody String data) throws SQLException {
        String user=Login.isLogin(request);
        String concept=request.getParameter("concept");
        String field=request.getParameter("field");
        int page=Integer.parseInt(request.getParameter("page"));

        JSONObject items=JSONObject.parseObject(data);
        JSONArray item_array=items.getJSONArray("item");
        JSONObject result=new JSONObject();
        JSONArray array=new JSONArray();
        Connection conn=DBConnection.getConn();
        Statement st=conn.createStatement();
        st.execute("set SQL_SAFE_UPDATES=0");

        String select="select * from "+user+"_"+field+"_concept_lib where concept='"+concept+"' order by ascii(entity) limit "+(page-1)*10+",10";
        ResultSet rs=st.executeQuery(select);
        while(rs.next()){
            JSONObject json=new JSONObject();
            json.put("entity",rs.getString("entity"));
            json.put("selected",rs.getString("isselected"));
            array.add(json);
        }
        result.put("item",array);
        rs.close();
        st.close();
        conn.close();
        return result;
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

    @RequestMapping(value="clear_selected",produces="application/json;charset=UTF-8",method=RequestMethod.POST)
    public @ResponseBody JSONObject clearSelected(HttpServletRequest request) throws SQLException{
        String user = Login.isLogin(request);
        String field = request.getParameter("field");
        Connection conn = DBConnection.getConn();
        try {
            Statement st = conn.createStatement();
            st.execute("SET SQL_SAFE_UPDATES=0");
            st.executeUpdate("delete from "+user+"_"+field+" where isselected=1");
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

    @RequestMapping(value="clear_selected_entity",produces="application/json;charset=UTF-8",method=RequestMethod.POST)
    public @ResponseBody JSONObject clearSelectedEntity(HttpServletRequest request) throws SQLException{
        String user = Login.isLogin(request);
        String field = request.getParameter("field");
        Connection conn = DBConnection.getConn();
        Statement st = conn.createStatement();
        st.execute("SET SQL_SAFE_UPDATES=0");
        String sql="delete from `"+user+"_"+field+"_concept_lib` where isselected=1";
        st.executeUpdate(sql);
        return new JSONObject();

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

    @RequestMapping(value="get_concepts",produces="application/json; charset=UTF-8",method = RequestMethod.GET)
    public @ResponseBody JSONObject get_concept_num(HttpServletRequest request) throws SQLException {
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        Connection conn=DBConnection.getConn();
        List<String> list=new ArrayList<>();
        Statement st=conn.createStatement();
        ResultSet rs=st.executeQuery("select concept_word from "+user+"_"+field+"_concept");
        while(rs.next()){
            list.add(rs.getString("concept_word"));
        }
        JSONObject result=new JSONObject();
        result.put("concepts",list);
        return result;
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
        if(rs.next()){
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
        while(rs.next()){
            String concept=rs.getString(1);
            json.put(concept,new JSONArray());
        }
        rs.close();
        st.close();
        conn.close();
        return json;
    }

    @RequestMapping(value="result-entity.html",produces="application/json;charset=UTF-8", method = RequestMethod.POST)
    public @ResponseBody JSONObject saveConceptLib(HttpServletRequest request) throws SQLException{
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        String name=request.getParameter("name");
        JSONObject json=new JSONObject();
        if(user==null){
            json.put("statu",0);
            return json;
        }
        json.put("statu",1);
        Connection conn = DBConnection.getConn();
        if (!DBConnection.validateTableExist(user + "_" +field+"_concept_lib")) {
            String add_table = "create table " + user + "_" + request.getParameter("field")+"_concept_lib" + "(" +
                    "concept varchar(50)," +
                    "entity varchar(50)," +
                    "point double,"+
                    "isselected bit,"+
                    "primary key(concept,entity))";
            Statement st = conn.createStatement();
            st.execute(add_table);
        }

        String add_lib = null;
        String update=null;
        Statement st=conn.createStatement();
        String select_concepts="select concept_word from "+user+"_"+field+"_concept";
        ResultSet rs=st.executeQuery(select_concepts);
        while(rs.next()){
            String concept=rs.getString("concept_word");
            Statement s=conn.createStatement();
            add_lib="insert ignore into "+user+"_"+field+"_concept_lib (`concept`,`entity`,`point`,`isselected`) select '"+concept+"',entity,`point`,0 from "+user+"_"+field+"_实_"+name.substring(4)+"_"+concept+" where isselected=1";
            s.executeUpdate(add_lib);
            s.execute("set SQL_SAFE_UPDATES=0");
            s.executeUpdate("update "+user+"_"+field+"_实_"+name.substring(4)+"_"+concept+" set isselected=0");
            s.close();
        }
        conn.close();
        return json;
    }

    @RequestMapping(value = "get-concept",method = RequestMethod.GET)
    public @ResponseBody JSONObject get_concept(HttpServletRequest request) throws SQLException, IOException {
        Connection conn=DBConnection.getConn();
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        String concept=request.getParameter("concept");
        String task_name=request.getParameter("name");
        JSONObject json=new JSONObject();
        JSONArray array=new JSONArray();
        String file_name=SteveApplication.rootdir+"/"+user+"/"+field+"/mission/"+task_name+"/"+field+"_"+concept+"_rank.txt";
        InputStreamReader isr=new InputStreamReader(new FileInputStream(file_name));
        BufferedReader br=new BufferedReader(isr);
        String line=null;
        boolean flag=false;
        if(!DBConnection.validateTableExist(user+"_"+field+"_concept_lib"))
            flag=true;
        while((line=br.readLine())!=null) {
            JSONObject item_json = new JSONObject();
            line = line.trim();
            String[] ss = line.split("  ");
            String entity = ss[0];
            String point = ss[1];
            item_json.put("concept", concept);
            item_json.put("point", point);
            item_json.put("entity", entity);
            item_json.put("selected", false);
            Statement st1 = conn.createStatement();
            if(flag)
                item_json.put("isnew",true);
            else {
                ResultSet r = st1.executeQuery("select * from `" + user + "_" + field + "_concept_lib` where concept='" + concept + "' and entity='" + entity + "'");
                if (r.next()) {
                    item_json.put("isnew", false);
                } else item_json.put("isnew", true);
                r.close();
            }
            st1.close();
            array.add(item_json);
        }
        isr.close();
        br.close();
        json.put("item",array);
        return json;
    }

    @RequestMapping(value="conceptlib.html",method = RequestMethod.GET)
    public String conceptlib(HttpServletRequest request,Model model) throws SQLException{
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        if(user==null){
            return "login.html";
        }
        model.addAttribute("iscookies", new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        if(!DBConnection.validateTableExist(user+"_"+field+"_concept_lib"))
            return "index.html";
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
            ResultSet concept_items=st1.executeQuery("select entity from `"+user+"_"+field+"_concept_lib` where concept='"+concept+"' order by point desc");
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
        File concept_lib=new File(SteveApplication.rootdir+"/"+user+"/"+field+"/"+field+"_"+concept+"_概念.txt");
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
            String concept=request.getParameter("concept");
            Statement st = conn.createStatement();
            st.execute("SET SQL_SAFE_UPDATES=0");
            st.execute("delete from `"+user+"_"+field+"_concept_lib` where concept='"+concept+"'");
            ResultSet rs=st.executeQuery("select * from `"+user+"_"+field+"_concept_lib`");
            if(!rs.next()){
                st.execute("DROP table `"+user+"_"+field+"_concept_lib`");
            }
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

    @RequestMapping(value="clear_concept_lib_total",produces = "application/json;charset=UTF-8",method = RequestMethod.GET)
    public @ResponseBody JSONObject clear_concept_lib_total(HttpServletRequest request){
        Connection conn=DBConnection.getConn();
        JSONObject json=new JSONObject();
        try {
            String user=Login.isLogin(request);
            String field=request.getParameter("field");
            String concept=request.getParameter("concept");
            Statement st = conn.createStatement();
            st.execute("SET SQL_SAFE_UPDATES=0");
            st.execute("DROP table `"+user+"_"+field+"_concept_lib`");
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
