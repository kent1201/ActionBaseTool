package com.csmvl.actionbasetool;

import java.util.Arrays;

public class SignalProcessing {

    public Double median(Double[] data){
        //System.out.println("Before sorted: "+data[0]+"\t"+data[1]+"\t"+data[2]);
        Double[] tmp = data.clone();
        Arrays.sort(tmp);
        //System.out.println("After sorted: "+data[0]+"\t"+data[1]+"\t"+data[2]);
        Double median;
        if (tmp.length % 2 == 0)
            median = (tmp[tmp.length/2] + tmp[tmp.length/2 - 1])/2;
        else
            median = tmp[tmp.length/2];
        //System.out.println("The median number is "+median);
        return median;
    }

    private Double[][] Copy2DArray(Double[][] src){
        Double[][] rst = new Double[src.length][src[0].length];
        for(int i = 0;i < src.length;i++){
            for(int j = 0;j < src[0].length;j++){
                rst[i][j] = src[i][j];
            }
        }
        return rst;
    }


    public Double[][] medianfilter(Double[][] data, int[] size){
        Double[] tmpdata = new Double[(size[0]*size[1])];
        Double[][] resultdata = Copy2DArray(data);
        for(int row = 0; row < data.length;row++){
            for(int col = 0;col < data[0].length;col++){
                int tt = 0;
                for(int i = -(size[0]/2); i <= (size[0]/2); i++){
                    for(int j = -(size[1]/2);j <= (size[1]/2);j++){
                        if(row+i < 0.0 || col+j < 0.0)
                            tmpdata[tt++] = 0.0;
                        else if(row+i >= data.length || col+j >= data[0].length)
                            tmpdata[tt++] = 0.0;
                        else
                            tmpdata[tt++] = data[row+i][col+j];
                    }
                }
                //System.out.println("tmpdata: ");
                //System.out.println(tmpdata[0]+"\t"+tmpdata[1]+"\t"+tmpdata[2]);
                resultdata[row][col] = median(tmpdata);
            }
        }
        return resultdata;
    }

    public Double cheb1ord(Double Wp, Double Ws, Double gpass, Double gstop, boolean analog, Double fs){

        if(fs != -1){
            if(analog){
                System.out.println("fs cannot be specified for an analog filter");
            }
            Wp = 2 * Wp / fs;
            Ws = 2 * Ws / fs;

        }

        int filter_type = (int) (2 * (Wp -1));
        if(Wp < Ws){
            filter_type+=1;
        }
        else{
            filter_type+=2;
        }

        Double passb = 0.0;
        Double stopb = 0.0;
        if(!analog){
            passb = Math.tan(Math.PI * Wp / 2.0);
            stopb = Math.tan(Math.PI * Ws / 2.0);
        }
        else{
            passb = Wp * 1.0;
            stopb = Ws * 1.0;
        }
        // Natural frequencies are just the passband edges
        Double wn = 0.0;
        if(!analog) {
            wn = (2.0 / Math.PI) * Math.atan(passb);
        }
        else{
            wn = passb;
        }

        if(fs != -1)
            wn = wn * fs / 2.0;
        return wn;

    }

    public Double[][] linear_filter(int order, double[] bCoeff, double[] aCoeff, Double[][] src){


        Double[][] rst = new Double[src.length][src[0].length];
        Double[][] unfilteredSignal = Copy2DArray(src);


        // initialize X(0), X(1)
        double[][] filteredSignal =   new double[src.length][src[0].length];
        for(int i = 0;i < filteredSignal.length;i++){
            for(int j = 0;j < filteredSignal[0].length;j++)
            filteredSignal[i][j] = 0.0;
        }
        double filterSampleA = 0;
        double filterSampleB = 0;
        for(int col = 0; col < src[0].length;col++) {
            for(int i = 0; i < order; i++){
                for(int j = 0; j < i+1;j++){
                    filterSampleA = filterSampleA + aCoeff[j] * unfilteredSignal[i-j][col];
                }
                for(int j = 1; j < i+1;j++){
                    filterSampleB = filterSampleB + bCoeff[j] * filteredSignal[i-j][col];
                }
                filteredSignal[i][col] = filterSampleA - filterSampleB;
                rst[i][col] = filteredSignal[i][col];
                filterSampleA = 0.0;
                filterSampleB = 0.0;
            }
        }

        filterSampleA = 0.0;
        filterSampleB = 0.0;
        //start from X(order = 2)
        for(int col = 0; col < src[0].length;col++){
            for(int row = order; row < src.length;row++){
                for(int j = 0; j < order+1;j++){
                    filterSampleA = filterSampleA + aCoeff[j] * unfilteredSignal[row-j][col];
                }
                for(int j = 1; j < order+1;j++){
                    filterSampleB = filterSampleB + bCoeff[j] * filteredSignal[row-j][col];
                }
                filteredSignal[row][col] = filterSampleA - filterSampleB;
                rst[row][col] = filteredSignal[row][col];
                filterSampleA = 0.0;
                filterSampleB = 0.0;
            }
        }
        return rst;
    }

}
