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
    public static final String rootdir = "/home/amax/IdeaProjects/corpus";
    public static final String extractdir = "/datamore/cc/entity_extraction/entity_extraction";
    public static HashMap<String, List<String>> taskPool = null;      //待执行的任务的集合 用户名 任务列表
    public static List<String> fields = null;                //正在执行的任务 用户名
    public static HashSet<String> threadPool = null;


    public static void main(String[] args) {
        SpringApplication.run(SteveApplication.class, args);
    }

    @PostConstruct
    public void Init() {
        taskPool = new HashMap<>();
        fields = new ArrayList<>();
        threadPool = new HashSet<>();
        Connection conn = DBConnection.getConn();
        String s_user = "select uid from `user`";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(s_user);
            while (rs.next()) {
                String uid = rs.getString("uid");
                List<String> user_tasks = new ArrayList<>();
                String s_task = "select * from `" + uid + "_task` where statu='正在处理' or statu='未完成' order by `task_time`";
                Statement s = conn.createStatement();
                ResultSet r_task = s.executeQuery(s_task);
                while (r_task.next()) {
                    user_tasks.add(r_task.getString("domain") + ":" + r_task.getString("task_name"));
                }
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
