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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ResultClassification {
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

    @RequestMapping(value="getConceptPageNum",produces="application/json; charset=UTF-8",method= RequestMethod.POST)
    public @ResponseBody
    JSONObject getConceptPageNum(HttpServletRequest request, @RequestBody String data) throws SQLException {
        String user=Login.isLogin(request);
        String field=request.getParameter("field");
        String name=request.getParameter("name");
        String concept=request.getParameter("concept");
        String select=null;
        int page_num;

        Connection conn= DBConnection.getConn();
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
}
