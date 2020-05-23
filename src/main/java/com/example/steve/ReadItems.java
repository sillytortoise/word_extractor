package com.example.steve;

import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;

public class ReadItems implements Runnable{
    private String field;
    private String name;
    private String user;

    public ReadItems(){}

    public ReadItems(String f, String n, String u){
        field=f;
        name=n;
        user=u;
    }

    public void run() {
        Connection conn=DBConnection.getConn();
        String result_file;
        String get_file_name="select `result` from `"+user+"_task` where domain=? and task_name=?";

        try {
            PreparedStatement ptmt=conn.prepareStatement(get_file_name);
            ptmt.setString(1,field);
            ptmt.setString(2,name);
            ResultSet rs=ptmt.executeQuery();
            rs.next();
            result_file=rs.getString("result");
            rs.close();
            ptmt.close();

            InputStreamReader isr=new InputStreamReader(new FileInputStream(result_file));
            BufferedReader br=new BufferedReader(isr);
            JSONObject state=new JSONObject();
            JSONObject json=new JSONObject();
            ArrayList<JSONObject> items=new ArrayList<>();


            int flag;
            if (!DBConnection.validateTableExist(user + "_" + field))
                flag=0;
            else flag=1;

            String search="select * from `"+user+"_"+field+"` where entity=?";
            PreparedStatement ptmt1=conn.prepareStatement(search);

            String line=null;
            while((line=br.readLine())!=null){
                if(name.charAt(0)=='领' || name.charAt(0)=='基'){
                    String[] ss=line.split("\t");
                    JSONObject item=new JSONObject();           //单条
                    item.put("entity",ss[1]);
                    item.put("point",Double.parseDouble(ss[0]));
                    item.put("selected",false);
                    if(flag==0)
                        item.put("isnew", true);
                    else{
                        ptmt1.setString(1,ss[1]);
                        ResultSet rs1=ptmt1.executeQuery();
                        if(rs1.next())
                            item.put("isnew",false);
                        else
                            item.put("isnew",true);

                        rs1.close();
                    }
                    items.add(item);
//                    if(items.size()==0)
//                        items.add(item);
//                    else{       //插入排序
//                        int i;
//                        items.add(new JSONObject());
//                        for(i=items.size()-2;i>=0;i--){
//                            if(items.get(i).getDouble("point")<item.getDouble("point"))
//                                items.set(i+1,items.get(i));
//                            else break;
//                        }
//                        items.set(i+1,item);
//                    }
                }
                else{                   //实体扩充
                    JSONObject item=new JSONObject();           //单条
                    item.put("entity",rs.getString(1));
                    item.put("point",Double.parseDouble(rs.getString(2)));
                    item.put("selected",false);
                    items.add(item);
                }
            }
            ptmt1.close();
            isr.close();
            br.close();
            json.put("finish_load",true);


            /*默认按照分数从大到小排序*/
//            items.sort((a,b)->{
//                if(b.getDouble("point")>a.getDouble("point")){
//                    return 1;
//                }
//                else if(b.getDouble("point")==a.getDouble("point")){
//                    return 0;
//                }
//                else return -1;
//            });


        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
