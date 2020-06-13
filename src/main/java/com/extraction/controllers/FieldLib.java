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
import java.util.List;

@Controller
public class FieldLib {
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
    public @ResponseBody
    JSONObject getFieldLib(HttpServletRequest request) throws SQLException {
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
        String select="select * from "+table_name+" order by convert (entity using gbk) limit "+(page-1)*10+",10";
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

    @GetMapping(value = "/download/fieldlib")
    public void downloadFile(HttpServletRequest request, HttpServletResponse response) throws SQLException,IOException{
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
        String select_entity="select entity from `"+user+"_"+field+"` order by convert(entity using gbk) collate gbk_chinese_ci asc";
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
}
