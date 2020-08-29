package com.example.pasb;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.IBinder;
import android.util.Log;

public class SensorService extends Service {
    public SensorService() {
    }

    private SensorManager mSensorManager;
    private Sensor lnaccl;
    private Sensor step;
    private Sensor orientation;
    private SensorEventListener mLnAcclListner;
    private SensorEventListener mStepListener;
    private SensorEventListener mOrntListener;

    public final static String TO_MAIN =
            "com.example.pasb.TO_MAIN";

    private long lastStepMillis;
    public final float K = 0.48f;
    public float SL;

    public static float[] axRaws = new float[5];
    public static float[] ayRaws = new float[5];
    public static float[] azRaws = new float[5];

    private int front,rear;

    public static int stepcount=0;

    private static int stepdetect=0;


    private class AccelerometerListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            Calc.ax = (5.0f*Calc.ax - axRaws[front] + sensorEvent.values[0])/5.0f;
            Calc.ay = (5.0f*Calc.ay - ayRaws[front] + sensorEvent.values[1])/5.0f;
            Calc.az = (5.0f*Calc.az - azRaws[front] + sensorEvent.values[2])/5.0f;

            rear = (rear+1)%5;

            axRaws[rear] = sensorEvent.values[0];
            ayRaws[rear] = sensorEvent.values[1];
            azRaws[rear] = sensorEvent.values[2];

            front = (front+1)%5;

            Calc.vupdate();
            Calc.pupdate();

//            if(Calc.acc> Calc.accmax)
//                Calc.accmax = Calc.acc;
//            if(Calc.acc<Calc.accmin)
//                Calc.accmin = Calc.acc;

//            Calc.aConvert();
//            Calc.accEast +=Calc.arx;
//            Calc.accNorth +=Calc.ary;
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class OrientationListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            Log.d("SensorService", "orientation changed");
            Calc.q0 = sensorEvent.values[3];
            Calc.q1 = sensorEvent.values[0];
            Calc.q2 = sensorEvent.values[1];
            Calc.q3 = sensorEvent.values[2];

            Calc.quaternionToMatrix();

            Intent intent = new Intent(TO_MAIN);
            sendBroadcast(intent);
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private class StepListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if(sensorEvent.values[0]==1&&(System.currentTimeMillis()-lastStepMillis)>200){

                stepcount++;

                lastStepMillis = System.currentTimeMillis();

                SL = (float) Math.pow((Calc.accmax-Calc.accmin),0.25f);
                float magnitude = (float) Math.sqrt(Calc.accEast*Calc.accEast+Calc.accNorth*Calc.accNorth);
                Calc.delX += SL*Calc.accEast/magnitude;
                Calc.delY += SL*Calc.accNorth/magnitude;

                Calc.accEast = Calc.accNorth = 0.0f;
                Calc.accmax = -100.0f;
                Calc.accmin = 100.0f;
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private BroadcastReceiver stepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float mag = (float) Math.sqrt(Calc.c13*Calc.c13+Calc.c23*Calc.c23);
            Calc.delX += Calc.SL * Calc.c13 / mag;
            Calc.delY += Calc.SL * Calc.c23 / mag;

        }
    };


    @Override
    public void onCreate() {

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

//        lnaccl = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//        mLnAcclListner = new AccelerometerListener();
//        mSensorManager.registerListener(mLnAcclListner, lnaccl, SensorManager.SENSOR_DELAY_GAME);

        orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mOrntListener = new OrientationListener();
        mSensorManager.registerListener(mOrntListener, orientation, SensorManager.SENSOR_DELAY_GAME);

//        step = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
//        mStepListener = new StepListener();
//        mSensorManager.registerListener(mStepListener, step, SensorManager.SENSOR_DELAY_FASTEST);

        Calc.delX = Calc.delY = Calc.delZ = 0;
        Calc.vx = Calc.vy = Calc.vz = 0;



        Log.d("SensorService", "Service create");
    }

    @Override
    public void onDestroy() {
//        mSensorManager.unregisterListener(mLnAcclListner);
        mSensorManager.unregisterListener(mOrntListener);
//        mSensorManager.unregisterListener(mStepListener);
        Log.d("SensorService", "Service destroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
