package com.csmvl.actionbasetool;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import biz.k11i.xgboost.Predictor;
import biz.k11i.xgboost.util.FVec;

public class Xgboost_classifier {

    //parameters
    // private List inputList;
    // private double[] labelList;
    private Predictor predictor = null;

    File model_file;

    // initialize
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Xgboost_classifier(File model_file) throws IOException {
        this.model_file = model_file;
        // this.inputList = GetInputList();
        // this.labelList = GetLabelList();
    }

    //Create Predictor object
    public void initialize() throws IOException {
        //create predictor
        try {
            this.predictor = new Predictor(new FileInputStream(this.model_file));
        } catch (IOException e) {
            System.out.println("------------------------");
            System.out.println("Create Predictor Failed.");
            System.out.println("------------------------");
            e.printStackTrace();
        }
    }

    // Get input data (list type)
    /*
    @RequiresApi(api = Build.VERSION_CODES.N)
    List GetInputList() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(this.input_file);
        CSVFile csvFile = new CSVFile(inputStream);
        final List scoreList = csvFile.read();
        return scoreList;
    }
     */


    // Get ground truth data (double array type)
    /*
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    double[] GetLabelList(){
        ArrayList<String> labels = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(this.label_file))) {
            while (br.ready()) {
                labels.add(br.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int labels_num = labels.size();
        double[] labelArray = new double[labels_num];
        for(int i =0;i < labels_num;i++){
            labelArray[i] = Double.parseDouble(labels.get(i));
            //System.out.println(labelArray[i]);
        }
        return  labelArray;
    }
    */

    /*
    // Get specific data in input data (double array type)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public double[] GetDenseArray(int i) throws FileNotFoundException {
        double[] denseArray = (double[]) this.inputList.get(i);
        return denseArray;
    }

     */


    /*
    // Get specific ground truth in ground truth data (double type)
    public double GetLabel(int i){
        return this.labelList[i];
    }
     */

    // Get the prediction result of the input(double type)
    @RequiresApi(api = Build.VERSION_CODES.N)
    public double GetPrediction(double[] denseArray) throws FileNotFoundException {
        FVec fVecDense = FVec.Transformer.fromArray(
                denseArray,
                true );
        double[] prediction = this.predictor.predict(fVecDense);
        return prediction[0];
    }
    /*
    // Get Accuracy of the data
    @RequiresApi(api = Build.VERSION_CODES.N)
    public double GetAccuracy() throws FileNotFoundException {
        int labels_num = this.labelList.length;
        int seq_num = this.inputList.size();
        if (labels_num == seq_num) {
            System.out.println("Correct number:" + labels_num);
            int crr = 0;
            for (int i = 0; i < seq_num; i++) {
                double[] denseArray = GetDenseArray(i);
                FVec fVecDense2 = FVec.Transformer.fromArray(
                        denseArray,
                        true); // treat zero element as N/A);
                double[] prediction2 = predictor.predict(fVecDense2);
                //System.out.println("Prediction: " + prediction2[0]);
                //System.out.println("label: " + labelList[i]);
                if (prediction2[0] == labelList[i]) {
                    //System.out.println("Is same");
                    crr++;
                }

            }
            double acc = crr / (double) labels_num;
            System.out.println("Accuracy is: " + acc);
            return acc;
        }
        else{
            System.out.println("Number Error!!!");
            return 1;
        }
    }

     */
}
