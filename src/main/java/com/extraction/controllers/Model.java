package com.extraction.controllers;

import com.alibaba.fastjson.JSONObject;
import com.extraction.steve.DBConnection;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Controller
public class Model {
    @RequestMapping(value="model.html",method = RequestMethod.GET)
    public String model(HttpServletRequest request, org.springframework.ui.Model model) throws SQLException {
        if(Login.isLogin(request)==null){
            return "redirect:login.html";
        }
        model.addAttribute("iscookies",new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "model.html";
    }


    @RequestMapping(value="modify",produces = "application/json;charset=UTF-8",method = RequestMethod.GET)
    public @ResponseBody
    JSONObject get_model(HttpServletRequest request) throws SQLException{
        Connection conn= DBConnection.getConn();
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
}
