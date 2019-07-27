package com.example.steve;

import java.io.FileInputStream;
import java.io.FileWriter;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;


public class PdfConvert implements GenerateTxt {

    private String mpdf_path;

    public PdfConvert() {}

    public PdfConvert(String path) {
        mpdf_path = path;
    }

    @Override
    public void generateTxt() {
        String result = null;
        PDDocument document = null;

        try {
            FileInputStream fis = new FileInputStream(mpdf_path);
            PDFParser parser = new PDFParser(new RandomAccessBuffer(fis));
            parser.parse();
            document = parser.getPDDocument();
            System.out.print(document);
            PDFTextStripper stripper = new PDFTextStripper();
            result = stripper.getText(document);
            FileWriter fw = new FileWriter(result_folder +
                    mpdf_path.substring(mpdf_path.lastIndexOf("/") + 1, mpdf_path.lastIndexOf(".")) +
                    ".txt", false);
            fw.write(result);
            fw.flush();
            fw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        PdfConvert convert = new PdfConvert();
        convert.generateTxt();
    }
}
