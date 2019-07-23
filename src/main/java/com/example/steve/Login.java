package com.example.steve;

import org.apache.ibatis.jdbc.SQL;
import org.springframework.boot.Banner;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Controller
public class Login {

    @RequestMapping(value="login.html",method = RequestMethod.GET)
    public String login(HttpServletRequest request) throws SQLException {
        if(isLogin(request)!=null){
            return "redirect:index.html";
        }
        return "login.html";
    }

    @RequestMapping(value="login.html",method = RequestMethod.POST)
    public String do_login(HttpServletRequest request,HttpServletResponse response) throws SQLException, IOException {
        Connection conn=DBConnection.getConn();
        String uid=request.getParameter("user");
        String passwd=request.getParameter("passwd");
        String check_user="select * from `user` where uid=? and passwd=?";
        PreparedStatement ptmt=conn.prepareStatement(check_user);
        ptmt.setString(1,uid);
        ptmt.setString(2,MD5Utils.MD5Encode(passwd,"utf8"));
        ResultSet rs=ptmt.executeQuery();
        if(rs.next()){
            conn.close();
            Cookie cookie = new Cookie(uid,MD5Utils.MD5Encode(passwd,"utf8"));        //把用户名和密码加入cookie
            cookie.setMaxAge(3600);  //生命周期3600s
            response.addCookie(cookie); //添加cookie
            return "redirect:index.html";
        }
        else {
            conn.close();
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.print("<script type=\"text/javascript\">alert('请检查你的用户名和密码')</script>");
            return "login.html";
        }
    }


    //未登录返回null，如果登录返回用户名
    public static String isLogin(HttpServletRequest request) throws SQLException{
        Connection conn=DBConnection.getConn();
        Cookie[] cookies=request.getCookies();
        if(cookies!=null){
            for(Cookie c:cookies){
                String check_user="select * from `user` where `uid`=? and `passwd`=?";
                PreparedStatement ptmt=conn.prepareStatement(check_user);
                ptmt.setString(1,c.getName());      //用户名
                ptmt.setString(2,c.getValue());     //MD5后的密码
                ResultSet rs=ptmt.executeQuery();
                if(rs.next()){
                    conn.close();
                    return c.getName();
                }
            }
            return null;
        }
        conn.close();
        return null;
    }
}
