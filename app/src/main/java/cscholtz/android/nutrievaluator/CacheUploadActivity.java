package cscholtz.android.nutrievaluator;

import android.net.Uri;
import android.os.Environment;
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
import java.io.File;
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
    private long t0,t1,t2,t3;
    private long timecreation, timemerge, timetotal, timeupload;


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
                timecreation = 0;
                timemerge = 0;
                timetotal = 0;
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
            int size = is.available();
            byte[] buffer = new byte[size];
            if(is.read(buffer)>0) {
                jsonString = new String(buffer, "UTF-8");
                JSONArray jsonArray = new JSONArray(jsonString);
                PDFMergerUtility ut = new PDFMergerUtility();
                for (int i = 0; i < len; i++) {
                    jsonObject = jsonArray.getJSONObject(i);
                    inputReceiver();
                    evaluateData();
                    createPDF();
                    ut.addSource(ExtDir + "/PDF/" + FileName + ".pdf");
                }
                t1 = System.nanoTime();
                ut.setDestinationFileName(ExtDir + "/PDF/MergedPDF.pdf");
                ut.mergeDocuments();
                t2 = System.nanoTime();
                uploadFile();
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
        File f1 = new File(ExtDir+"/PDF/MergedPDF.pdf");
        Uri uri_file = Uri.fromFile(f1);

        StorageReference stg = storageReference.child("Cache").child(f1.getName());
        stg.putFile(uri_file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        t3 = System.nanoTime();
                        timetotal = (int)((t3-t0)/1e6);
                        timemerge = (int)((t2-t1)/1e6);
                        timecreation = (int)((t1-t0)/1e6);
                        timeupload = (int)((t3-t2)/1e6);

                        reports.setText(String.valueOf(len) + " PDFs files uploaded");
                        timeCreation.setText("Creation: " + timecreation + "ms | Merging: " + timemerge+ "ms");
                        timeUpload.setText("Uploading time: " + (timeupload) + "ms");
                        timeTotal.setText("Total time: " + timetotal + "ms");

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

}