package com.example.pasb;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver displayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView Xtext = (TextView)findViewById(R.id.delX);
            TextView Ytext = (TextView)findViewById(R.id.delY);
            TextView Ztext = (TextView)findViewById(R.id.delZ);
            Xtext.setText(Float.toString(Calc.delX));
            Ytext.setText(Float.toString(Calc.delY));
            Ztext.setText(Integer.toString(SensorService.stepcount));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(getApplicationContext(), SensorService.class);
        startService(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(displayReceiver, new IntentFilter(SensorService.TO_MAIN));
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(displayReceiver);
    }
}