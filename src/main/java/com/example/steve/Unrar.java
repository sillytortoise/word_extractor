package com.example.steve;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Unrar {

    /*sourceRar rar路径
     **destDir 目标路径
     **所有路径须是unix格式
     */
    private static List<String> unrarToDest(String sourceRar, String destDir) {
        String cmd = "unrar x -o+ " + sourceRar + " " + destDir;
        String result = null;
        String parent_path = sourceRar.substring(0, sourceRar.lastIndexOf('/'));
        List<String> list = new ArrayList();
        try {
            Process ps = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Extracting  ")) {
                    String file_name = line.substring(12, line.indexOf(" ", 12));
                    if (file_name.indexOf("/") >= 0)
                        file_name = file_name.substring(0, file_name.indexOf("/"));
                    file_name = parent_path + "/" + file_name;
                    file_name = file_name.trim();
                    list.add(file_name);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void generateTxt(String user_corpus_path, String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        ArrayList<File> list = new ArrayList<>();
        Collections.addAll(list, files);
        for (File file : list) {
            /*word*/
            if (file.getName().endsWith(".doc") || file.getName().endsWith(".docx")) {
                WordConvert.generateTxt(user_corpus_path, file.getAbsolutePath());
            }
            /*txt*/
            else if (file.getName().endsWith(".txt")) {
                try {
                    File cpy_file = new File(user_corpus_path + "/" + file.getName());       //建立文件副本
                    FileInputStream fin = new FileInputStream(file);
                    FileOutputStream fout = new FileOutputStream(cpy_file);
                    byte[] buf = new byte[2048];
                    int len;
                    while ((len = fin.read(buf)) != -1) {
                        fout.write(buf, 0, len);
                    }
                    fin.close();
                    fout.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            /*pdf*/
            else if (file.getName().endsWith(".pdf")) {
                PdfConvert.generateTxt(user_corpus_path, file.getAbsolutePath());
            }
            /*rar, zip*/
            else if (file.getName().endsWith(".rar") || file.getName().endsWith(".zip")) {
                String full_path = file.getAbsolutePath();
                int index = full_path.lastIndexOf("/");
                List<String> unrar_list = unrarToDest(full_path, full_path.substring(0, index));
                for (String s : unrar_list) {
                    list.add(new File(s));
                }
            }

            /*folder*/
            else if (file.isDirectory()) {
                generateTxt(user_corpus_path, file.getAbsolutePath());
            }
        }
    }

}
