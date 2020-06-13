package com.extraction.controllers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.extraction.steve.DBConnection;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.extraction.controllers.SteveApplication.getTime;

@Controller
public class Task {
    @RequestMapping(value = "task.html",method = RequestMethod.GET)
    public String task(HttpServletRequest request, Model model) throws SQLException {
        if(Login.isLogin(request)==null){
            return "redirect:login.html";
        }
        model.addAttribute("iscookies",new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "task.html";
    }

    @RequestMapping(value="task",produces = "application/json;charset=UTF-8",method = RequestMethod.GET)
    public @ResponseBody
    JSONObject getTasks(HttpServletRequest request) throws SQLException{
        Connection conn= DBConnection.getConn();
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
}
