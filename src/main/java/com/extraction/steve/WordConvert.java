package com.extraction.steve;

import java.io.*;

import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.ooxml.extractor.POIXMLTextExtractor;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

public class WordConvert {

    public static StringBuffer readWord(String path) {
        StringBuffer buffer = new StringBuffer();
        try {
            if (path.endsWith(".doc")) {
                InputStream is = new FileInputStream(new File(path));
                WordExtractor ex = new WordExtractor(is);
                buffer.append(ex.getText());
                is.close();
                ex.close();
            } else if (path.endsWith("docx")) {
                OPCPackage opcPackage = POIXMLDocument.openPackage(path);
                POIXMLTextExtractor extractor = new XWPFWordExtractor(opcPackage);
                buffer.append(extractor.getText());
                extractor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static void generateTxt(String user_corpus_path, String mword_path) {
        StringBuffer content = readWord(mword_path);
        String name = mword_path.substring(mword_path.lastIndexOf("/") + 1, mword_path.lastIndexOf("."));
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(user_corpus_path + "/" + name + ".txt", false));
            bw.write(content.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
