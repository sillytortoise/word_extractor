package com.example.steve;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class Unrar implements GenerateTxt {

    private String mrar_path;

    public Unrar() {
    }

    public Unrar(String s) {
        mrar_path = s;
    }


    /*sourceRar rar路径
     **destDir 目标路径
     **所有路径须是unix格式
     */
    private void unrarToDest(String sourceRar, String destDir) {
        String cmd = "rar x " + sourceRar + " " + destDir;
        try {
            Runtime.getRuntime().exec("mkdir"+destDir).waitFor();
            Runtime.getRuntime().exec("sh generator.sh").waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void generateTxt() {
    }

    public void generateTxt(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();
        ArrayList<File> list = new ArrayList<>();
        Collections.addAll(list, files);
        for (File file : list) {
            /*word*/
            if (file.getName().endsWith(".doc") || file.getName().endsWith(".docx")) {
                WordConvert wc = new WordConvert(file.getAbsolutePath().replaceAll("\\\\", "/"));
                wc.generateTxt();
            }
            /*txt*/
            else if (file.getName().endsWith(".txt")) {
                try {
                    File cpy_file = new File(result_folder + file.getName());       //建立文件
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
                PdfConvert pc = new PdfConvert(file.getAbsolutePath().replaceAll("\\\\", "/"));
                pc.generateTxt();
            }
            /*rar*/
            else if (file.getName().endsWith(".rar")) {
                String full_path = file.getAbsolutePath();
                int index = full_path.lastIndexOf(".");
                unrarToDest(full_path, full_path.substring(0, index));
                list.add(new File(full_path.substring(0, index)));       //新解压出的文件夹添加进文件列表中
            }
            /*zip*/
            else if (file.getName().endsWith(".zip")) {
                String full_path = file.getAbsolutePath().replaceAll("\\\\", "/");
                int index = full_path.lastIndexOf(".");
                try {
                    Unzip myUnzip = new Unzip(full_path);
                    myUnzip.unzipToDest(full_path, full_path.substring(0, index));
                    list.add(new File(full_path.substring(0,index)));    //新解压出的文件夹添加进文件列表中
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            /*folder*/
            else if (file.isDirectory()) {
                generateTxt(file.getAbsolutePath().replaceAll("\\*","/"));
            }
        }
    }

//    public static void main(String[] args) {
//        Unrar myrar = new Unrar("D:\\result\\result.rar");
//        myrar.unrarToDest("D:\\result\\result.rar", "D:\\resultrar");
//        myrar.generateTxt("D:\\resultrar");
//    }
}
