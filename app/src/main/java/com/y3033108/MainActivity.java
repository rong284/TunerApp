package com.y3033108;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int PERMISSION = 1;
    Point view = new Point(0, 0);;
    View V;
    PopupWindow popupWindow;
    RadioGroup G1;
    RadioGroup G2;
    RadioGroup G3;
    TextView Sound;
    int[] idlist;
    int[] Artlist;
    int Ontei = 0;
    int Articulation = 0;
    String[] Arr = {"A","B","C","D","E","F","G"};
    String[] art_Arr = {" ","#","♭"};
    Button b;
    MakeSound ms;
    AcousticAnalysis aa;
    boolean flg = false;
    ImageView bar;

    TextView FREQ ;
    TextView NODE ;
    TextView CENT ;


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
    protected void onPostResume() {
        super.onPostResume();

    }

    @Override
    public void onClick(View v) {
        if(v == b) {
            // 切替ボタン押下時にポップアップウィンドウの表示、非表示を切り替える
            if (popupWindow.isShowing()) {

                popupWindow.dismiss();
                aa = new AcousticAnalysis(FREQ,NODE,CENT,bar,view);
                aa.start();

            } else {
                aa.stoprun();
                aa = null;
                popupWindow.showAtLocation(V, Gravity.CENTER, 0, 0);
            }
        }
//        else {
//            if(flg == false) {
//                ms = new MakeSound();
//                ms.start();
//                flg = true;
//            }
//            else {
//                ms.stoprun();
//                ms = null;
//                flg = false;
//            }
//        }

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        setLayout();
        setWindow();
    }

    public static Point getViewSize(View View){
        Point point = new Point(0, 0);
        point.set(View.getWidth(), View.getHeight());

        return point;
    }

    public void setLayout(){
        LinearLayout r = findViewById(R.id.size);
        view = getViewSize(r);

        Log.d("log","layoutWidth="+view.x);
        Log.d("log","layoutHeight="+view.y);

        LinearLayout showNode = findViewById(R.id.showNode);
        showNode.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*1/10));

        FrameLayout showMeter = findViewById(R.id.showMeter);
        showMeter.setLayoutParams(new LinearLayout.LayoutParams(
                view.x*9/10,
                view.y*3/10));

        LinearLayout showWiget = findViewById(R.id.showWiget);
        showWiget.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                view.y*6/10));

        bar = findViewById(R.id.bar);

        FREQ = findViewById(R.id.freq);
        NODE = findViewById(R.id.node);
        CENT = findViewById(R.id.cent);

        b = findViewById(R.id.button);
        b.setOnClickListener(this);
        ImageView meter = findViewById(R.id.meter);

        aa = new AcousticAnalysis(FREQ,NODE,CENT,bar,view);
        aa.start();
    }



    public void setWindow(){
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // ポップアップ用のViewをpopupxmlから読み込む
        View popupView = (View)inflater.inflate(R.layout.popup, null);

        // レイアウトパラメータをセット
        popupView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        // PopupWindowを紐づけるViewのインスタンスを取得
        V = findViewById(R.id.button);

        // viewに紐づけたPopupWindowインスタンスを生成
        popupWindow = new PopupWindow(V);

        // ポップアップ用のViewをpopupWindowにセットする
        popupWindow.setContentView(popupView);

        // サイズ(幅)を設定
        popupWindow.setWidth(view.x*8/10);

        // サイズ(高さ)を設定
        popupWindow.setHeight(view.y*7/10);

        Sound = popupView.findViewById(R.id.Sound);

        // ★リスナー登録
        G1 = popupView.findViewById(R.id.Group1);
        G2 = popupView.findViewById(R.id.Group2);
        G3 = popupView.findViewById(R.id.Group3);

        G1.setOnCheckedChangeListener(listener);
        G2.setOnCheckedChangeListener(listener);
        G3.setOnCheckedChangeListener(listener);

        idlist = new int[]{R.id.A, R.id.B, R.id.C, R.id.D, R.id.E, R.id.F, R.id.G};
        Artlist = new int[]{R.id.nothing, R.id.Sharp, R.id.Flat};

        Button ms_b = popupView.findViewById(R.id.MakeSound);
        ms_b.setOnClickListener(this);
    }

    RadioGroup.OnCheckedChangeListener listener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            final int terget = group.getId();
            switch (terget){
                case R.id.Group1:
                    if(G2.getCheckedRadioButtonId() != -1){
                        G2.setOnCheckedChangeListener(null);
                        G2.clearCheck();
                        G2.setOnCheckedChangeListener(this);
                    }
                    break;

                case R.id.Group2:
                    if(G1.getCheckedRadioButtonId() != -1){
                        G1.setOnCheckedChangeListener(null);
                        G1.clearCheck();
                        G1.setOnCheckedChangeListener(this);
                    }
                    break;
            }

            setSound(checkedId);

        }
    };

    public void setSound(int checkedId){
        for(int i = 0; i < idlist.length; i++){
            if(idlist[i] == checkedId){
                Ontei = i;
                Sound.setText(Arr[Ontei]+art_Arr[Articulation]);
            }

        }
        for(int i = 0; i < Artlist.length; i++) {
            if (Artlist[i] == checkedId) {
                Articulation = i;
                Sound.setText(Arr[Ontei]+art_Arr[Articulation]);
            }
        }

    }
}