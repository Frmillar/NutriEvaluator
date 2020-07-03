package cscholtz.android.nutrievaluator;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;

public class TemplatePDF {
    private Context context;
    private File pdfFile;
    private Document document;
    private PdfWriter pdfWriter;
    private Paragraph paragraph;
    private Font fTitle = new Font(Font.FontFamily.TIMES_ROMAN,20, Font.BOLD);
    private Font fSubTitle = new Font(Font.FontFamily.TIMES_ROMAN,18, Font.BOLD,BaseColor.GRAY);
    private Font fText = new Font(Font.FontFamily.TIMES_ROMAN,12, Font.BOLD);
    private Font fHighText = new Font(Font.FontFamily.TIMES_ROMAN,15, Font.BOLD, BaseColor.BLUE);
    private String ExtDir = Environment.getExternalStorageDirectory().toString();

    public TemplatePDF(Context context){
        this.context = context;
    }

    public void openDocument(String fileName){
        createFile(fileName);
        try {
            document = new Document(PageSize.A4);
            pdfWriter = PdfWriter.getInstance(document,new FileOutputStream(pdfFile));
            document.open();
        }catch (Exception e){
            Log.e("openDocument",e.toString());
        }
    }

    public void closeDocument(){
        document.close();
    }

    public void addMetaData(String title,String subject,String author){
        document.addTitle(title);
        document.addSubject(subject);
        document.addAuthor(author);
    }

    public  void addTitles(String title,String subtitle,String date){
        try {
            paragraph = new Paragraph();
            addChildP(new Paragraph(title, fTitle));
            addChildP(new Paragraph(subtitle, fSubTitle));
            addChildP(new Paragraph(date, fHighText));
            paragraph.setSpacingAfter(30);
            document.add(paragraph);
        }catch (Exception e){
            Log.e("addTitles",e.toString());
        }
    }

    public void addParagraph(String text){
        try {
            paragraph = new Paragraph(text,fText);
            paragraph.setSpacingAfter(5);
            paragraph.setSpacingBefore(5);
            document.add(paragraph);
        }catch (Exception e){
            Log.e("addTitles",e.toString());
        }
    }

    private void addChildP(Paragraph childParagraph){
        childParagraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.add(childParagraph);
    }

    private void createFile(String file_name){
        File folder = new File(ExtDir,"PDF");
        if(!folder.exists()){
            folder.mkdirs();
        }
        pdfFile = new File(folder,file_name+".pdf");
    }

}
