package com.extraction.controllers;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.extraction.steve.DBConnection;
import com.extraction.steve.ReadItems;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;

@Controller
public class ResultExtraction {
    @RequestMapping(value="result.html",method = RequestMethod.GET)
    public String result(HttpServletRequest request, org.springframework.ui.Model model) throws SQLException {
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
    public @ResponseBody
    JSONObject result_table(HttpServletRequest request) throws SQLException {
        String user=Login.isLogin(request);
        ArrayList<JSONObject> list = new ArrayList<>();
        JSONObject json=new JSONObject();

        Connection conn= DBConnection.getConn();
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
}
