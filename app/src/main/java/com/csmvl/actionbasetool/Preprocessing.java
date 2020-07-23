package com.csmvl.actionbasetool;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import joinery.DataFrame;


public class Preprocessing {


    //parameters
    //FileOperation FD = new FileOperation();
    //FileDescriptor fd;
    private File Filename = null;
    private File[] Filesname = null;
    private int next_index = 0;
    private DataFrame<Object> df = new DataFrame<>();
    private Properties props;
    // private List<Double> InputList = new ArrayList<>();
    private int timestamps = 0;

    public DataFrame<Object> getData(){
        return this.df;
    }

    public Preprocessing(Properties props, int timestamps) throws IOException {
        /*
        26 欄位 (MoTi)
        this.df = new DataFrame<>("Timestamp", "LTime", "LSeq", "LAccelerometer1", "LAccelerometer2","LAccelerometer3", "LGyroscope1",
                "LGyroscope2", "LGyroscope3","LQuaternion1","LQuaternion2", "LQuaternion3", "LQuaternion4", "RTime", "RSeq", "RAccelerometer1", "RAccelerometer2",
                "RAccelerometer3", "RGyroscope1", "RGyroscope2", "RGyroscope3","RQuaternion1","RQuaternion2", "RQuaternion3", "RQuaternion4", "Label");
        32 欄位 (Cavy Sensor)
        (Quaternions, Euler Andle, Accelerometer, Linear Accelerometer, Velocity)(4, 3, 3, 3)
        */
        this.next_index = 0;
        this.props = props;
        this.timestamps = timestamps;
    }


    void readFile(File name){
        this.Filename = name;
    }

    void readFiles(File[] name){
        this.Filesname = name;
    }

    void initialize(){
        this.df = new DataFrame<Object>();
    }


    private Double[] GetDoublePropArray(String prop){
        String[] integerStrings = prop.split(",");
        Double[] dst = new Double[integerStrings.length];
        for(int i = 0; i < integerStrings.length; i++){
            dst[i] = Double.parseDouble(integerStrings[i]);
        }
        return dst;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void getInputs(String[] data) throws IOException {
        this.df = new DataFrame<Object>();
        // Get input data
        List<Double> DoubleList = new ArrayList<>();
        for(int i = 0;i < data.length;i++){
            Double[] tmpArray = GetDoublePropArray(data[i]);
            DoubleList.addAll(Arrays.asList(tmpArray));
            this.df.append(DoubleList);
            DoubleList.clear();
        }
        System.out.println(this.df.length());

    }

    //輸入想要的timestamp長度，即從原始資料取出該timestamp行數的資料
    //如要取得下一段長度的資料，再一次呼叫便可
    @RequiresApi(api = Build.VERSION_CODES.N)
    void GetOriginalList() throws IOException {
        InputStream inputStream = new FileInputStream(this.Filename);
        CSVFile csvFile = new CSVFile(inputStream);
        List AllList = csvFile.read();
        this.df = new DataFrame<Object>();
        int end_index = Math.min(this.next_index+this.timestamps, AllList.size());
        for(int i = this.next_index;i < end_index;i++){
            double[] doubleArray = (double[]) AllList.get(i);
            List<Double> DoubleList = new ArrayList<>();
            for (double no : doubleArray) {
                DoubleList.add(no);
            }
            this.df.append(DoubleList);
            DoubleList.clear();
        }
        this.next_index = end_index;
        System.out.println("next index point: "+this.next_index);
        System.out.println(this.df.length());

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void GetFilesList() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(this.Filesname[this.next_index]));
        String thisLine = null;
        while ((thisLine = reader.readLine()) != null){
            String[] strArray = thisLine.split("\t");
            List<Double> DoubleList = new ArrayList<>();
            for (String value : strArray) {
                DoubleList.add(Double.parseDouble(value));
            }
            this.df.append(DoubleList);
            DoubleList.clear();
        }
        this.next_index++;
        if(this.df.length() < this.timestamps){
            initialize();
            this.next_index++;
        }
        reader.close();
    }

    private Integer[] GetPropArray(String prop){
        String[] integerStrings = prop.split(",");
        Integer[] dst = new Integer[integerStrings.length];
        for(int i = 0; i < integerStrings.length; i++){
            dst[i] = Integer.parseInt(integerStrings[i]);
        }
        return dst;
    }


