package com.example.steve;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.charset.Charset;

public class Unzip implements GenerateTxt {
    //zip的路径
    private String mzip_path;

    public static final int BUFFER_SIZE = 2048;

    public Unzip() {}

    public Unzip(String s) {
        mzip_path = s;
    }


    /*将给定路径的zip文件解压到指定目标路径下
     ** zip_path 压缩包绝对路径
     * *dest 解压目标目录的绝对路径
     */
    void unzipToDest(String zip_path, String dest) throws IOException, InterruptedException {
        ZipFile zip = new ZipFile(zip_path, Charset.forName("GBK"));    //解决中文文件夹乱码

        /*遍历压缩包中的每一个文件*/
        for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = entries.nextElement();
            String zipEntryName = entry.getName();
            InputStream in = zip.getInputStream(entry);
            String outPath = (dest + "/" + zipEntryName).replaceAll("\\\\", "/");    //条目的解压绝对路径
            FileOutputStream out;
            byte[] buf1 = new byte[Unzip.BUFFER_SIZE];
            int len;

            //条目的文件对象
            File file = new File(outPath);

            // 判断路径文件夹是否存在,不存在则创建文件夹
            File parent_path = new File(outPath.substring(0, outPath.lastIndexOf('/')));

            if (!parent_path.exists()) {
                boolean isSuccessful = parent_path.mkdirs();       //级联创建目录
                if (!isSuccessful) {
                    System.out.println("Make dir failed!");
                    return;
                }
            }

            // 判断文件全路径是否为文件夹
            File subfolder = new File(outPath);
            if (subfolder.isDirectory()) {
                scanFolder(subfolder.getAbsolutePath().replaceAll("\\\\", "/"));
            } else {

                out = new FileOutputStream(file);

                while ((len = in.read(buf1)) > 0) {
                    out.write(buf1, 0, len);
                }
                out.flush();
                in.close();
                out.close();

                if (outPath.substring(outPath.lastIndexOf(".") + 1).equals("zip")) {   //如果该条目是zip 递归解压
                    unzipToDest(outPath, outPath.substring(0, outPath.lastIndexOf(".")));   //解压到与zip文件名相同的目录
                }
            }

        }
    }

    /*解压时处理文件夹中的内容
     * path 文件夹路径
     * */
    public void scanFolder(String path) {
        File folder = new File(path);
        File[] files = folder.listFiles();            //列出文件夹下所有文件
        for (File file : files) {
            try {
                FileInputStream fin = new FileInputStream(file);
                FileOutputStream fout = new FileOutputStream(path + "/" + file.getName());
                int len;
                /*如果此文件是目录*/
                if (file.isDirectory()) {
                    scanFolder(path + file.getName());
                }
                /*是zip压缩包*/
                else if (file.getName().endsWith(".zip")) {
                    String zip_path = file.getAbsolutePath().replaceAll("\\*", "/");
                    unzipToDest(zip_path, zip_path.substring(0, zip_path.lastIndexOf(".")));
                }
                /*如果是其他类型文件*/
                else {
                    byte[] buffer = new byte[Unzip.BUFFER_SIZE];
                    while ((len = fin.read(buffer)) > 0) {
                        fout.write(buffer, 0, len);
                    }
                    fout.flush();
                }
                fin.close();
                fout.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /*s为要遍历的文件夹路径*/
    public void generateTxt(String s) {
        File folder = new File(s);
        File[] files = folder.listFiles();
        for (File file : files) {
            /*word*/
            if (file.getName().endsWith(".doc") || file.getName().endsWith(".docx")) {
                WordConvert wc = new WordConvert(file.getAbsolutePath().replaceAll("\\\\", "/"));
                wc.generateTxt();
            }
            /*pdf*/
            else if (file.getName().endsWith(".pdf")) {
                PdfConvert pc = new PdfConvert(file.getAbsolutePath().replaceAll("\\\\", "/"));
                pc.generateTxt();
            }
            /*txt*/
            else if (file.getName().endsWith(".txt")) {
                try {
                    File cpy_file = new File(result_folder + file.getName());       //建立文件
                    FileInputStream fin=new FileInputStream(file);
                    FileOutputStream fout=new FileOutputStream(cpy_file);
                    byte[] buf=new byte[2048];
                    int len;
                    while ((len=fin.read(buf))!=-1) {
                        fout.write(buf,0,len);
                    }
                    fin.close();
                    fout.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            /*folder*/
            else if(file.isDirectory()){
                generateTxt(file.getAbsolutePath().replaceAll("\\*","/"));
            }
        }
    }

    @Override
    public void generateTxt() {}

//    /*主函数 测试解压zip*/
//    public static void main(String[] args) {
//        Unzip myzip = new Unzip();
//        try {
//            myzip.unzipToDest("D:/123/myzip.zip","D:/result");
//            myzip.generateTxt("D:/result");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}

