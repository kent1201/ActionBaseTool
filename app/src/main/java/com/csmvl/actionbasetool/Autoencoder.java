package com.csmvl.actionbasetool;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import joinery.DataFrame;

/*
*  class (Autoencoder):
*      use:
*          Load the pytorch model and get the
*  function:
*      public Autoencoder(String modelPath, DataFrame df, Properties props) throws IOException{}
*          use:
*              initialize parameters and load model.
*          input:
*              String modelPath: Your model path.
*              DataFrame df: The input data (DataFrame type).
*              Properties props: The parameters in the config file (properties).
*          output:
*              None.
*
*       private float[] transDim(Double[][] src){}
*           use:
*               Transform the data type (Double to Float) and dimension (2d to 1d).
*           input:
*               Double[][] src: A 2d and Double type data.
*           output:
*               float[] dst : A 1d and Float type array.
*
*       double GetLabels(){}
*           use:
*               Get the input data's label.
*           input:
*               None.
*           output:
*               return label (Double type).
*
*       private Integer[] GetPropArray(String prop)
*           use:
*               Transform String into Integer array.
*           input:
*               String input.
*           output:
*               Integer array.
*
*       private Double[][] preprocess(){}
*           use:
*               The data need to be preprocessed before input to the model.
*           input:
*               None.
*           output:
*               Double[][] data.
*
*       IValue[] forward(){}
*           use:
*               Put the data after preprocess and return the prediction.
*           input:
*               None.
*           output:
*               IValue[2](By pytorch) output.
*               => IValue[0]: output.
*               => IValue[1]: latents.
*
*       void Load_centroids(String Path) throws FileNotFoundException{}
*           use:
*               Load the best centroids from file('centroid.txt') into class's attribute private double[][] centroids.
*           input:
*               String, the file's path.
*           output:
*               None.
*
*       private double calculate_distance(double[] vector1, double[] vector2){}
*           use:
*               Calculate the certain feature with certain centroid distances.
*           input:
*               double[] vector1: certain feature.
*               double[] vector2: certain centroid.
*           output:
*               distance between certain feature and certain centroid (double).
*
*       static double[] indexesOfTopElements(double[] orig, int nummin) {}
*           use:
*               Sort to get the 'nummin' top small numbers from thr array.
*           input:
*               double[] orig: the original array you want to get top K elements.
*               int nummin: The K number you need.
*           output:
*               result contains nummin elements array.
*
*        double[] float2double(float[] src){}
*           use:
*               trandform float array to double array.
*           input:
*               float array.
*           output:
*               double array.
*
*       double[] Get_near_clusters_id(IValue[] outputs){}
*           use:
*               get output from forward and calculate the output with all centroids
*           input:
*               IValue[] outputs. Get Tensor type  with IValue.
*           output:
*                return near_cluster;
*                return double[]
*
* */

public class Autoencoder{

    private Module model;
    private String ModelPath;
    DataFrame df = new DataFrame<>();
    private double[][] centroids = null;
    Properties props = new Properties();


    public Autoencoder(String modelPath, Properties props) throws IOException {
        //model 轉換必須是用 cpu 的 model 轉換而來，否則 GPU id 不為空
        this.ModelPath = modelPath;
        this.props = props;
        //this.df = df;
        //int avg_time_stamp_num = this.df.length();
    }

    void initialize(){
        this.model = Module.load(this.ModelPath);
    }

    void InputData(DataFrame df){
        this.df = df;
    }

    private float[] transDim(Double[][] src){
        float[] dst = new float[src.length*src[0].length];
        int count = 0;
        for(int row = 0; row < src.length; row ++) {
            for (int col = 0; col < src[row].length; col++) {
                dst[count] = src[row][col].floatValue();
                count++;
            }
        }
        return dst;
    }

    double GetLabels(){
        int label_columns = Integer.parseInt(this.props.getProperty("label_column"));
        DataFrame labels = this.df.retain(label_columns);
        Double[] data = new Double[labels.length()];
        data = (Double[]) labels.toArray(data);
        return data[0];
    }

    private Integer[] GetPropArray(String prop){
        String[] integerStrings = prop.split(",");
        Integer[] dst = new Integer[integerStrings.length];
        for(int i = 0; i < integerStrings.length; i++){
            dst[i] = Integer.parseInt(integerStrings[i]);
        }
        return dst;
    }

    private float[] fdecimalFormat(float[] src){
        float[] rst = new float[src.length];
        DecimalFormat df = new DecimalFormat( "##.#######");
        for(int i = 0; i < src.length;i++){
            rst[i] = Float.parseFloat(df.format(src[i]));
            // System.out.println("rst: "+rst[i]);
        }
        return rst;
    }

    private double[] ddecimalFormat(double[] src){
        double[] rst = new double[src.length];
        DecimalFormat df = new DecimalFormat( "##.########");
        for(int i = 0; i < src.length;i++){
            rst[i] = Double.parseDouble(df.format(src[i]));
            // System.out.println("rst: "+rst[i]);
        }
        return rst;
    }

    //After join column
    // [Timestamp, LAccelerometer1, LAccelerometer2, LAccelerometer3,
    // LGyroscope1, LGyroscope2, LGyroscope3, LQuaternion1, LQuaternion2, LQuaternion3, LQuaternion4,
    // RAccelerometer1, RAccelerometer2, RAccelerometer3,
    // RGyroscope1, RGyroscope2, RGyroscope3, RQuaternion1, RQuaternion2, RQuaternion3, RQuaternion4,
    // Label, 0_left, 1_left, 2_left, 3_left, 4_left, 5_left, 0_right, 1_right, 2_right, 3_right, 4_right, 5_right]

