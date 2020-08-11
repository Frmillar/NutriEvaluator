package cscholtz.android.nutrievaluator;

import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class CacheUploadActivity extends AppCompatActivity {

    private String ExtDir = Environment.getExternalStorageDirectory().toString();
    private Button startButton;
    private TextView reports, timeUpload, timeTotal, timeCreation;
    private EditText NReports, CacheSize;
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
    private int len, cache_size; // number of inputs and cache size

    //for measuring the time
    private long t0;
    private Vector<Long> timecreationstarts, timemergestarts, timeuploadstarts, timeuploadends;
    private int nloops, num, inloopn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache_upload);
        startButton = (Button) findViewById(R.id.startButtonCache);
        NReports = (EditText) findViewById(R.id.NReports);
        CacheSize = (EditText) findViewById(R.id.Cache_size);
        reports = (TextView) findViewById(R.id.NReportsText);
        timeUpload = (TextView) findViewById(R.id.timeUpload);
        timeTotal = (TextView) findViewById(R.id.timeTotal);
        timeCreation = (TextView) findViewById(R.id.timeCreation);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                num = 0;
                inloopn = 0;
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
        String jsonString = null;
        InputStream is = null;
        len = Integer.parseInt(NReports.getText().toString());
        cache_size = Integer.parseInt(CacheSize.getText().toString());
        nloops = len/cache_size;
        timecreationstarts = new Vector<Long>(nloops);
        timemergestarts = new Vector<Long>(nloops);
        timeuploadstarts = new Vector<Long>(nloops);
        timeuploadends = new Vector<Long>(nloops);
        nloops = len/cache_size;
        t0 = System.nanoTime();
        try {
            is = getAssets().open("inputs_example.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            if(is.read(buffer)>0) {
                jsonString = new String(buffer, "UTF-8");
                JSONArray jsonArray = new JSONArray(jsonString);
                PDFMergerUtility ut;
                for(int i = 0;i<len; i+=cache_size){
                    ut = new PDFMergerUtility();
                    long taux1=System.nanoTime();
                    timecreationstarts.add(inloopn, taux1);
                    for (int j = 0; j<cache_size; j++) {
                        jsonObject = jsonArray.getJSONObject((i+j));
                        inputReceiver();
                        evaluateData();
                        createPDF();
                        ut.addSource(ExtDir + "/PDF/" + FileName + ".pdf");
                    }
                    long taux2=System.nanoTime();
                    timemergestarts.add(inloopn, taux2);
                    ut.setDestinationFileName(ExtDir + "/PDF/MergedPDF" + String.valueOf(inloopn) + ".pdf");
                    ut.mergeDocuments();
                    long taux3=System.nanoTime();
                    timeuploadstarts.add(inloopn, taux3);
                    uploadFile();
                    inloopn++;
                }
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

    public void uploadFile(){
        File f1 = new File(ExtDir + "/PDF/MergedPDF" + String.valueOf(inloopn) + ".pdf");
        Uri uri_file = Uri.fromFile(f1);

        StorageReference stg = storageReference.child("Cache").child(f1.getName());
        stg.putFile(uri_file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        long taux=System.nanoTime();
                        timeuploadends.add(num, taux);
                        num++;
                        if(num==nloops)uploadTimes();
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void uploadTimes(){
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
        String currentDate = sdf.format(new Date());
        templatePDF = new TemplatePDF(getApplicationContext());
        String sFileName ="[" + len + " Cache size " + cache_size + "] " + currentDate +".txt";
        try {
            File root = new File(ExtDir, "TimeStamps");
            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile);
            writer.write(String.valueOf(t0));
            writer.write(System.lineSeparator());
            writer.write("\r\n");
            //writer.write(System.lineSeparator());
            for(int i=0; i<num; i++){
                writer.write(String.valueOf(timecreationstarts.elementAt(i)));
                writer.write(System.lineSeparator());
                //System.out.println("CRE: " + timecreationstarts.elementAt(i));
            }
            writer.append("\n");
            for(int i=0; i<num; i++){
                writer.write(String.valueOf(timemergestarts.elementAt(i)));
                writer.write(System.lineSeparator());
                //System.out.println("MER: " + timecreationstarts.elementAt(i));
            }
            writer.append("\n");
            for(int i=0; i<num; i++){
                writer.write(String.valueOf(timeuploadstarts.elementAt(i)));
                writer.write(System.lineSeparator());
            }
            writer.append("\n");
            for(int i=0; i<num; i++){
                writer.write(String.valueOf(timeuploadends.elementAt(i)));
                writer.write(System.lineSeparator());
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
                        reports.setText(String.valueOf(len) + " Reports in " +  String.valueOf((int)(len/cache_size)) + " PDFs files uploaded");
                        timeTotal.setText("Total time: " + (int)((timeuploadends.elementAt(num-1)-t0)/1e6) + "ms");
                    }
                });
    }

}