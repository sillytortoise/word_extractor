package com.example.steve;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TaskProcessor implements Runnable {
    private String user;
    private List<String> list;

    TaskProcessor(String u, List<String> l) {
        user = u;
        list = l;
    }

    public void run() {
        Connection conn = DBConnection.getConn();
        try {
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                String task_name = it.next();
                String field = task_name.substring(0, task_name.indexOf(':'));
                String fix_statu = "update `" + user + "_task` set statu='正在处理' where task_name='" + task_name.substring(task_name.indexOf(':') + 1) + "' and domain='" + field + "'";
                Statement stmt = conn.createStatement();
                stmt.executeUpdate(fix_statu);
//                String corpus = "select * from `" + user + "_task` where `task_name`='" + task_name.substring(task_name.indexOf(':') + 1) + "'";
//                ResultSet rs = stmt.executeQuery(corpus);
//                rs.next();
//                String files = rs.getString("corpus");
//                String[] corpus_selected = files.split("[+]");
//                for (String s : corpus_selected) {
//                    if (s.endsWith(".doc") || s.endsWith(".docx")) {
//                        WordConvert.generateTxt(SteveApplication.rootdir + "/" + user + "/" + field,
//                                SteveApplication.rootdir + "/" + user + "/" + field + "/row_doc/" + s);
//                    } else if (s.endsWith(".pdf")) {
//                        PdfConvert.generateTxt(SteveApplication.rootdir + "/" + user + "/" + field,
//                                SteveApplication.rootdir + "/" + user + "/" + field + "/row_doc/" + s);
//                    } else if (s.endsWith(".zip")) {
//                        String zip_path = SteveApplication.rootdir + "/" + user + "/" + field + "/row_doc/" + s.substring(0, s.lastIndexOf('.'));   //解压出的文件夹
//                        Unzip.unzipToDest(SteveApplication.rootdir + "/" + user + "/" + field + "/row_doc/" + s, zip_path);
//                        Unrar.generateTxt(SteveApplication.rootdir + "/" + user + "/" + field, zip_path);
//                        try {
//                            String delete_folder = "rm -r " + zip_path;
//                            Runtime.getRuntime().exec(delete_folder).waitFor();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    } else if (s.endsWith(".rar")) {
//                        String rar_path = SteveApplication.rootdir + "/" + user + "/" + field + "/row_doc/" + s.substring(0, s.lastIndexOf('.'));   //解压出的文件夹
//                        Unrar.unrarToDest(SteveApplication.rootdir + "/" + user + "/" + field + "/row_doc/" + s, rar_path);
//                        Unrar.generateTxt(SteveApplication.rootdir + "/" + user + "/" + field + "/row_doc/" + s, rar_path);
//                        try {
//                            String delete_folder = "rm -r " + rar_path;
//                            Runtime.getRuntime().exec(delete_folder).waitFor();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    } else if (s.endsWith(".txt")) {
//                        try {
//                            File cpy_file = new File(SteveApplication.rootdir + "/" + user + "/" + field + "/" + s);       //建立文件副本
//                            FileInputStream fin = new FileInputStream(new File(SteveApplication.rootdir + "/" + user + "/" + field + "/row_doc/" + s));
//                            FileOutputStream fout = new FileOutputStream(cpy_file);
//                            byte[] buf = new byte[2048];
//                            int len;
//                            while ((len = fin.read(buf)) != -1) {
//                                fout.write(buf, 0, len);
//                            }
//                            fin.close();
//                            fout.close();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                Runtime.getRuntime().exec("rm -f " + SteveApplication.rootdir + "/" + user + "/" + field + "/processed.txt").waitFor();    //删除原有的文件
//                File[] txts = new File(SteveApplication.rootdir + "/" + user + "/" + field).listFiles();
//                List<File> txt_list = new ArrayList<>();
//                Collections.addAll(txt_list, txts);
//                for (File file : txt_list) {
//                    if (file.getName().endsWith(".txt")) {
//                        FileReader fr = new FileReader(file);
//                        BufferedReader br = new BufferedReader(fr);
//                        File fout = new File(SteveApplication.rootdir + "/" + user + "/" + field + "/processed.txt");
//                        if (!fout.exists()) {
//                            fout.createNewFile();
//                        }
//                        FileWriter fw = new FileWriter(fout);
//                        String line;
//                        while ((line = br.readLine()) != null) {
//                            line = line.trim();
//                            fw.append(line + "\n");
//                        }
//                        fr.close();
//                        br.close();
//                        fw.close();
//                        Runtime.getRuntime().exec("rm -f " + file.getAbsolutePath()).waitFor();    //删除原有的文件
//                    }
//                }
//
//                //创建任务目录
//                File taskFile = new File(SteveApplication.rootdir + "/" + user + "/" + field + "/mission/" + task_name.substring(task_name.indexOf(':') + 1));
//                if (!taskFile.exists()) {
//                    taskFile.mkdirs();
//                }
//
//                //检测正在执行的任务中有无与此任务同领域的
//                while (SteveApplication.fields.contains(field)) {
//                    Thread.sleep(1000);
//                }
//
//                SteveApplication.fields.add(field);         //添加本任务的领域进去
//                Runtime runtime=Runtime.getRuntime();
//                Process pro=runtime.exec("sh /home/amax/IdeaProjects/knowledge/e.sh " + SteveApplication.rootdir + "/" + user + "/" + field + "/processed.txt " + SteveApplication.extractdir + "/context_" + field + ".txt " + field);
//                pro.waitFor();
//                BufferedReader br=new BufferedReader(new InputStreamReader(pro.getInputStream()));
//                StringBuffer sb=new StringBuffer();
//                String line;
//                while((line=br.readLine())!=null){
//                    sb.append(line).append("\n");
//                }
//                System.out.println(sb.toString());
                String propagate_result = SteveApplication.extractdir + "/results/Propagate_" + field + ".txt";
                if (new File(propagate_result).exists()) {
                    String result_file = SteveApplication.rootdir + "/" + user + "/" + field + "/mission/" + task_name.substring(task_name.indexOf(':') + 1) + "/extraction_result.txt";
                    String mvCommand = "sh /home/amax/IdeaProjects/knowledge/mv.sh " + propagate_result + " " + result_file;
                    Runtime.getRuntime().exec(mvCommand).waitFor();
                    FileInputStream fin = new FileInputStream(result_file);
                    InputStreamReader isr = new InputStreamReader(fin);
                    BufferedReader br_field = new BufferedReader(isr);
                    FileWriter fw_field = new FileWriter(new File(SteveApplication.rootdir + "/" + user + "/" + field + "/mission/" + task_name.substring(task_name.indexOf(':') + 1) + "/result.txt"));
                    String line_field;
                    while ((line_field = br_field.readLine()) != null) {
                        int separator = line_field.indexOf('[');
                        String latter_part = line_field.substring(separator + 1, line_field.length() - 1);
                        String former_part = line_field.substring(0, separator);
                        String[] ss = latter_part.split(", ");
                        String latter_part_modified = "";
                        for (String s : ss) {
                            latter_part_modified += s;
                        }
                        String line_result = former_part + latter_part_modified + "\n";
                        fw_field.write(line_result);
                    }
                    fin.close();
                    isr.close();
                    br_field.close();
                    fw_field.close();
                    String update_statu = "update `" + user + "_task` set statu='已完成'," +
                            "result='" + SteveApplication.rootdir + "/" + user + "/" + field + "/missions/" + task_name.substring(task_name.indexOf(':') + 1) + "/extraction_result.txt', " +
                            "finish_time='" + MainController.getTime() + "' where task_name='" + task_name.substring(task_name.indexOf(':') + 1) + "' and domain='" + field + "'";
                    stmt.executeUpdate(update_statu);
                    SteveApplication.fields.remove(field);
                    it.remove();
                    if (list.isEmpty()) {
                        SteveApplication.taskPool.remove(user);
                        SteveApplication.threadPool.remove(user);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
