package com.csmvl.actionbasetool;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.pytorch.IValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;

import joinery.DataFrame;


public class MainActivity extends AppCompatActivity {


    TextView tv1 = null;
    TextView tv2 = null;
    TextView tv3 = null;
    private Button btnCon;

    String centroid_path;
    String model_path;
    Properties props = new Properties();
    int timestamp = 0;
    Autoencoder autoencoder = null;
    Preprocessing preprocess = null;
    DataFrame Aligndata = new DataFrame<>();
    double[] output_near_cluster;


    //公有目錄
    String root = Environment.getExternalStorageDirectory().getPath();
    File data_file = new File(root, "Documents/txtdata/Dataset_sliding_window_82/");
    String input_data_string = String.valueOf(data_file);
    // read All files name
    final File[] files = data_file.listFiles();
    final int files_count = files.length;

    final String ouptut = "output";
    final String text = ".txt";
    final String[] outputfle = {null};


    // 寫入資料路徑
    String output_data_string = new String(root+"Documents/Data/");


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int STORAGE_PERMISSION_CODE = 101;
        final int WRITE_PERMISSION_CODE = 102;

        //要求權限

        checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                WRITE_PERMISSION_CODE);

        checkPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                STORAGE_PERMISSION_CODE);




        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //取得介面元件

        tv1 = findViewById(R.id.Input);
        tv2 = findViewById(R.id.Output);
        tv3 = findViewById(R.id.State);
        btnCon = (Button) findViewById(R.id.button);

        tv1.setText(input_data_string);
        tv2.setText(output_data_string);

        // Load Configure
        String assets_file_path = Utils.assetFilePath(this, "properties.properties");
        try {
            assert assets_file_path != null;
            this.props.load(new FileInputStream(assets_file_path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String model_name = props.getProperty("model_name");
        String centroids_name = props.getProperty("centroids_name");
        this.model_path = Utils.assetFilePath(this, model_name);
        this.centroid_path = Utils.assetFilePath(this, centroids_name);
        this.timestamp = Integer.parseInt(props.getProperty("data_avg_size"));

        tv3.setText("模型載入中");
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Load Model
                try {
                    preprocess = new Preprocessing(props, timestamp);
                    preprocess.initialize();
                    autoencoder = new Autoencoder(model_path, props);
                    autoencoder.initialize();
                    autoencoder.Load_centroids(centroid_path);
                } catch (IOException e) {
                    tv3.setText("模型載入失敗");
                    e.printStackTrace();
                }
                Message msg = new Message();
                msg.what = 112;
                String tmp = "模型載入成功";
                msg.obj = tmp;
                handler.sendMessage(msg);
            }
        }).start();

        this.props.clear();

        // button: Connected to bluetooth
        btnCon.setOnClickListener(btnConListener);

//        preprocess.readFiles(files);
//        assert files != null;
//        for(int i = 0; i < files.length; i++){
//            System.out.println("Files: "+files[i]);
//            try {
//                preprocess.initialize();
//                preprocess.GetFilesList();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            // Preprocessed data
//            Aligndata = preprocess.AlignData();
//            autoencoder.InputData(Aligndata);
//            IValue[] outputs = autoencoder.forward();
//            output_near_cluster = autoencoder.Get_near_clusters_id(outputs);
//            int label = (int) autoencoder.GetLabels();
//            if(label > 0){
//                String[] stringData = new String[output_near_cluster.length+1];
//                int count = 0;
//                for (count = 0; count < output_near_cluster.length; count++)
//                    stringData[count] = String.valueOf(output_near_cluster[count]);
//                stringData[count] = String.valueOf(label-1);
//                String TrainData = TextUtils.join(",", stringData);
//                System.out.println("\nTrainData: "+ TrainData);
//                if(i % 5000 == 0) {
//                    outputfle[0] = ouptut + i + text;
//                }
//                writeToFile(TrainData, outputfle[0]);
//            }
//
//        }
//        this.props.clear();

    }


    private Button.OnClickListener btnConListener = new Button.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onClick(final View v){
            // Start receive data and get prediction
            new Thread(new Runnable() {
                @Override
                public void run() {
                    preprocess.readFiles(files);
                    assert files != null;
                    for(int i = 0; i < files.length; i++){
                        System.out.println("Files: "+files[i]);
                        try {
                            preprocess.initialize();
                            preprocess.GetFilesList();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // Preprocessed data
                        Aligndata = preprocess.AlignData();
                        autoencoder.InputData(Aligndata);
                        IValue[] outputs = autoencoder.forward();
                        output_near_cluster = autoencoder.Get_near_clusters_id(outputs);
                        int label = (int) autoencoder.GetLabels();
                        if(label > 0){
                            String[] stringData = new String[output_near_cluster.length+1];
                            int count = 0;
                            for (count = 0; count < output_near_cluster.length; count++)
                                stringData[count] = String.valueOf(output_near_cluster[count]);
                            stringData[count] = String.valueOf(label-1);
                            String TrainData = TextUtils.join(",", stringData);
                            Message msg = new Message();
                            msg.what = 112;
                            String tmp = TrainData;
                            msg.obj = tmp;
                            handler.sendMessage(msg);
                            if(i % 5000 == 0) {
                                outputfle[0] = ouptut + i + text;
                            }
                            writeToFile(TrainData, outputfle[0]);
                        }
                    }
                    Message msg = new Message();
                    msg.what = 112;
                    String tmp = "資料處理完成";
                    msg.obj = tmp;
                    handler.sendMessage(msg);
                }
            }).start();
        }
    };

    public void checkPermission(String permission, int requestCode)
    {

        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(
                MainActivity.this,
                permission)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat
                    .requestPermissions(
                            MainActivity.this,
                            new String[] { permission },
                            requestCode);
        }
        else {
            Toast
                    .makeText(MainActivity.this,
                            "Permission already granted",
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }

    // Updata UI when you get new message
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 112:
                    String res = (String) msg.obj;
                    tv3.setText(res);
                    break;
            }
        }
    };

    private void writeToFile(String data, String outputfile_name) {
        try {
            String root = Environment.getExternalStorageDirectory().getPath();
            File output_data_file = new File(root, "Documents/Data/");
            File filename = new File(output_data_file, outputfile_name);
            FileOutputStream fout = new FileOutputStream(filename, true);
            String new_data = data + "\n";
            fout.write(new_data.getBytes());
            fout.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


}
