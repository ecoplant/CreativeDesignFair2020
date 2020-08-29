package com.example.pasb;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {
    public static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static float ByteToFloat(byte[] value, int start){
//        int accum = 0;
//        int i=0;
//        byte[] temp = new byte[4];
//        System.arraycopy(value, start, temp, 0,4);
//
//        for(int shiftBy = start; shiftBy < start+32; shiftBy += 8){
//            accum |= ( (long)(temp[i]&0xff) ) << shiftBy;
//            i++;
//        }
//        return Float.intBitsToFloat(accum);
        byte temp[] = new byte[4];
        System.arraycopy(value, start, temp, 0, 4);
        return ByteBuffer.wrap(temp).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}