package com.example.steve;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@Controller
public class Register {
    @RequestMapping(value="register.html",method = RequestMethod.GET)
    public String register(){
        return "register.html";
    }

    @RequestMapping(value="register.html",produces ="application/json;charset=UTF-8",method = RequestMethod.POST)
    public @ResponseBody JSONObject register(@RequestBody String data) throws SQLException {
        Connection conn=DBConnection.getConn();
        String user_name=JSONObject.parseObject(data).get("user").toString();
        JSONObject json=new JSONObject();
        if(user_name.length()==0){
            json.put("error",1);
            return json;
        }
        String select_user_name="select * from `user` where uid='"+user_name+"'";
        Statement stmt=conn.createStatement();
        ResultSet rs=stmt.executeQuery(select_user_name);
        if(rs.next()){
            json.put("error",2);
            return json;
        }
        String passwd=JSONObject.parseObject(data).get("passwd").toString();
        String confirm=JSONObject.parseObject(data).get("confirm").toString();

        if(passwd.equals("")||confirm.equals("")){
            json.put("error",3);
            return json;
        }

        if(!passwd.equals(confirm)){
            json.put("error",4);
            return json;
        }

        try{
            conn.setAutoCommit(false);//开启事务
            String create_account="insert into `user` values(?,?)";
            PreparedStatement ptmt=conn.prepareStatement(create_account);
            ptmt.setString(1,user_name);
            ptmt.setString(2,MD5Utils.MD5Encode(passwd,"utf8"));
            int n=ptmt.executeUpdate();
            String create_table="create table `"+user_name+"_task`("+
                    "domain varchar(50),"+
                    "task_name varchar(50),"+
                    "corpus varchar(200),"+
                    "task_time char(19),"+
                    "finish_time char(19),"+
                    "statu varchar(5),"+
                    "result varchar(200),"+
                    "primary key(domain,task_name))";
            ptmt.execute(create_table);
            conn.commit();//try的最后提交事务
        } catch(Exception e) {
            json.put("error",5);
            conn.rollback();//回滚事务
        }
        json.put("error",0);
        return json;
    }
}
