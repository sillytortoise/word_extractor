package com.extraction.controllers;

import com.alibaba.fastjson.JSONObject;
import com.extraction.steve.DBConnection;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.ui.Model;
import java.io.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

import static com.extraction.controllers.SteveApplication.getTime;
import static com.extraction.controllers.SteveApplication.getFileSize;

@Controller
public class Corpus {
    @RequestMapping(value="corpus.html",method = RequestMethod.GET)
    public String corpus(HttpServletRequest request, Model model) throws SQLException {
        if(Login.isLogin(request)==null){
            return "redirect:login.html";
        }
        model.addAttribute("iscookies",new ArrayList<>().add(request.getCookies()[0]));
        model.addAttribute("user_name",request.getCookies()[0].getName());
        return "corpus.html";
    }

    @RequestMapping(value="corpus.html",method = RequestMethod.POST,produces = "text/html; charset=UTF-8")
    public @ResponseBody
    String submit_corpus(HttpServletRequest request, HttpServletRequest response) throws IOException,SQLException {
        Connection conn= DBConnection.getConn();
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
    public @ResponseBody
    JSONObject getCorpus(HttpServletRequest request) throws SQLException {
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
}
