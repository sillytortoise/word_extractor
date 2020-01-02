package com.example.steve;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.charset.Charset;

public class Unzip {
    /*sourceZip zip路径
     **destDir 目标路径
     **所有路径须是unix格式
     */
    public static List<String> unzipToDest(String sourceZip, String destDir) throws Exception{
        String cmd = "unzip -n -d " + destDir + " " + sourceZip;
        String result = null;
        List<String> list = new ArrayList<>();
        Process ps = Runtime.getRuntime().exec(cmd);
        ps.waitFor();
        BufferedReader br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("   creating: ") || line.startsWith("  inflating: ")) {
                String file_name = line.substring(13);
                if (file_name.indexOf("/") >= 0)
                    file_name = file_name.substring(0, file_name.indexOf("/"));
                file_name = file_name.trim();
                list.add(file_name);
            }
        }
        return list;
    }
}

