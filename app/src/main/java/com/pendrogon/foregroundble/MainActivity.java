package com.pendrogon.foregroundble;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.pendrogon.foregroundblesdk.ForegroundBleMain;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button start_bt, stop_bt, manual;
    private ForegroundBleMain bleForeground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayList<String> antenas = new ArrayList<String>();
        antenas.add("202112055");
        bleForeground = new ForegroundBleMain(this, "202112055", antenas);//, MainActivity.class);
        bleForeground.requestLocationPermission();

        start_bt = findViewById(R.id.btnStartService);
        stop_bt = findViewById(R.id.btnStopService);
        manual = findViewById(R.id.manual);

        start_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleForeground.StartForeground();
            }
        });
        stop_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleForeground.StopForeground();
            }
        });
        manual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleForeground.startBleScanManual();
            }
        });
    }

    @Override public void onResume() {
        super.onResume();
        bleForeground.promptEnableBluetooth(MainActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        bleForeground.onRequestPermissionsBluetooth(requestCode, resultCode, data);
    }
}