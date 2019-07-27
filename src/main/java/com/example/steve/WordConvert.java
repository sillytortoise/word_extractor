package com.example.steve;

import java.io.*;

import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.extractor.POIXMLTextExtractor;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

public class WordConvert implements GenerateTxt {
    /**
     * 读取word文件内容
     *
     * @param path
     * @return buffer
     */
    private String mword_path;

    public WordConvert() {
    }

    public WordConvert(String path) {
        mword_path = path;
    }

    public String readWord(String path) {
        String buffer = "";
        try {
            if (path.endsWith(".doc")) {
                InputStream is = new FileInputStream(new File(path));
                WordExtractor ex = new WordExtractor(is);
                buffer = ex.getText();
                ex.close();
            } else if (path.endsWith("docx")) {
                OPCPackage opcPackage = POIXMLDocument.openPackage(path);
                POIXMLTextExtractor extractor = new XWPFWordExtractor(opcPackage);
                buffer = extractor.getText();
                extractor.close();
            } else {
                System.out.println("此文件不是word文件！");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    @Override
    public void generateTxt() {
        String content = readWord(mword_path);
        String name = mword_path.substring(mword_path.lastIndexOf("/") + 1, mword_path.lastIndexOf("."));
        try {
            FileWriter fw = new FileWriter(result_folder + name + ".txt", false);
            fw.write(content);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        WordConvert wc = new WordConvert();
        String content = wc.readWord("");
        try {
            FileWriter fw = new FileWriter("result.txt", false);
            fw.write(content);
            fw.flush();
            fw.close();
        } catch (IOException e) {
        }
    }
}
