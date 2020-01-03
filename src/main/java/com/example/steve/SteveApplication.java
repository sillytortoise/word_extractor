package com.example.steve;

import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;


@Controller
@SpringBootApplication
public class SteveApplication {
    public static final String rootdir = "/datamore/cc/corpus";
    public static final String extractdir = "/datamore/cc/entity_extraction";
    public static final String baikedir="/datamore/cc/entity_extraction/baike_extraction/";
    public static final String entitydir="/datamore/cc/entity_extraction/entity_classification/";
    public static HashMap<String, List<String>> taskPool = null;      //待执行的任务的集合 用户名 任务列表
    public static List<String> fields = null;
    public static List<String> fields_baike = null;
    public static List<String> fields_entity = null;
    public static HashMap<String, JSONObject> buffer=null;      //存储用户读取的抽取结果的数据
    public static List<String> threadPool = null;


    public static void main(String[] args) {
        SpringApplication.run(SteveApplication.class, args);
    }

    @PostConstruct
    public void Init() {
        taskPool = new HashMap<>();
        threadPool = new ArrayList<>();
        fields = new ArrayList<>();
        fields_baike=new ArrayList<>();
        fields_entity=new ArrayList<>();
        buffer=new HashMap<>();
        Connection conn = DBConnection.getConn();
        String s_user = "select uid from `user`";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(s_user);
            while (rs.next()) {
                String uid = rs.getString("uid");
                Statement stmt1=conn.createStatement();
                stmt1.execute("SET SQL_SAFE_UPDATES=0");
                String init_task = "update `" + uid + "_task` set statu='任务失败' where statu='待处理' or statu='正在处理'";
                stmt1.executeUpdate(init_task);
                stmt1.close();
            }
            stmt.close();
            conn.close();
            Thread t = new Thread(new TaskListener());
            t.start();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
