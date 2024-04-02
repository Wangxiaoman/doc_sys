package com.qiwenshare.file.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import com.qiwenshare.file.log.CommonLogger;
import com.spire.doc.Document;
import com.spire.pdf.FileFormat;
import com.spire.pdf.PdfDocument;

@Service
public class PdfConvertTextService {

    public File pdfConvertWord(String pdfPath, String wordPath) {
        File outFile = spirePdfToWord(pdfPath, wordPath);
        if (outFile == null) {
            return pdfboxConvertWord(pdfPath, wordPath);
        }
        return outFile;
    }

    public File pdfboxConvertWord(String pdfPath, String wordPath) {
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
            File outFile = new File(wordPath);
            out = new FileOutputStream(outFile);
            doc.write(out);
            return outFile;
        } catch (Exception e) {
            e.printStackTrace();
            CommonLogger.error("pdf to word error, ex:", e);
        } finally {
            try {
                out.close();
                doc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static File spirePdfToWord(String srcPath, String wordPath) {
        boolean result = false;
        String baseDir = srcPath.substring(0, srcPath.length() - 4);
        String splitPath = baseDir + "_temp_split" + File.separator;
        String docPath = baseDir + "_temp_doc" + File.separator;

        String desPath = wordPath;
        try {
            // 0、判断输入的是否是pdf文件
            // 第一步：判断输入的是否合法
            boolean flag = isPDFFile(srcPath);
            if (flag) {
                // 第二步：在输入的路径下新建文件夹
                boolean flag1 = create(splitPath, docPath);
                if (flag1) {
                    File pdf = new File(srcPath);
                    // 将pdf切分成单页
                    splitPdf(pdf, splitPath);
                    // 将切分的pdf，一个一个进行转换
                    File[] fs = getSplitFiles(splitPath);
                    for (int i = 0; i < fs.length; i++) {
                        PdfDocument sonpdf = new PdfDocument();
                        sonpdf.loadFromFile(fs[i].getAbsolutePath());
                        String fname =
                                docPath + fs[i].getName().substring(0, fs[i].getName().length() - 4)
                                        + ".docx";
                        try {
                            sonpdf.saveToFile(fname, FileFormat.DOCX);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            CommonLogger.error("convert page {} error,ex:{}", i, ex);
                        }
                    }
                    // 对转化的doc文档进行合并，合并成一个大的word
                    try {
                        result = merge(docPath, desPath);
                        // 移除水印
                        File outFile = new File(desPath);
                        removeWatermark(outFile);
                        return outFile;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            CommonLogger.error("pdf to word error, ex:", e);
        } finally {
            // 4、把刚刚缓存的split和doc删除
            if (result == true) {
                clearFiles(splitPath);
                clearFiles(docPath);
            }
        }
        return null;
    }

    private static final String TEMP_FILE_NAME = "temp";

    private static void splitPdf(File pdfFile, String descPath) throws Exception {
        // load pdf file
        PDDocument document = Loader.loadPDF(pdfFile);
        // instantiating Splitter
        Splitter splitter = new Splitter();
        // split the pages of a PDF document
        List<PDDocument> Pages = splitter.split(document);
        // Creating an iterator
        Iterator<PDDocument> iterator = Pages.listIterator();

        // saving splits as pdf
        int i = 0;
        while (iterator.hasNext()) {
            PDDocument pd = iterator.next();
            // provide destination path to the PDF split
            pd.save(descPath + "/" + TEMP_FILE_NAME + i++ + ".pdf");
        }
        document.close();
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
            CommonLogger.error("pdf to word error, ex:", e);
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
        Document document = new Document(docPath + TEMP_FILE_NAME + 0 + ".docx");
        for (int i = 1; i < fs.length; i++) {
            document.insertTextFromFile(docPath + TEMP_FILE_NAME + i + ".docx",
                    com.spire.doc.FileFormat.Docx_2013);
            System.out.println("file path:" + docPath + ",merge file index:" + i);
        }
        // 第四步：对合并的doc进行保存2
        document.saveToFile(desPath);
        System.out.println("file path:" + docPath + ",doc merge finish");
        return true;
    }

    public static void main(String[] args) {
        long begin = System.currentTimeMillis();
        spirePdfToWord("/Users/xiaomanwang/doc_sys/pdf_test/schreyer2012.pdf","/Users/xiaomanwang/doc_sys/pdf_test/schreyer2012.docx");
        System.out.println(System.currentTimeMillis()-begin);
        begin = System.currentTimeMillis();
        spirePdfToWord("/Users/xiaomanwang/doc_sys/pdf_test/031002000211_42562221.pdf","/Users/xiaomanwang/doc_sys/pdf_test/031002000211_42562221.docx");
        System.out.println(System.currentTimeMillis()-begin);
        begin = System.currentTimeMillis();
        spirePdfToWord("/Users/xiaomanwang/doc_sys/pdf_test/Invoice_1320530627.pdf","/Users/xiaomanwang/doc_sys/pdf_test/Invoice_1320530627.docx");
        System.out.println(System.currentTimeMillis()-begin);
        begin = System.currentTimeMillis();
        spirePdfToWord("/Users/xiaomanwang/doc_sys/pdf_test/Invoice_1323634139.pdf","/Users/xiaomanwang/doc_sys/pdf_test/Invoice_1323634139.docx");
        System.out.println(System.currentTimeMillis()-begin);
        begin = System.currentTimeMillis();
        spirePdfToWord("/Users/xiaomanwang/doc_sys/pdf_test/jm301323k.pdf","/Users/xiaomanwang/doc_sys/pdf_test/jm301323k.docx");
        System.out.println(System.currentTimeMillis()-begin);
        begin = System.currentTimeMillis();
        spirePdfToWord("/Users/xiaomanwang/doc_sys/pdf_test/molecules-25-01375.pdf","/Users/xiaomanwang/doc_sys/pdf_test/molecules-25-01375.docx");
        System.out.println(System.currentTimeMillis()-begin);
        begin = System.currentTimeMillis();
        spirePdfToWord("/Users/xiaomanwang/doc_sys/pdf_test/PCT320230224000003.pdf","/Users/xiaomanwang/doc_sys/pdf_test/PCT320230224000003.docx");
        System.out.println(System.currentTimeMillis()-begin);
        begin = System.currentTimeMillis();
        spirePdfToWord("/Users/xiaomanwang/doc_sys/pdf_test/RD-9404954-250619-1108-31.pdf","/Users/xiaomanwang/doc_sys/pdf_test/RD-9404954-250619-1108-31.docx");
        System.out.println(System.currentTimeMillis()-begin);
    }
}
