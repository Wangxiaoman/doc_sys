package com.qiwenshare.file.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import com.spire.doc.Document;
import com.spire.pdf.FileFormat;
import com.spire.pdf.PdfDocument;
import com.spire.pdf.widget.PdfPageCollection;

public class PdfConvertTextService {
    
    public static String pdfConvertWord(String pdfPath) {
        String descPath = spirePdfToWord(pdfPath);
        if(StringUtils.isBlank(descPath)) {
            descPath = pdfPath.replace("pdf", "docx");
            pdfboxConvertWord(pdfPath, descPath);
        }
        return descPath;
    }

    public static void pdfboxConvertWord(String pdfPath, String wordPath) {
        // 创建Word文档
        XWPFDocument doc = new XWPFDocument();
        XWPFParagraph p = doc.createParagraph();
        FileOutputStream out = null;
        try {
            File pdf = new File(pdfPath);
            // 读取PDF文件
            PDDocument pdfDoc = Loader.loadPDF(pdf);
            PDFTextStripper stripper = new PDFTextStripper();
            String pdfText = stripper.getText(pdfDoc);

            p.createRun().setText(pdfText);
            // 写入Word文件
            out = new FileOutputStream(new File(wordPath));
            doc.write(out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                doc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String spirePdfToWord(String srcPath) {
        boolean result = false;
        String baseDir = srcPath.substring(0, srcPath.length() - 4);
        String splitPath = baseDir + "_temp_split" + File.separator;
        String docPath = baseDir + "_temp_doc" + File.separator;

        String desPath = baseDir + ".docx";
        try {
            // 0、判断输入的是否是pdf文件
            // 第一步：判断输入的是否合法
            boolean flag = isPDFFile(srcPath);
            if (flag) {
                // 第二步：在输入的路径下新建文件夹
                boolean flag1 = create(splitPath, docPath);
                if (flag1) {
                    // 1、加载pdf
                    PdfDocument pdf = new PdfDocument();
                    pdf.loadFromFile(srcPath);
                    PdfPageCollection num = pdf.getPages();

                    // 2、如果pdf的页数小于11，那么直接进行转化
                    if (num.getCount() <= 10) {
                        pdf.saveToFile(desPath, com.spire.pdf.FileFormat.DOCX);
                    }
                    // 3、否则输入的页数比较多，就开始进行切分再转化
                    else {
                        // 第一步：将其进行切分,每页一张pdf
                        pdf.split(splitPath + "test{0}.pdf", 0);

                        // 第二步：将切分的pdf，一个一个进行转换
                        File[] fs = getSplitFiles(splitPath);
                        for (int i = 0; i < fs.length; i++) {
                            PdfDocument sonpdf = new PdfDocument();
                            sonpdf.loadFromFile(fs[i].getAbsolutePath());
                            sonpdf.saveToFile(
                                    docPath + fs[i].getName().substring(0,
                                            fs[i].getName().length() - 4) + ".docx",
                                    FileFormat.DOCX);
                        }
                        // 第三步：对转化的doc文档进行合并，合并成一个大的word
                        try {
                            result = merge(docPath, desPath);
                            // 移除水印
                            removeWatermark(new File(desPath));
                            return desPath;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 4、把刚刚缓存的split和doc删除
            if (result == true) {
                clearFiles(splitPath);
                clearFiles(docPath);
            }
        }
        return null;
    }


    public static boolean removeWatermark(File file) {
        try {
            XWPFDocument doc = new XWPFDocument(new FileInputStream(file));
            // 段落
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if ("Evaluation Only. Created with Aspose.PDF. Copyright 2002-2021 Aspose Pty Ltd."
                        .equals(text)) {
                    List<XWPFRun> runs = paragraph.getRuns();
                    runs.forEach(e -> e.setText("", 0));
                }
            }
            FileOutputStream outStream = new FileOutputStream(file);
            doc.write(outStream);
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static boolean create(String splitPath, String docPath) {
        File f = new File(splitPath);
        File f1 = new File(docPath);
        if (!f.exists())
            f.mkdirs();
        if (!f.exists())
            f1.mkdirs();
        return true;
    }

    // 判断是否是pdf文件
    private static boolean isPDFFile(String srcPath2) {
        File file = new File(srcPath2);
        String filename = file.getName();
        if (filename.endsWith(".pdf")) {
            return true;
        }
        return false;
    }

    // 取得某一路径下所有的pdf
    private static File[] getSplitFiles(String path) {
        File f = new File(path);
        File[] fs = f.listFiles();
        if (fs == null) {
            return null;
        }
        return fs;
    }

    // 删除文件和目录
    private static void clearFiles(String workspaceRootPath) {
        File file = new File(workspaceRootPath);
        if (file.exists()) {
            deleteFile(file);
        }
    }

    private static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i]);
            }
        }
        file.delete();
    }

    private static boolean merge(String docPath, String desPath) {
        File[] fs = getSplitFiles(docPath);
        Document document = new Document(docPath + "test0.docx");
        for (int i = 1; i < fs.length; i++) {
            document.insertTextFromFile(docPath + "test" + i + ".docx",
                    com.spire.doc.FileFormat.Docx_2013);
        }
        // 第四步：对合并的doc进行保存2
        document.saveToFile(desPath);
        return true;
    }

    public static void main(String[] args) {
        String pdfPath = "/Users/xiaomanwang/doc_sys/file/week.pdf";
        String wordPath = "/Users/xiaomanwang/doc_sys/file/abc_doc.docx";
        spirePdfToWord(pdfPath);
        // spirePdfConvertWord(pdfPath, wordPath);
        // asposPdfConvertWord(pdfPath,wordPath);
        // File pdf = new File(padfPath);
        // pdfboxConvertWord(pdf,wordPath);
    }
}
