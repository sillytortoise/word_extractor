package com.example.steve;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


@Controller
@SpringBootApplication
public class SteveApplication {
    public static final String rootdir = "/datamore/cc/corpus";
    public static final String extractdir = "/datamore/cc/entity_extraction/entity_extraction";
    public static final String baikedir="/datamore/cc/entity_extraction/baike_extraction/";
    public static final String entitydir="/datamore/cc/entity_extraction/entity_classification/";
    public static HashMap<String, List<String>> taskPool = null;      //待执行的任务的集合 用户名 任务列表
    public static List<String> fields = null;
    public static List<String> fields_baike = null;
    public static List<String> fields_entity = null;

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
        Connection conn = DBConnection.getConn();
        String s_user = "select uid from `user`";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(s_user);
            while (rs.next()) {
                String uid = rs.getString("uid");
                List<String> user_tasks = new ArrayList<>();
                String s_task = "select * from `" + uid + "_task` where statu='待处理' order by `task_time`";
                Statement s = conn.createStatement();
                ResultSet r_task = s.executeQuery(s_task);
//                while (r_task.next()) {
//                    user_tasks.add(r_task.getString("domain") + ":" + r_task.getString("task_name"));
//                }
                s.close();
                if (user_tasks.size() > 0)
                    taskPool.put(uid, user_tasks);
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
