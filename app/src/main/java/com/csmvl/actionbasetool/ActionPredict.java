package com.csmvl.actionbasetool;

import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import org.pytorch.IValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import joinery.DataFrame;

public class ActionPredict {
    private String centroid_path;
    private String model_path;
    private String classifier_path;
    private String props_path;
    private Properties props = new Properties();
    private int timestamp = 0;
    private Autoencoder autoencoder = null;
    private Xgboost_classifier xgboost = null;
    private Preprocessing preprocess = null;
    private DataFrame Aligndata = new DataFrame<>();
    private double[] output_near_cluster;

    /*
        //私有目錄(SD card)
        File modelDir = getExternalFilesDir("data/model/");
        File testDir = getExternalFilesDir("data/test/");
    */

    // 公有目錄
    File InputDir = Environment.getExternalStoragePublicDirectory("Documents/data/Input/");
    // read Original signal text
    File OrgSignal = new File(InputDir, "Original_Signal.txt");
    //公有目錄
    File root = Environment.getExternalStoragePublicDirectory("Documents/txtdata/Dataset_sliding_window_82/");
    // read All files name
    File[] files = root.listFiles();

    public ActionPredict(String config_file_path, String model_file_path, String centroid_file_path, String classifier_file_path) {
        this.props_path = config_file_path;
        this.model_path = model_file_path;
        this.centroid_path = centroid_file_path;
        this.classifier_path = classifier_file_path;
        this.output_near_cluster = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void initialize() throws IOException {
        this.output_near_cluster = null;
        // load config file
        this.props.load(new FileInputStream(this.props_path));
        // initialize timestamp
        this.timestamp = Integer.parseInt(props.getProperty("data_avg_size"));
        // Create Prepossessing object
        this.preprocess = new Preprocessing(this.props, this.timestamp);
        this.preprocess.initialize();
        // Set Algorithm, load model
        this.autoencoder = new Autoencoder(this.model_path, props);
        // Load centroids data
        this.autoencoder.initialize();
        this.autoencoder.Load_centroids(centroid_path);
        //Create Xgboost object
        File classifier_model = new File(this.classifier_path);
        this.xgboost = new Xgboost_classifier(classifier_model);
        this.xgboost.initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void getInputs(String[] data) throws IOException {
        this.preprocess.getInputs(data);
        // Preprocessed data
        this.preprocess.AlignData();
        this.Aligndata = this.preprocess.getData();
        // this.preprocess.initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void getData() throws IOException {
        // Get test data
        // this.preprocess.readFile(OrgSignal);
        // this.preprocess.GetOriginalList();
        // this.preprocess.readFiles(this.files);
        // this.preprocess.GetFilesList();
        // Preprocessed data
        this.Aligndata = this.preprocess.AlignData();
        if(this.Aligndata.length() < this.timestamp){
            this.preprocess.GetFilesList();
            this.Aligndata = this.preprocess.AlignData();
        }
        // this.preprocess.initialize();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    double forward() throws FileNotFoundException {

        // Set Algorithm, load model

        this.autoencoder.InputData(this.Aligndata);
        //this.Aligndata = new DataFrame<>();

        // Get Predict output
        IValue[] outputs = this.autoencoder.forward();


        // Get top 20 nearly center of clusters
        this.output_near_cluster = this.autoencoder.Get_near_clusters_id(outputs);

        System.out.println("-------------------------------------------------");
        System.out.println("output_near_cluster: "+ Arrays.toString(output_near_cluster));
        System.out.println("-------------------------------------------------");


        // Get test data label
        // double label = this.autoencoder.GetLabels();

        // Get the input(20 near clusters sequence) from the data (input_file)
        // double[] denseArray = this.xgboost.GetDenseArray(8);

        // Get the data accuracy
        // double accuracy = X1.GetAccuracy();

        // return the prediction result

        return this.xgboost.GetPrediction(output_near_cluster);

    }

    void close(){
        this.props.clear();
    }

    double getlabel(){
        return autoencoder.GetLabels();
    }


}