    DataFrame<Object> AlignData(){

        // System.out.println("-------------------------------------------------------------");
        // System.out.println("Original df: "+this.df.head());
        // System.out.println("-------------------------------------------------------------");
        Integer[] unused_columns = GetPropArray(this.props.getProperty("unused_column"));
        this.df = this.df.drop(unused_columns);
        // System.out.println("-------------------------------------------------------------");
        // System.out.println("Before Align: "+this.df.head());
        // System.out.println("-------------------------------------------------------------");

        Integer[] accerlation_column1 = GetPropArray(this.props.getProperty("accerlation_column1"));
        Integer[] accerlation_column2 = GetPropArray(this.props.getProperty("accerlation_column2"));

        DataFrame<Object> currentAccData1 = this.df.retain(accerlation_column1);
        DataFrame<Object> currentAccData2 = this.df.retain(accerlation_column2);
        // System.out.println("-------------------------------------------------------------");
        // System.out.println("currentAccData1: "+ currentAccData1.head());
        // System.out.println("currentAccData2: "+ currentAccData2.head());
        // System.out.println("-------------------------------------------------------------");

        DataFrame<Object> GB1 = GbFilter(currentAccData1);
        DataFrame<Object> GB2 = GbFilter(currentAccData2);



        //this.df = this.df.resetIndex();

        DataFrame<Object> GB = GB1.join(GB2);
        DataFrame<Object> Dst = this.df.join(GB);

        Dst = Dst.resetIndex();

        // System.out.println("-----------------------------------------------------------------------");
        // System.out.println("Dst: "+ Dst.head());
        // System.out.println("-----------------------------------------------------------------------");

        return Dst;

    }

    private DataFrame<Object> GbFilter(DataFrame<Object> df){

         DataFrame<Object> DstList = new DataFrame<>();

        //medium_filter_window_size = 3
        final int n = Integer.parseInt(this.props.getProperty("medium_filter_window_size"));
        //sampling frequency
        final int Fs = Integer.parseInt(this.props.getProperty("Fs"));
        //passband frequency
        final double Fpass = Double.parseDouble(this.props.getProperty("Fpass"));
        //stopband frequency
        final int Fstop = Integer.parseInt(this.props.getProperty("Fstop"));
        //passband ripple(dB)
        final double Apass = Double.parseDouble(this.props.getProperty("Apass"));
        //stopband attenuation(dB)
        final int Astop = Integer.parseInt(this.props.getProperty("Astop"));
        //filter order
        final int filter_order = Integer.parseInt(this.props.getProperty("filter_order"));
        // IIRFilter Prototype
        final String Prototype = this.props.getProperty("Prototype");
        // IIRFilter FilterType
        final String FilterType = this.props.getProperty("FilterType");

        Double[][] DataArray = new Double[df.length()][df.size()];
        DataArray = (Double[][]) df.toArray(DataArray);

        SignalProcessing S1 = new SignalProcessing();

        int[] size = {n, 1};
        DataArray = S1.medianfilter(DataArray, size);


        Double[][] GravityArray = new Double[DataArray.length][DataArray[0].length];
        Double[][] BodyArray = new Double[df.length()][df.size()];


         //error parameters
        IIRFilter F1 = new IIRFilter();
        F1.setPrototype(Prototype);
        F1.setFilterType(FilterType);
        // F1.setPrototype("Chebyshev");
        // F1.setFilterType("LP");
        F1.setOrder(filter_order);
        F1.setRate(Fs);
        F1.setFreq1(Fstop);
        // suggest Freq2 is Frequency pass
        F1.setFreq2(Fpass);
        F1.setRipple(Apass);
        F1.design();
        F1.filterGain();
        double[] aCoeff = new double[filter_order+1];
        double[] bCoeff = new double[filter_order+1];
        // B, A 與投影片顛倒
        for(int i = 0;i <= filter_order; i++){
            aCoeff[i] = F1.getACoeff(i);
            bCoeff[i] = F1.getBCoeff(i);
        }


        GravityArray =  S1.linear_filter(filter_order, bCoeff, aCoeff, DataArray);
        /*
        for(int i = 0; i< GravityArray.length;i++){
            System.out.println("GravityArray"+"\t"+GravityArray[i][0]+"\t"+GravityArray[i][1]+"\t"+GravityArray[i][2]);
        }
        */

       for(int i = 0;i < DataArray.length;i++){
           for(int j = 0; j < DataArray[0].length;j++){
               BodyArray[i][j] = DataArray[i][j] - GravityArray[i][j];
           }
       }

       Double[][] Dstlist = new Double[GravityArray.length][GravityArray[0].length+BodyArray[0].length];

       for(int i = 0;i < Dstlist.length;i++){
           for(int j = 0; j < GravityArray[0].length;j++){
               Dstlist[i][j] = GravityArray[i][j];
           }
           for(int k = 0; k < BodyArray[0].length;k++){
               Dstlist[i][k+GravityArray[0].length] = BodyArray[i][k];
           }
       }

       for(int i = 0; i < Dstlist.length;i++){
           List<Double> list = Arrays.asList(Dstlist[i]);
           DstList.append(list);
           //list.clear();
       }

        return DstList;
    }

}
