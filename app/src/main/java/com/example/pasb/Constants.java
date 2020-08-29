package com.example.pasb;

import android.util.Log;

public class Constants {
    // Enter your custom service UUID below between the quotes
    public static final String ADC_SERVICE = "9a48ecba-2e92-082f-c079-9e75aae428b1";


    // Enter your data characteristic UUID below between the quotes
    public static final String ACC_CHARACTERISTIC = "2db29ee2-d964-43fe-b33f-fbfe83941613";
    public static final String GYRO_CHARACTERISTIC = "61992289-84da-4633-82c8-1a33c6d3e6fc";
    public static final String MAGNETIC_CHARACTERISTIC = "4bdf6e82-b600-4518-9b82-54f1e3d849da";
    public static final String ORIENTATION_CHARACTERISTIC = "c8fafd7f-38df-40b3-92fa-a0b30d795a85";
    public static final String STEP_CHARACTERISTIC = "d732cd53-1a10-441b-811e-c627708f868b";

    // This is a standard descriptor UUID, change it if required
    public static final String CLIENT_CHARACTERISTIC_CONFIGURATION_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    // Enter your custom device name below between the quotes
    public static final String DEVICE_NAME = "Arduino Nano 33 BLE";

}
