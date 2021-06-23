package com.y3033108;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        setContentView(R.layout.activity_main);
        TextView FREQ = findViewById(R.id.freq);
        TextView NODE = findViewById(R.id.node);
        TextView CENT = findViewById(R.id.cent);

        Button b = findViewById(R.id.button);
        b.setOnClickListener(this);

        AcousticAnalysis aa = new AcousticAnalysis(FREQ,NODE,CENT);
        aa.start();
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
    public void onClick(View v) {
        MakeSound ms = new MakeSound();
        ms.playSound();
    }
}