package com.y3033108;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AcousticAnalysis extends Thread{

    // サンプリングレート
    int SAMPLING_RATE = 44100;
    // FFTのポイント数
    int FFT_SIZE = 65536;
    // ベースライン
    double baseline = Math.pow(2, 15) * FFT_SIZE * Math.sqrt(2);
    // 分解能の計算
    double resol = ((SAMPLING_RATE / (double) FFT_SIZE));
    //音声録音用のクラス
    AudioRecord audio;
    //録音の開始/停止を制御するためのboolean変数
    boolean isRecording = false;
    //バッファサイズ
    int bufSize;
    //ピーク周波数
    static double freq = 0;

    double[] dataCopy = new double[FFT_SIZE];

    double PITCH = 440.0;


    AcousticAnalysis(){
        bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        isRecording = true;

        //AudioRecordの作成
        audio = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufSize * 2);

        audio.startRecording();
    }

    public void run(){

        byte buf[] = new byte[bufSize * 2];
        while (isRecording) {
            //bufに録音データを読み込む
            audio.read(buf, 0, buf.length);

            //エンディアン変換
            //録音はリトルエンディアンだが、フーリエ変換はビックエンディアン
            ByteBuffer bf = ByteBuffer.wrap(buf);
            //bfをリトルエンディアンに設定
            bf.order(ByteOrder.LITTLE_ENDIAN);
            short[] s = new short[bufSize];
            //bfをの値をsに格納
            for (int i = bf.position(); i < bf.capacity() / 2; i++) {
                s[i] = bf.getShort();
            }

            //FFT4gクラスの作成と計算
            fft4g fft = new fft4g(FFT_SIZE);
            double[] FFTdata = new double[FFT_SIZE];


            for (int i = bufSize; i < FFT_SIZE; i++) {
                FFTdata[i-bufSize] =(double) dataCopy[i];
            }

            for (int i = 0; i < bufSize; i++){
                FFTdata[i+FFT_SIZE-bufSize] = (double) s[i];
            }

            dataCopy = FFTdata.clone();

            double[] rslt = hamming(FFTdata);
            //離散フーリエ変換
            fft.rdft(1, rslt);


            //フーリエ変換後のデシベルの計算
            double[] dbfs = new double[FFT_SIZE / 2];
            double max_db = -120d;
            int max_i = 0;
            for (int i = 0; i < FFT_SIZE; i += 2) {
                dbfs[i / 2] = (int) (20 * Math.log10(Math.sqrt(Math.pow(rslt[i], 2)
                        + Math.pow(rslt[i + 1], 2)) / baseline));
                //ピーク検出
                if (max_db < dbfs[i / 2]) {
                    max_db = dbfs[i / 2];
                    max_i = i / 2;
                }
            }

            //ピーク周波数の計算
            freq = resol * max_i;

            scale(freq);


        }
        audio.stop();
        audio.release();
    }

    public double[] hamming(double[] input) {

        double[] output = new double[input.length];

        for(int i = 0; i < input.length; i++){
            // 窓関数の値を取得
            double  ham =  0.54 - 0.46 * Math.cos( 2.0 * Math.PI * i / (input.length-1) );
            output[i] = input[i] * ham;
        }

        return output;
    }

    public void scale(double freq) {
        int node = (int) Math.round(log2(freq/PITCH)*12);
        double DD = (double) node/12.0;
        double node_freq = Math.pow(2.0,DD) * PITCH;
        double Error = freq - node_freq;
        int node_e = 0;
        if (Error>0) node_e = node+1;
        else node_e = node-1;
        double DC = (double) node_e/12.0;
        double freq_e = Math.pow(2.0,DC) * PITCH;
        double cent = Error / Math.abs(node_freq - freq_e);

        String[] Arr = {"A","B♭","B","C","C#","D","E♭","E","F","F#","G","A♭"};

        int p = (node + 9) / 12;

        int pp = 4 + p;

        int node2 = node % 12;

        if(node2<0) node2 = node2 + 12;



        Log.d("fft","周波数："+ freq + "  音程："+ Arr[node2] + pp +"  度数："+ Math.toDegrees(Math.asin(cent)));


        return;
    }

    public double log2(double x){
        return Math.log(x) / Math.log(2);
    }


}
