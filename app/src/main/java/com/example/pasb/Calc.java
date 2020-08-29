package com.example.pasb;

import android.util.Log;

public class Calc {
    private static final float GRAVITATION = 9.80665f;

    public static float ax, ay, az;
    public static float arx, ary, arz;
    public static float gx, gy, gz;
    public static float mx=0, my=0, mz=0;

    public static float c11,c12,c13,c21,c22,c23,c31,c32,c33;
    public static float delX, delY, delZ;
    public static float vx, vy, vz;

    public static float q0,q1,q2,q3;
    private static float beta = 0.05f;
    public static float interval=0.02f;
    public static float sampleFreq=50.0f;

    public static float acc,accmax,accmin;

    public static float accEast, accNorth;




    public static void quaternionToMatrix(){
        c11 = 2.0f*q0*q0-1.0f+2.0f*q1*q1;
        c12 = 2.0f*q1*q2-2.0f*q0*q3;
        c13 = 2.0f*q1*q3+2.0f*q0*q2;
        c21 = 2.0f*q1*q2+2.0f*q0*q3;
        c22 = 2.0f*q0*q0-1.0f+2.0f*q2*q2;
        c23 = 2.0f*q2*q3-2.0f*q0*q1;
        c31 = 2.0f*q1*q3-2.0f*q0*q1;
        c32 = 2.0f*q2*q3+2.0f*q0*q1;
        c33 = 2.0f*q0*q0-1.0f+2.0f*q3*q3;
    }

    public static void vupdate(){
        vx += interval*(c11*ax+c12*ay+c13*az);
        vy += interval*(c21*ax+c22*ay+c23*az);
        vz += interval*(c31*ax+c32*ay+c33*az);
    }

    public static void aConvert(){
        arx = c11*ax+c12*ay+c13*az;
        ary = c21*ax+c22*ay+c23*az;
        arz = c31*ax+c32*ay+c33*az;
    }

    public static void pupdate(){
        delX += interval*vx;
        delY += interval*vy;
        delZ += interval*vz;
    }

   }