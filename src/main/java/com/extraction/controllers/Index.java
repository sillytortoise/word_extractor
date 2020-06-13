package com.extraction.controllers;

import com.alibaba.fastjson.JSONObject;
import com.extraction.steve.DBConnection;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Controller
public class Index {
    @RequestMapping(value="/",method = RequestMethod.GET)
    public String root(){
        return "redirect:index.html";
    }

    @RequestMapping(value="index.html",method = RequestMethod.GET)
    public String index(Model model, HttpServletRequest request) throws SQLException {
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
    public @ResponseBody
    JSONObject getField(HttpServletRequest request) throws SQLException{
        String user=Login.isLogin(request);
        Connection conn= DBConnection.getConn();
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
}