    private Double[][] preprocess(){
        String gyro_column1 = this.props.getProperty("gyro_column1");
        String Q_column1 = this.props.getProperty("Q_column1");
        String gyro_column2 = this.props.getProperty("gyro_column2");
        String Q_column2 = this.props.getProperty("Q_column2");
        String gravity_column1 = this.props.getProperty("gravity_column1");
        String body_column1 = this.props.getProperty("body_column1");
        String gravity_column2 = this.props.getProperty("gravity_column2");
        String body_column2 = this.props.getProperty("body_column2");
        String All = gyro_column1 + "," + Q_column1 + "," + gyro_column2 + "," + Q_column2 + "," + gravity_column1 + "," + body_column1 + "," + gravity_column2 + "," + body_column2;
        System.out.println("All get data="+All);
        Integer[] data_columns = GetPropArray(All);
        int gravity_body_normalize_basis =  Integer.parseInt(this.props.getProperty("gravity_body_normalize_basis"));
        int gyro_q_normalize_basis = Integer.parseInt(this.props.getProperty("gyro_q_normalize_basis"));
        DataFrame dff = this.df.retain(data_columns);
        Double[][] data = new Double[dff.length()][dff.size()];
        data = (Double[][]) dff.toArray(data);
        int des = 14;

        for(int a = 0; a < data.length; a++){
            for(int b = 0; b < des; b++){
                data[a][b] = data[a][b] / gyro_q_normalize_basis;
            }
        }
        for(int a = 0; a < data.length; a++){
            for(int b = des; b < data[a].length; b++){
                data[a][b] = data[a][b] / gravity_body_normalize_basis;
            }
        }
        return data;
    }

    IValue[] forward(){
        Double[][] data = preprocess();
        float[] data1D = transDim(data);
        data1D = fdecimalFormat(data1D);
        long[] shape = {1, 1, data.length, data[0].length};
        Tensor inputTensor = Tensor.fromBlob(data1D, shape);
        float[] tmp = inputTensor.getDataAsFloatArray();
        System.out.println("inputTensor shape: "+ Arrays.toString(inputTensor.shape()));
        System.out.println("inputTensor Arraytype: "+ Arrays.toString(tmp));
        IValue inputs = IValue.from(inputTensor);
        //System.out.println(Arrays.toString(this.model.forward(inputs).toTuple()));
        IValue[] outputs = this.model.forward(inputs).toTuple();
        /*
        Tensor output = outputs[0].toTensor();
        System.out.println("output: "+output);
        Tensor latents = outputs[1].toTensor();
        float[] latents_data = latents.getDataAsFloatArray();
        System.out.println("latents: "+Arrays.toString(latents_data));
         */
        return outputs;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    void Load_centroids(String Path) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(Path);
        CSVFile csvFile = new CSVFile(inputStream);
        List AllList = csvFile.read();
        this.centroids = new double[AllList.size()][((double[]) AllList.get(0)).length];
        for(int i = 0; i < AllList.size(); i++){
            if(this.centroids[i].length == ((double[]) AllList.get(i)).length) {
                this.centroids[i] = (double[]) AllList.get(i);
            }
            else
                System.out.println("Error: not correct feature number.");
        }
    }


    private double calculate_distance(double[] vector1, double[] vector2){
        double dst = 0.0;
        if(vector1.length == vector2.length){
            for(int i = 0; i < vector1.length;i++){
                dst += Math.pow((vector1[i]-vector2[i]), 2);
            }
            dst = Math.sqrt(dst);
        }
        return dst;
    }


    static double[] indexesOfTopElements(double[] orig, int nummin) {
        double[] copy = Arrays.copyOf(orig,orig.length);
        Arrays.sort(copy);
        double[] honey = Arrays.copyOfRange(copy,0, nummin);
        System.out.print("top 20 near distance: "+ Arrays.toString(honey));
        double[] result = new double[nummin];
        int resultPos = 0;
        for(int i = 0; i < honey.length;i++){
            for(int j = 0; j < orig.length;j++){
                if(honey[i] == orig[j]){
                    result[resultPos++] = j;
                }
            }
        }
        return result;
    }

    double[] float2double(float[] src){
        double[] dst = new double[src.length];
        for(int i = 0; i < src.length;i++)
            dst[i] = src[i];
        return dst;
    }

    double[] Get_near_clusters_id(IValue[] outputs){
        int near_num = Integer.parseInt(this.props.getProperty("near_num"));
        Tensor latents = outputs[1].toTensor();
        float[] latents2 = latents.getDataAsFloatArray();
        System.out.println("------------------------------------------------");
        System.out.println("Action base prediction latents: "+ Arrays.toString(latents2));
        System.out.println("------------------------------------------------");
        double[] feature_array = float2double(latents2);
        // feature_array = ddecimalFormat(feature_array);
        double[] short_centroids = new double[this.centroids.length];
        double[] dist = new double[this.centroids.length];
        for(int i = 0;i < this.centroids.length; i++){
            dist[i] = calculate_distance(feature_array, this.centroids[i]);
        }
        // dist = ddecimalFormat(dist);
        System.out.println("------------------------------------------------");
        System.out.println("The distance between outputs and centroids: "+ Arrays.toString(dist));
        System.out.println("------------------------------------------------");
        double[] near_cluster = indexesOfTopElements(dist, near_num);
        // double[] near_cluster = ddecimalFormat(dist);
        return near_cluster;
    }


}
