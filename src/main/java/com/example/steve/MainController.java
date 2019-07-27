package com.example.steve;

import ch.qos.logback.classic.db.names.DBNameResolver;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Null;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class MainController {
    public static final String rootdir="/home/steve/IdeaProjects/corpus";

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
        List<String> list=new ArrayList<>();
        while(rs.next()){
            list.add(rs.getString("domain"));
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
            String[] seeds= rs1.getString("seed").split(" ");
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
            String s2 = "update `field` set `domain`=?,`seed`=? where `uid`=? and `domain`=?";
            PreparedStatement p2 = conn.prepareStatement(s2);
            p2.setString(1, new_field_name);
            p2.setString(2, seed_list);
            p2.setString(3, user_id);
            p2.setString(4, field_name);
            int r1 = p2.executeUpdate();
            if (r1 == 0) {
                out.print("<script>alert('构建失败！');</script>");
                return;
            }
            if (!field_name.equals(new_field_name)) {        //领域名发生变化
                try {
                    //修改概念表名、语料表名
                    conn.setAutoCommit(false);
                    String s3 = "rename table `" + user_id + "_" + field_name + "_concept` to `" + user_id + "_" + new_field_name + "_concept`";
                    Statement stmt1 = conn.createStatement();
                    stmt1.execute(s3);
                    String s4 = "rename table `" + user_id + "_" + field_name + "_corpus` to `" + user_id + "_" + new_field_name + "_corpus";
                    stmt1.execute(s4);
                    conn.commit();
                    out.print("\"<script>alert('修改成功！');</script>\"");
                } catch (Exception se) {
                    conn.rollback();
                    out.print("\"<script>alert('修改失败！');</script>\"");
                }
            } else {
                int count2 = 1;
                int count_concept = 0;
                String table_name = user_id + "_" + new_field_name + "_concept";
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
                    out.print("<script>alert('概念" + count_concept + "构建失败！');</script>");
                    conn.rollback();
                }

                conn.close();
                out.print("<script>alert('修改成功！');</script>");
            }
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
                //如果以txt结尾
                if (name.endsWith(".txt")) {
                    String file_path = rootdir + "/" + uid + "/" + name;
                    File new_file = new File(file_path);
                    if (!new_file.exists()) {
                        new_file.mkdirs();
                    }
                    file.transferTo(new_file);
                }
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
            String sql = "select * from " + user + "_task where domain='" + request.getParameter("field") + "'";
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


    @RequestMapping(value = "task.html", produces={"text/html;charset=UTF-8;","application/json;"}, method = RequestMethod.POST)
    public String create_task(HttpServletRequest request,@RequestBody String data) throws UnsupportedEncodingException, SQLException {
        Connection conn=DBConnection.getConn();
        String user=Login.isLogin(request);
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
            String sql = "insert into " + user + "_task values(?,?,?,?,?,?)";
            PreparedStatement ptmt = conn.prepareStatement(sql);
            ptmt.setString(1, field);
            ptmt.setString(2, symbol + num);
            ptmt.setString(3, files);
            ptmt.setString(4, getTime());
            ptmt.setString(5, "未完成");
            ptmt.setString(6, null);
            ptmt.executeUpdate();
            ptmt.close();
            conn.close();
            return "task.html";
        }
        return "login.html";
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

    @RequestMapping(value="result_data",produces="application/json;charset=UTF-8", method = RequestMethod.POST)
    public @ResponseBody JSONObject result_table(HttpServletRequest request) throws SQLException {
        String user=Login.isLogin(request);
        ArrayList<JSONObject> list = new ArrayList<>();
        JSONObject json=new JSONObject();
        try {
            FileReader fr = new FileReader(rootdir + "/"+user+"/"+"baike_short.txt");
            BufferedReader bf = new BufferedReader(fr);
            String str;
            // 按行读取字符串
            while ((str = bf.readLine()) != null) {
                str=str.replace('\t',' ');
                String[] s = str.split(" ");
                JSONObject item=new JSONObject();
                item.put("entity",s[0]);
                item.put("point",Double.parseDouble(s[s.length-1]));
                item.put("selected",false);
                list.add(item);
            }
            bf.close();
            fr.close();
            json.put("item",list);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return json;
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

    public String getTime(){
        Long time_stamp=System.currentTimeMillis();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(Long.parseLong(String.valueOf(time_stamp))));      // 时间戳转换成时间
    }
}
