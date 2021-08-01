package com.y3033108;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/* 楽器チューナー
 * MainActivity
 * マイクから音を拾い周波数分析をするAcousticAnalysisクラス
 * メトロノームの機能を実現させるMFunctionクラスが含まれる
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private static final int PERMISSION = 1;  //権限の許可のための変数
    static Point view = new Point(0, 0);  //画面サイズの取得のための変数
    ImageButton btss; //再生停止ボタン
    AcousticAnalysis aa;  //周波数分析をするAcousticAnalysisクラス
    ImageView bar;
    static TextView L1, L2, L3, L4, L5, L6, L7; //テンポ表示のためのランプ
    static TextView[] Lamp = {L1, L2, L3, L4, L5, L6, L7};
    Handler handler = new Handler(Looper.getMainLooper());
    Timer timer; //メトロノームのための周期的な実行をするTimerクラス
    MFunction mf; //Timerで呼び出される機能のクラス

    TextView NODE; //音程表示のテキスト
    TextView standard; //基準周波数表示のテキスト
    ImageView meter; //メーター表示

    Button stan_p,stan_m,change; //チューナーの設定ボタン

    Button plus,minus; //テンポの変更ボタン
    SeekBar seekBar; //テンポの変更シークバー

    //拍子変更スピナー
    Spinner beat;
    Spinner rhythm;

    LinearLayout showMtr;
    Context con = this;

    static AudioTrack audioTrack; //音楽ファイル再生のためのインスタンス

    //オーディオデータを再生するための配列
    static byte[] wavData1 = null;
    static byte[] wavData2 = null;

    int tmp = 60; //テンポの変数（初期設定は60）
    int setTime = 1000; //Timer処理を行う間隔(ms)
    static int mode = 0; //テンポのモード(0:4分音符,1:8分音符,2:三連符,3:16分音符)

    static int mtrState = 0; //メトロノームの状態(0:停止中,1:再生中)
    static int showFlag = 0; //表示の変更(0:音程,1:周波数)

    static int ball = 4; //ランプの数

    static int count = 0;
    static int haku = 0;

    static double PITCH = 442; //基準周波数の設定


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // マイクの権限が取れてなかったらリクエストをする
        //APIレベル23以降であれば(それ以下は許可を取るダイアログは不要)
        if (Build.VERSION.SDK_INT >= 23) {
            //マイクの権限が取れていなければ
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                //許可を取るダイアログを表示
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.RECORD_AUDIO }, PERMISSION);
            }
        }

        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_TunerApp);
        setContentView(R.layout.activity_main);

        setAudio(); //オーディオデータの準備

        //AudioTrackの準備
        int bufSize = android.media.AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufSize*2,
                AudioTrack.MODE_STATIC);

        //再生データの設定(オフセット44を除いて再生している)
        audioTrack.write(wavData2, 44, wavData2.length-44);
        audioTrack.setVolume(1);

        //各表示設定
        beat = findViewById(R.id.spinner);
        beat.setOnItemSelectedListener(this);
        rhythm = findViewById(R.id.spinner2);
        rhythm.setOnItemSelectedListener(this);
        rhythm.setSelection(3);
        standard = findViewById(R.id.standard);
        standard.setText(Integer.toString((int) PITCH));

        timer = new Timer(); //タイマーインスタンス作成

    }

    @Override
    //権限リクエストのダイアログの結果
    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        if (grantResults.length <= 0) {
            return;
        }
        switch (requestCode) {
            case PERMISSION: {
                //許可が取れた場合
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    //テキストを表示
                    Toast.makeText(this, "アプリを起動できませんでした", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            return;
        }
    }

    @Override
    //プルダウンで選択されたら呼ばれる関数
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        mode = beat.getSelectedItemPosition(); //テンポのモード設定

        //ランプの数の設定
        String item = (String) rhythm.getSelectedItem();
        ball = Integer.parseInt(item);

        count = 0; //カウントを０に

        if(mtrState == 1){
            setTime = (int)60000 / tmp;
            int time = 500;   //タイマー処理を行う間隔(ms)の変数
            switch(mode){    //mode:音符の変更
                case 0: time = setTime; break;  //0:4分音符
                case 1: time = setTime / 2; break;  //1:8分音符
                case 2: time = setTime / 3; break;  //2:3蓮符
                case 3: time = setTime / 4; break;  //3:16分音符
            }
            mf.cancel();    //タイマータスクを終了させる
            mf = null;      //タイマータスクを空にする
            mf  = new MFunction(this); //タイマータスクインスタンス作成
            timer.schedule(mf,0,time);    //タイマーを設定してタイマータスクを動作させる
        }

    }

    @Override
    //プルダウンで選択されなかったら呼ばれる関数
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    //クリックのイベントが起きたら呼ばれる
    public void onClick(View v) {
        //基準周波数を+するボタンが押された場合
        if(v == stan_p){
            //PITCHに+1して表示
            PITCH = PITCH + 1;
            if(PITCH >= 881) PITCH = 880;
            standard.setText(Integer.toString((int) PITCH));
        }

        //基準周波数を-するボタンが押された場合
        if(v == stan_m){
            //PITCHに-1して表示
            PITCH = PITCH - 1;
            if(PITCH <= 219) PITCH = 220;
            standard.setText(Integer.toString((int) PITCH));
        }

        //表示変更のボタンが押された場合
        if(v == change){
            //showFlagの0,1を変更する
            if(showFlag ==0){
                showFlag = 1;
            }
            else {
                showFlag = 0;
            }
        }

        //テンポを+1するボタンが押された場合
        if(v == plus){
            //テンポを+1してseekbarも同期させる
            tmp = tmp + 1;
            if(tmp > 252) tmp = 252;
            seekBar.setProgress(tmp);

            //メトロノームが動いている場合変更した結果を適用
            if(mtrState == 1){
                setTime = (int)60000 / tmp;
                int time = 500;   //タイマー処理を行う間隔(ms)の変数
                switch(mode){    //mode1:音符の変更
                    case 0: time = setTime; break;  //0:4分音符
                    case 1: time = setTime / 2; break;  //1:8分音符
                    case 2: time = setTime / 3; break;  //2:3蓮符
                    case 3: time = setTime / 4; break;  //3:16分音符
                }
                mf.cancel();    //タイマータスクを終了させる
                mf = null;      //タイマータスクを空にする
                mf  = new MFunction(this); //タイマータスクインスタンス作成
                timer.schedule(mf,0,time);    //タイマーを設定してタイマータスクを動作させる
            }

        }

        //テンポを-1するボタンが押された場合
        if(v == minus){
            //テンポを-1してseekbarも同期させる
            tmp = tmp - 1;
            if(tmp < 30) tmp = 30;
            seekBar.setProgress(tmp);

            //メトロノームが動いている場合変更した結果を適用
            if(mtrState == 1){
                setTime = (int)60000 / tmp;
                int time = 500;   //タイマー処理を行う間隔(ms)の変数
                switch(mode){    //mode1:音符の変更
                    case 0: time = setTime; break;  //0:4分音符
                    case 1: time = setTime / 2; break;  //1:8分音符
                    case 2: time = setTime / 3; break;  //2:3蓮符
                    case 3: time = setTime / 4; break;  //3:16分音符
                }
                mf.cancel();    //タイマータスクを終了させる
                mf = null;      //タイマータスクを空にする
                mf  = new MFunction(this); //タイマータスクインスタンス作成
                timer.schedule(mf,0,time);    //タイマーを設定してタイマータスクを動作させる
            }

        }

        //メトロノーム再生停止ボタンが押された場合
        if(v == btss){

            //メトロノーム停止中だったらメトロノームを再生する
            if(mtrState == 0){
                count = 0;
                setTime = (int)60000 / tmp;
                int time = 500;   //タイマー処理を行う間隔(ms)の変数
                switch(mode){    //mode1:音符の変更
                    case 0: time = setTime; break;  //0:4分音符
                    case 1: time = setTime / 2; break;  //1:8分音符
                    case 2: time = setTime / 3; break;  //2:3蓮符
                    case 3: time = setTime / 4; break;  //3:16分音符
                }
                mf  = new MFunction(this); //タイマータスクインスタンス作成
                timer.schedule(mf,0,time);    //タイマーを設定してタイマータスクを動作させる
                mtrState = 1;      //flgを1(メトロノーム動作中)に
                btss.setImageResource(R.drawable.stopbuttun);
            }

            //メトロノーム再生中だったらメトロノームを停止する
            else if(mtrState == 1){
                mf.cancel();    //タイマータスクを終了させる
                mf = null;      //タイマータスクを空にする
                mtrState = 0;        //flgを0(メトロノーム停止中)に
                btss.setImageResource(R.drawable.startbutton);
                for(int i = 0;i<ball;i++){
                    Lamp[i].setText("◯");
                }
            }
        }
    }

    @Override
    //フォーカスが移ると呼ばれる
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setLayout(); //レイアウトを設定する
    }

    //オーディオデータの準備(wavファイルを読み込んで配列に書き込む)
    public void setAudio(){

        InputStream input1 = null;
        InputStream input2 = null;

        try {
            // wavを読み込む
            input1 = getResources().openRawResource(R.raw.metro1w);
            wavData1 = new byte[input1.available()];

            // wavファイルを書き込む
            String readBytes = String.format(
                    Locale.US, "read bytes = %d",input1.read(wavData1));
            input1.close();
        } catch (FileNotFoundException fne) {
            fne.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally{
            try{
                if(input1 != null) input1.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        try {
            // wavを読み込む
            input2 = getResources().openRawResource(R.raw.metro2w);
            wavData2 = new byte[input2.available()];

            // wavファイルを書き込む
            String readBytes = String.format(
                    Locale.US, "read bytes = %d",input2.read(wavData2));
            input2.close();
        } catch (FileNotFoundException fne) {
            fne.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally{
            try{
                if(input2 != null) input2.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    //画面サイズの取得
    public static Point getViewSize(View View){
        Point point = new Point(0, 0);
        point.set(View.getWidth(), View.getHeight());
        return point;
    }

    //画面のviewのレイアウトの設定(取得した画面サイズに合わせて部品サイズを変更)
    public void setLayout(){
        //画面全体
        LinearLayout SIZE = findViewById(R.id.size);
        view = getViewSize(SIZE);

        //音程表示部
        LinearLayout showNode = findViewById(R.id.showNode);
        showNode.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*1/10));

        //メーター表示部
        FrameLayout showMeter = findViewById(R.id.showMeter);
        showMeter.setLayoutParams(new LinearLayout.LayoutParams(
                view.x*9/10,
                view.y*3/10));

        //チューナー設定ボタン表示部
        LinearLayout tunerButton = findViewById(R.id.showWiget);
        tunerButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*2/10));

        //メトロノーム表示部
        LinearLayout mtr = findViewById(R.id.Metronome);
        mtr.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*4/10));

        //メトロノーム内でのレイアウトの設定
        setMetronome();

        //各種ボタンやViewのリスナー登録などの設定
        stan_p = findViewById(R.id.stanP);
        stan_p.setOnClickListener(this);

        stan_m = findViewById(R.id.stanM);
        stan_m.setOnClickListener(this);

        change = findViewById(R.id.change);
        change.setOnClickListener(this);

        bar = findViewById(R.id.bar);

        NODE = findViewById(R.id.node);

        meter = findViewById(R.id.meter);

        //AcousticAnalysisクラスの作成とスレッドのスタート
        NODE.setText("");
        aa = new AcousticAnalysis(NODE,bar,view);
        aa.start();
    }

    //メトロノーム部のviewのレイアウトの設定(取得した画面サイズに合わせて部品サイズを変更)
    public void setMetronome(){
        //ランプ表示部
        showMtr = findViewById(R.id.ShowMtr);
        showMtr.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*1/10));

        //テンポ表示部
        LinearLayout showBPM = findViewById(R.id.ShowBPM);
        showBPM.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*1/20));

        //テンポ変更シークバー表示部
        LinearLayout sb = findViewById(R.id.Seekbar);
        sb.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*1/10));

        //拍子の変更プルダウンの表示部
        LinearLayout showPulldown = findViewById(R.id.MtrWiget);
        showPulldown.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*1/25));

        //再生停止ボタンの表示部
        LinearLayout playButton = findViewById(R.id.playButton);
        playButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*1/10));

        //ランプの設定と表示
        setLamp(ball,showMtr);

        //各種ボタンやViewのリスナー登録などの設定
        TextView tempo = findViewById(R.id.tempo);
        tempo.setText("♩ = "+tmp);

        btss = findViewById(R.id.btss);
        btss.setOnClickListener(this);

        //テンポ変更シークバーの設定
        seekBar = findViewById(R.id.tmpbar);
        seekBar.setProgress(tmp);//初期値
        seekBar.setMax(252);//最大値
        seekBar.setMin(30);//最小値
        //シークバーのリスナー登録
        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    //ツマミがドラッグされると呼ばれる
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        //テンポの表示を切り替える
                        tmp = progress;
                        tempo.setText("♩ = "+tmp);
                    }

                    @Override
                    //ツマミがタッチされた時に呼ばれる
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    //ツマミがリリースされた時に呼ばれる
                    public void onStopTrackingTouch(SeekBar seekBar) {

                        setTime = (int)60000 / tmp; //テンポの設定

                        setLamp(ball,showMtr); //ランプの表示

                        //メトロノームが再生中の場合
                        if(mtrState == 1){
                            int time = 500;   //タイマー処理を行う間隔(ms)の変数
                            switch(mode){    //mode1:音符の変更
                                case 0: time = setTime; break;  //0:4分音符
                                case 1: time = setTime / 2; break;  //1:8分音符
                                case 2: time = setTime / 3; break;  //2:3蓮符
                                case 3: time = setTime / 4; break;  //3:16分音符
                            }
                            mf.cancel();    //タイマータスクを終了させる
                            mf = null;      //タイマータスクを空にする
                            mf  = new MFunction(con); //タイマータスクインスタンス作成
                            timer.schedule(mf,0,time);    //タイマーを設定してタイマータスクを動作させる
                        }
                    }
                });

        plus = findViewById(R.id.plus);
        plus.setOnClickListener(this);

        minus = findViewById(R.id.minus);
        minus.setOnClickListener(this);
    }

    //ランプの設定、表示
    public void setLamp(int num, LinearLayout showMtr){
        showMtr.removeAllViews(); //ランプを全て消す
        //引数のnumの数だけランプを表示する
        for (int i = 0; i < num; i++){
            Lamp[i] = new TextView(this);
            Lamp[i].setTextSize(35);
            Lamp[i].setTextColor(Color.parseColor("red"));
            Lamp[i].setText("◯");
            showMtr.addView(Lamp[i]);
        }
    }

    //周波数分析、結果の表示をするクラス
    //別スレッドとして起動してから動き続ける
    class AcousticAnalysis extends Thread{

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
        double freq = 0;
        //音程
        String NN = "";
        double move = 0;
        //メーターのアニメーションのための変数
        int animesize = 0;
        float startX = 0;
        float startY = 0;
        float currentX = 0;
        //表示のためのview
        TextView Node;
        ImageView meterBar;
        //表示調整用カウント
        int count = 0;
        //音声データcopyのための配列
        double[] dataCopy = new double[FFT_SIZE];
        //音のpeakの検出のための閾値
        double thr = 55;

        //インスタンス
        AcousticAnalysis(TextView N, ImageView bar, Point view){
            //音声取得のためのバッファーサイズの取得
            bufSize = AudioRecord.getMinBufferSize(SAMPLING_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

            isRecording = true;

            //AudioRecordの作成
            audio = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufSize * 2);

            //音声取得開始
            audio.startRecording();

            Node = N;
            meterBar = bar;

            //メーターのバーのアニメーションのための初期位置取得
            animesize = ((view.x * 9/10) * 2750/3611)/100;
            startX = meterBar.getLeft();
            startY = meterBar.getTop();
            currentX = meterBar.getLeft();
        }

        //スレッドで実行される処理
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

                //前のデータをコピーしたものから読み取った音声のサイズ分を除いて格納
                for (int i = 10*bufSize; i < FFT_SIZE; i++) {
                    FFTdata[i-10*bufSize] =(double) dataCopy[i];
                }

                //読み取った音声データを10個分に重み付けして格納
                for(int k = 10; k > 0; k--){
                    for (int i = 0; i < bufSize; i++){
                        FFTdata[i+FFT_SIZE-k*bufSize] = (double) s[i];
                    }
                }

                //できたデータのコピーを取っておく
                dataCopy = FFTdata.clone();

                //窓関数をかけておく
                double[] rslt = hamming(FFTdata);
                //離散フーリエ変換
                fft.rdft(1, rslt);

                //フーリエ変換後のデシベルの計算
                double[] dbfs = new double[FFT_SIZE / 2];

                double max_db = -120;
                double max_i = 0;

                for (int i = 0; i < FFT_SIZE; i += 2) {
                    //55~4187Hz以外の周波数はカットする
                    if ( 2066 <= Math.abs(2121-resol*i/2) ) {
                        dbfs[i/2] = -120;
                    }

                    else {
                        dbfs[i / 2] = (int) (20 * Math.log10(Math.sqrt(Math.pow(rslt[i], 2)
                                + Math.pow(rslt[i + 1], 2)) / baseline));
                        //一番大きい周波数を探す
                        if (max_db < dbfs[i / 2]) {
                            max_db = dbfs[i / 2];
                            max_i = i * resol / 2;
                        }
                    }
                }

                //peakを検出してinに格納
                int in = find_peak(dbfs);

                //周波数を計算
                freq = in * resol;
                String moji = NN;
                //音程の計算
                scale(freq);

                //周波数表示の設定の場合そのまま一番大きい周波数を表示する
                if (showFlag ==1){
                    freq = max_i;
                }

                //表示のための設定
                //カウントが3以上になるまで表示しない
                if (moji.equals("") && count < 3){
                    count = count +1;
                }
                //カウントが3以上になり
                else if(count >= 3){
                    //前と表示している音程が同じ場合そのまま表示
                    if(moji.equals(NN)) {
                        count = count +1;
                        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
                        mainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                show();
                            }
                        });
                    }
                    //前と表示している音程が違う場合カウントをリセットして表示
                    else {
                        count = 0;
                        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
                        mainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                show();
                            }
                        });
                    }
                }
            }
            audio.stop();
            audio.release();
        }

        //音程を表示するためのメソッド
        public void show() {
            //周波数0の場合
            if (freq == 0 && count >= 3) {
                //音程表示は何も表示しない
                Node.setText("");
                //メーターは初期位置(中央へ移動)
                TranslateAnimation anime = new TranslateAnimation(
                        Animation.ABSOLUTE, currentX,
                        Animation.ABSOLUTE, (float) (startX + move * animesize),
                        Animation.ABSOLUTE, startY,
                        Animation.ABSOLUTE, startY);

                currentX = (float) (startX + move * animesize);

                anime.setDuration(100);
                // 繰り返し回数
                anime.setRepeatCount(0);
                // animationが終わったそのまま表示にする
                anime.setFillAfter(true);
                //アニメーションの開始
                meterBar.startAnimation(anime);

                //周波数表示設定の時は0.0Hzと表示
                if (showFlag ==1){
                    Node.setText("0.0 Hz");
                }
            }
            //周波数0ではない場合
            else {
                //音程表示設定の場合
                if (showFlag == 0) {
                    //メーターのバーを動かす
                    TranslateAnimation anime = new TranslateAnimation(
                            Animation.ABSOLUTE, currentX,
                            Animation.ABSOLUTE, (float) (startX + move * animesize),
                            Animation.ABSOLUTE, startY,
                            Animation.ABSOLUTE, startY);

                    currentX = (float) (startX + move * animesize);

                    anime.setDuration(100);
                    // 繰り返し回数
                    anime.setRepeatCount(0);
                    // animationが終わったそのまま表示にする
                    anime.setFillAfter(true);
                    //アニメーションの開始
                    meterBar.startAnimation(anime);

                    //音程をセットし表示
                    Node.setText(NN);
                }

                //周波数表示設定の場合
                else{
                    //小数点第二位で四捨五入して表示
                    double showF = ((double)Math.round(freq * 10))/10;
                    Node.setText(Double.toString(showF)+" Hz");
                }
            }
        }

        //ハミング窓関数
        public double[] hamming(double[] input) {
            //出力する配列
            double[] output = new double[input.length];

            for(int i = 0; i < input.length; i++){
                //窓関数の値を取得
                double  ham =  0.54 - 0.46 * Math.cos( 2.0 * Math.PI * i / (input.length-1) );
                output[i] = input[i] * ham;
            }

            return output;
        }

        //音程計算メソッド
        public void scale(double freq) {
            //拾った音のpeak周波数から音程を計算する
            int node = (int) Math.round(log2(freq/PITCH)*12);
            double DD = (double) node/12.0;
            double node_freq = Math.pow(2.0,DD) * PITCH;
            double cent = 1200 * log2(freq/node_freq);

            String[] Arr = {"A","B♭","B","C","C#","D","E♭","E","F","F#","G","A♭"};

            int node2 = node % 12;
            if(node2<0) node2 = node2 + 12;

            //計算した結果の音程の文字を格納
            NN = Arr[node2];

            //周波数が0の場合
            if (freq == 0){
                NN = "";
                cent = 0;
            }
            //アニメーションの動く距離
            move = cent;

            return;
        }

        //log2の関数
        public double log2(double x){
            return Math.log(x) / Math.log(2);
        }

        //配列中の最大値を一定間隔の間で見つけ、その最大値の要素数を返す
        public int Max_find(int s, int range, double[] d){
            int max_i = s;
            double max = d[s];
            for (int i = s; i < s+range && i < d.length; i++){
                if(d[i] > max){
                    max = d[i];
                    max_i = i;
                }
            }
            return max_i;
        }

        //配列中の最小値を引数のs~eの間で見つけ、その最小値の要素数を返す
        public int Min_find(int s, int e, double[] d){
            int min_i = s;
            double min = d[s];
            for (int i = s; i < e; i++){
                if(d[i] < min){
                    min = d[i];
                    min_i = i;
                }
            }
            return min_i;
        }

        //配列中のpeakを検出する
        public int find_peak(double[] d){
            //要素50個間隔で配列の最大値を探していく
            int len = d.length;
            int[] max_array = new int[len];
            int range = 50;
            int c = 0;
            //55Hzまでは周波数はカットしているので82から
            for (int i = 82; i < len - range; i++){
                int index = Max_find(i,range,d);
                if(index == i + range -1){
                    if (index == Max_find(i+1,range,d)){
                        int check = 0;
                        for (int k = 1; k < range; k++){
                            if(index == Max_find(i+k,range,d)){
                                check++;
                            }
                        }
                        if(check == range - 1){
                            max_array[c] = index;
                            c++;
                        }
                    }
                }
            }

            //最大値から最大値の間で最小値を探していく
            int[] min_array = new int[len];
            min_array[0] = Min_find(82,max_array[0],d);
            for(int i = 1; i < c; i++){
                min_array[i] = Min_find(max_array[i-1],max_array[i],d);
            }

            //最大値とその間の最小値の差が閾値以上であればその最大値をpeakとする
            List<Integer> peak = new ArrayList<>();
            for(int i = 0; i < c; i++){
                if(d[max_array[i]] - d[min_array[i]] >= thr){
                    peak.add(max_array[i]);
                }
            }

            //peakの中から最終的に使う周波数を決定する
            int out = 0;
            if(peak.isEmpty() == false) {
                int index = peak.get(0);
                //peakが一つであればその値を返す
                if(peak.size() == 1 && d[index] > -60){
                    out = index;
                }
                //2つ以上の場合倍音が含まれているかを確認する
                else if(peak.size() >= 2) {
                    //一番周波数が低いpeakとその次のpeakの値が倍音の関係かを確かめる
                    int check = peak.get(1) - index;
                    while(check > index + 3){
                        check = check - index;
                    }
                    for(int i = 1; i < 7; i++){
                        int k = Math.abs(check*i-index);
                        if(k <= 3){
                            out = check;
                            break;
                        }
                    }
                    //倍音がない場合peakの中で一番大きい値を使う
                    if (out == 0){
                        double max = 0;
                        for (int i = 0; i < peak.size(); i++){
                            int ind = peak.get(i);
                            if(d[ind] > -60 && max < d[ind]){
                                max = d[ind];
                                out = ind;
                            }
                        }
                    }
                }
            }

            return out;
        }
    }

    //TimerTaskのクラス
    //Timerで呼び出される周期を決定されその周期でrunが実行される
    class MFunction extends TimerTask {

        Context c; //コンテキスト

        //インスタンス
        MFunction(Context context){
            c = context;
            count = 0;
            haku = 0;
        }

        //Timerにより周期的に呼ばれる
        public void run(){
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //強拍のタイミング(haku=0)ではwavData2を再生,ランプの更新
                    if(count == 0 && haku == 0){
                        play(wavData2);
                        Lampchange();
                        haku = haku+1;
                    }
                    else if(count == ball && haku == 0){
                        play(wavData2);
                        Lampchange();
                        haku = haku+1;
                    }
                    //それ以外のタイミングではwavData1を再生,ランプの更新
                    else {
                        play(wavData1);
                        if(haku == 0) {
                            Lampchange();
                        }
                        haku = haku+1;
                    }
                    //hakuがランプの数まで到達したらリセット
                    if(haku >= mode+1){
                        haku = 0;
                    }
                }
            });
        }

        //音声の再生メソッド
        public void play(byte[] w){
            if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                //再生中の場合は止める
                audioTrack.stop();
                //再生バッファをクリアする
                audioTrack.flush();
            }

            //読み込み済データを再度読み出す
            audioTrack.reloadStaticData();
            //再生バッファにデータを書き込む
            audioTrack.write(w,44,w.length-44);

            //再生中にする
            audioTrack.play();
        }

        //ランプの表示更新メソッド
        public void Lampchange(){
            //ランプを次に進める,最後のランプだったら最初に戻す
            if(count == 0){
                Lamp[count].setText("●");
                count++;
            }
            else if(count >= ball) {
                count = 0;
                for(int i = 0;i<ball;i++){
                    Lamp[i].setText("◯");
                }
                Lamp[count].setText("●");
                count++;
            }
            else{
                for(int i = 0;i<ball;i++){
                    Lamp[i].setText("◯");
                }
                Lamp[count].setText("●");
                count++;
            }
        }
    }
}
