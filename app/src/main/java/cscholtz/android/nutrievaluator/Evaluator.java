package cscholtz.android.nutrievaluator;
import android.database.Cursor;

public class Evaluator {
    private String nombre,sexo;
    private int edad,tricipital,bicipital,suprailiaco,subescapular;
    private float peso,talla,cintura,cadera,braquial,carpo;
    private double IMC,PesoIdeal,IPT,CMB,AMB,AGB,RelCinCad,contextura;
    private int[] percentiles = {5,10,25,50,75,90,96};

    public Evaluator( String nom, String sex, int ed, int trici, int bici, int supra, int subes, float pe, float tal, float cint, float cad, float braq, float carp){
        this.nombre = nom;
        this.edad = ed;
        this.sexo = sex;
        this.tricipital = trici;
        this.bicipital = bici;
        this.suprailiaco = supra;
        this.subescapular = subes;
        this.peso = pe;
        this.talla = tal;
        this.cintura = cint;
        this.braquial = braq;
        this.cadera = cad;
        this.carpo = carp;

        this.IMC = peso/Math.pow(talla,2);
        if(sexo.equals("Masculino")){
            this.PesoIdeal = Math.pow(talla,2) * 22;
        }else{
            this.PesoIdeal = Math.pow(talla,2) * 21.5;
        }
        this.IPT = peso*100/PesoIdeal;
        this.CMB = 10*braquial - tricipital * Math.PI;
        this.AMB = Math.pow(CMB,2)/(4*Math.PI);
        this.AGB = (Math.pow(braquial*10,2)/(4*Math.PI)) - AMB;
        this.RelCinCad = cintura/cadera;
        this.contextura = 100*talla/carpo;
    }

    public double getIMC(){
        return IMC;
    }
    public double getIPT(){
        return IPT;
    }
    public double getPesoIdeal(){
        return PesoIdeal;
    }
    public double getCMB(){
        return CMB;
    }
    public double getAMB(){
        return AMB;
    }
    public double getAGB(){
        return AGB;
    }
    public double getPT(){
        return tricipital;
    }
    public double getCin(){
        return cintura;
    }
    public double getRelCinCad(){
        return RelCinCad;
    }
    public double getContextura(){
        return contextura;
    }

    public int percentilMin(double value,Integer[] p){
        double min;
        min = 100000;
        int aux=0;
        for(int i=0;i<7;i++){
            if(value-p[i]<0){
                break;
            }else{
                if(value - p[i] < min){
                    min = value-p[i];
                    aux = i;
                }
            }
        }
        return percentiles[aux];
    }

    public int percentilMax(double value,Integer[] p){
        double max;
        max = 100000;
        int aux = 6;
        for(int i=6;i>=0;i--){
            if(p[i]-value<0){
                break;
            }else{
                if(p[i]- value < max){
                    max = p[i] - value;
                    aux = i;
                }
            }
        }
        return percentiles[aux];
    }

    public String evaluarIMC(){
        if(IMC<18.5){
            return "Enflaquecido";
        }else if(IMC>= 18.5 && IMC <25){
            return "Normal";
        }else if(IMC>=25 && IMC<30){
            return "Sobrepeso";
        }else{
            return "Obesidad";
        }
    }

    public String evaluarIPT(){
        if(IPT<70){
            return "Desnutricion Severa";
        }else if(IPT>= 70 && IPT <80){
            return "Desnutricion Moderada";
        }else if(IPT>=80 && IPT<90){
            return "Desnutricion Leve";
        }else if(IPT>=90 && IPT<110){
            return "Normal";
        }else if(IPT>=110 && IPT<120){
            return "Sobrepeso";
        }else{
            return "Obesidad";
        }
    }

    public String evaluarPercentilesCMB(int pmin, int pmax){
        float prom = (pmin+pmax)/2f;
        if(prom<=5){
            return "Déficit moderado a severo";
        }else if(prom>5 && prom <=25){
            return "Déficit leve";
        }else if(prom>25 && prom<=75){
            return "Reserva normal";
        }else{
            return "Reserva normal alta";
        }
    }

    public String evaluarPercentiles(int pmin, int pmax){
        float prom = (pmin+pmax)/2f
                ;
        if(prom<=5){
            return "Déficit moderado a severo";
        }else if(prom>5 && prom <=10){
            return "Déficit leve";
        }else if(prom>10 && prom<=90){
            return "Reserva normal";
        }else if(prom>90 && prom<=95){
            return "Reserva alta";
        }else{
            return "Reserva muy alta";
        }
    }

    public String evaluarCintura(){
        if(sexo.equals("Masculino")){
            if(cintura<94){
                return "Normal";
            }else if(cintura>=94 && cintura<102){
                return "Riesgo moderado";
            }else{
                return "Riesgo Alto";
            }
        }else{
            if(cintura<80){
                return "Normal";
            }else if(cintura>=80 && cintura<88){
                return "Riesgo moderado";
            }else{
                return "Riesgo Alto";
            }
        }
    }

    public String evaluarRelCinCad(){
        if(sexo.equals("Masculino")){
            if(RelCinCad<0.9){
                return "Normal";
            }else if(RelCinCad>=0.9 && RelCinCad<=1){
                return "Riesgo moderado";
            }else{
                return "Riesgo Alto";
            }
        }else{
            if(RelCinCad<0.75){
                return "Normal";
            }else if(RelCinCad>=0.75 && RelCinCad<=0.85){
                return "Riesgo moderado";
            }else{
                return "Riesgo Alto";
            }
        }
    }

    public String evaluarContextura(){
        if(sexo.equals("Masculino")){
            if(contextura<9.6){
                return "grande";
            }else if(contextura>=9.6 && contextura<=10.4){
                return "mediana";
            }else{
                return "pequeña";
            }
        }else{
            if(contextura<10.1){
                return "grande";
            }else if(contextura>=10.1 && contextura<=11){
                return "mediana";
            }else{
                return "pequeña";
            }
        }
    }

    public Integer[] Percentiles(Cursor c){
        c.moveToFirst();
        Integer[] pe = new Integer[7];
        for(int i=0;i<7;i++){
            pe[i]= Integer.valueOf(c.getString(i+1));
        }
        return pe;
    }

    public Integer[] rangoPercentiles(double a, Integer[] p){
        Integer[] rango = new Integer[2];
        rango[0] = percentilMin(a,p);
        rango[1] = percentilMax(a,p);
        return rango;
    }
}