package cscholtz.android.nutrievaluator;

import android.net.Uri;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.health.SystemHealthManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class CacheUploadActivity extends AppCompatActivity {

    private String ExtDir = Environment.getExternalStorageDirectory().toString();
    private Button startButton;
    private TextView reports, timeUpload, timeTotal, timeCreation;
    private EditText NReports;
    //input parameters
    private String nombre,sexo,edad,peso,talla,cintura,cadera,braquial,carpo,tricipital,bicipital,suprailiaco,subescapular;
    //creates and access DB
    private SqliteOpenHelper helper = new SqliteOpenHelper(this,"BD1",null,1);
    //strgin for writing in pfd, values obtained by evaluator
    private String IMC,IPT,PESO_IDEAL,CMB,AMB,AGB,PT,CIN,RELCINCAD,CONTEXTURA;

    //class for creating pdf
    private TemplatePDF templatePDF;
    //saves name of current pdf file
    private String FileName;

    //reference to access a Cloud Firebase Storage
    private StorageReference storageReference;

    //JasonObjhect to read .json file with inputs
    private JSONObject jsonObject;
    private int len; // number of inputs

    //for measuring the time
    private long t0;
    private Vector<Long> timecreationstarts, timemergestarts, timeuploadstarts, timeuploadends;
    private int nloops, inloopn; //nloops = number of cycles of uploading ;; inloopn = determine the quantity of pdfs uploaded at that moment

    //for json data
    private String jsonString;
    private InputStream is;
    private int size;
    private byte[] buffer;
    private JSONArray jsonArray;
    private PDFMergerUtility ut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache_upload);
        startButton = (Button) findViewById(R.id.startButtonCache);
        NReports = (EditText) findViewById(R.id.NReports);
        reports = (TextView) findViewById(R.id.NReportsText);
        timeUpload = (TextView) findViewById(R.id.timeUpload);
        timeTotal = (TextView) findViewById(R.id.timeTotal);
        timeCreation = (TextView) findViewById(R.id.timeCreation);


        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nloops = 50;
                inloopn = 0;
                timecreationstarts = new Vector<Long>(nloops);
                timemergestarts = new Vector<Long>(nloops);
                timeuploadstarts = new Vector<Long>(nloops);
                timeuploadends = new Vector<Long>(nloops);
                storageReference = FirebaseStorage.getInstance().getReference();
                try {
                    runTasks();
                } catch (Exception e) {
                    Log.e("TryrunTasks",e.toString());
                }
            }
        });
    }

    public void runTasks() throws Exception {
        String jsonString;
        InputStream is = null;
        len = Integer.parseInt(NReports.getText().toString());
        t0 = System.nanoTime();
        try {
            is = getAssets().open("inputs_example.json");
            size = is.available();
            buffer = new byte[size];
            if(is.read(buffer)>0) {
                jsonString = new String(buffer, "UTF-8");
                jsonArray = new JSONArray(jsonString);
                ut = new PDFMergerUtility();
                doPDFs();
            }
        }catch (Exception e){
            Log.e("TryInrunTasks",e.toString());
        }
        finally {
            if(is!=null) {
                is.close();
            }
        }
    }

    private void doPDFs() throws Exception {
        ut = new PDFMergerUtility();
        timecreationstarts.add(inloopn, System.nanoTime());
        for (int i = 0; i < len; i++) {
            jsonObject = jsonArray.getJSONObject(inloopn*len+i);
            inputReceiver();
            evaluateData();
            createPDF();
            ut.addSource(ExtDir + "/PDF/" + FileName + ".pdf");
        }
        timemergestarts.add(inloopn, System.nanoTime());
        ut.setDestinationFileName(ExtDir + "/PDF/MergedPDF" + String.valueOf(inloopn) + ".pdf");
        ut.mergeDocuments();
        timeuploadstarts.add(inloopn, System.nanoTime());
        uploadFile();
    }

    public void uploadFile(){
        File f1 = new File(ExtDir + "/PDF/MergedPDF" + String.valueOf(inloopn) + ".pdf");
        Uri uri_file = Uri.fromFile(f1);

        StorageReference stg = storageReference.child("Cache").child(f1.getName());
        stg.putFile(uri_file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        timeuploadends.add(inloopn, System.nanoTime());
                        inloopn++;
                        if(inloopn<nloops) {
                            try {
                                doPDFs();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else{
                            uploadTimes();
                        }


                    }
                });

    }

    public void createPDF(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String currentDate = sdf.format(new Date());
        templatePDF = new TemplatePDF(getApplicationContext());
        FileName = nombre+"_"+currentDate;
        templatePDF.openDocument(FileName);
        templatePDF.addMetaData("Evaluacion Nutricional"+nombre,"evaluacion","cs");
        templatePDF.addTitles("Evaluacion Nutricional","Paciente: "+nombre,currentDate);
        templatePDF.addParagraph(IMC);
        templatePDF.addParagraph(IPT);
        templatePDF.addParagraph(PESO_IDEAL);
        templatePDF.addParagraph(CMB);
        templatePDF.addParagraph(AMB);
        templatePDF.addParagraph(AGB);
        templatePDF.addParagraph(PT);
        templatePDF.addParagraph(CIN);
        templatePDF.addParagraph(RELCINCAD);
        templatePDF.addParagraph(CONTEXTURA);
        templatePDF.closeDocument();
    }

    public void evaluateData(){
        Evaluator E = new Evaluator(nombre, sexo, new Integer(edad),  new Integer(tricipital),  new Integer(bicipital),  new Integer(suprailiaco), new Integer(subescapular),  new Float(peso),  new Float(talla),  new Float(cintura),  new Float(cadera),  new Float(braquial),  new Float(carpo));
        helper.abrir();
        String e = helper.getIdEdad(new Integer(edad));
        IMC = "IMC: "+String.format("%.2f",E.getIMC()) + " kg/mt² "+E.evaluarIMC();
        IPT = "%IPT: "+String.format("%.2f",E.getIPT())+"% "+E.evaluarIPT();
        PESO_IDEAL = "PESO IDEAL: "+String.format("%.2f",E.getPesoIdeal())+" kg";
        Integer[] rCMB = E.rangoPercentiles(E.getCMB(),E.Percentiles(helper.percentiles(e,sexo,"CMB")));
        CMB = "CMB: "+String.format("%.0f",E.getCMB())+ " mm (P"+rCMB[0]+"- P"+rCMB[1]+") "+E.evaluarPercentilesCMB(rCMB[0],rCMB[1]);
        Integer[] rAMB = E.rangoPercentiles(E.getAMB(),E.Percentiles(helper.percentiles(e,sexo,"AMB")));
        AMB = "AMB: "+String.format("%.0f",E.getAMB())+ " mm (P"+rAMB[0]+"- P"+rAMB[1]+") "+E.evaluarPercentiles(rAMB[0],rAMB[1]);
        Integer[] rAGB = E.rangoPercentiles(E.getAGB(),E.Percentiles(helper.percentiles(e,sexo,"AGB")));
        AGB = "AGB: "+String.format("%.0f",E.getAGB())+ " mm (P"+rAGB[0]+"- P"+rAGB[1]+") "+E.evaluarPercentiles(rAGB[0],rAGB[1]);
        Integer[] rPT = E.rangoPercentiles(E.getPT(),E.Percentiles(helper.percentiles(e,sexo,"PT")));
        PT = "PT: "+String.format("%.0f",E.getPT())+" mm (P"+rPT[0]+"- P"+rPT[1]+") "+E.evaluarPercentiles(rPT[0],rPT[1]);
        CIN = "CINTURA: "+String.format("%.2f",E.getCin())+" cm "+E.evaluarCintura();
        RELCINCAD = "REL CINT/CAD: "+String.format("%.2f",E.getRelCinCad())+" "+E.evaluarRelCinCad();
        CONTEXTURA = "CONTEXTURA: "+String.format("%.2f",E.getContextura())+" "+E.evaluarContextura();
        helper.cerrar();
    }

    public void inputReceiver()throws Exception{
        nombre = jsonObject.getString("nombre");
        sexo = jsonObject.getString("sexo");
        edad = jsonObject.getString("edad");
        peso = jsonObject.getString("peso");
        talla = jsonObject.getString("talla");
        cintura = jsonObject.getString("cintura");
        cadera = jsonObject.getString("cadera");
        braquial = jsonObject.getString("braquial");
        carpo = jsonObject.getString("carpo");
        tricipital = jsonObject.getString("tricipital");
        bicipital = jsonObject.getString("bicipital");
        suprailiaco = jsonObject.getString("suprailiaco");
        subescapular = jsonObject.getString("subescapular");
    }

    private void uploadTimes(){
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        String currentDate = sdf.format(new Date());
        templatePDF = new TemplatePDF(getApplicationContext());
        String sFileName ="[" + len + " Cache] " + currentDate +".txt";
        try {
            File root = new File(ExtDir, "TimeStamps");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(String.valueOf(t0));
            writer.append("\n\n");
            for(int i=0; i<nloops; i++){
                writer.append(String.valueOf(timecreationstarts.elementAt(i)) + "\n");
            }
            writer.append("\n");
            for(int i=0; i<nloops; i++){
                writer.append(String.valueOf(timemergestarts.elementAt(i)) + "\n");
            }
            writer.append("\n");
            for(int i=0; i<nloops; i++){
                writer.append(String.valueOf(timeuploadstarts.elementAt(i)) + "\n");
            }
            writer.append("\n");
            for(int i=0; i<nloops; i++){
                writer.append(String.valueOf(timeuploadends.elementAt(i)) + "\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File f1 = new File(ExtDir + "/TimeStamps/" + sFileName);
        Uri uri_file = Uri.fromFile(f1);
        StorageReference stg = storageReference.child("TimeStamps").child(f1.getName());
        stg.putFile(uri_file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        System.out.println("Uploaded");
                        reports.setText(String.valueOf(len) + "x" + String.valueOf(nloops) + " PDFs files uploaded");
                        int timetotal = (int)((timeuploadends.elementAt(nloops-1) - t0)/1e6);
                        /*timeCreation.setText("Creation time: " + timecreation + "ms");
                        timeUpload.setText("Uploading time: " + timeupload + "ms ");*/
                        timeTotal.setText("Total time: " + timetotal + "ms");
                    }
                });
    }

}