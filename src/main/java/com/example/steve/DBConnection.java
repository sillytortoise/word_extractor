package com.example.steve;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL="jdbc:mysql://localhost:3306/knowledge?useUnicode=true&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";

    public static Connection getConn() {
        String driver = "com.mysql.cj.jdbc.Driver";
        String username = "root";
        String password = "980115";
        Connection cn=null;
        try {
            Class.forName(driver); //classLoader,加载对应驱动
            cn = (Connection) DriverManager.getConnection(URL, username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cn;
    }
}
