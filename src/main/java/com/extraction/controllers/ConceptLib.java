package com.extraction.controllers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.extraction.steve.DBConnection;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

@Controller
public class ConceptLib {

    @RequestMapping(value="conceptlib.html",method = RequestMethod.GET)
    public String conceptlib(HttpServletRequest request, Model model) throws SQLException{
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

        String select="select * from "+user+"_"+field+"_concept_lib where concept='"+concept+"' order by convert(entity using gbk) limit "+(page-1)*10+",10";
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

    @GetMapping(value = "/download/conceptlib")
    public void downloadConcept(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
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
            select_entity="select entity from `"+user+"_"+field+"_concept_lib` order by convert(entity using gbk)";
        }
        else{
            select_entity="select entity from `"+user+"_"+field+"_concept_lib` where concept='"+concept+"' order by convert(entity using gbk)";
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


    @RequestMapping(value="clear_selected_entity",produces="application/json;charset=UTF-8",method= RequestMethod.POST)
    public @ResponseBody
    JSONObject clearSelectedEntity(HttpServletRequest request) throws SQLException {
        String user = Login.isLogin(request);
        String field = request.getParameter("field");
        Connection conn = DBConnection.getConn();
        Statement st = conn.createStatement();
        st.execute("SET SQL_SAFE_UPDATES=0");
        String sql="delete from `"+user+"_"+field+"_concept_lib` where isselected=1";
        st.executeUpdate(sql);
        return new JSONObject();
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
}
