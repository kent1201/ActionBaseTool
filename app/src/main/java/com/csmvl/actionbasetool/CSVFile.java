package com.csmvl.actionbasetool;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CSVFile {
    InputStream inputStream;
    BufferedReader reader = null;

    public CSVFile(InputStream inputStream){
        this.inputStream = inputStream;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public List read(){
        List resultList = new ArrayList();
        reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(",");

                double[] drow = new double[row.length];
                for(int i = 0; i < row.length; i++){
                    drow[i] = Double.parseDouble(row[i]);
                }
                //System.out.println("row: "+ Arrays.toString(drow));
                resultList.add(drow);
            }
        }
        catch (IOException ex) {
            throw new RuntimeException("Error in reading CSV file: "+ex);
        }
        finally {
            try {
                inputStream.close();
                reader.close();
            }
            catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: "+e);
            }
        }
        return resultList;
    }
}
