package com.example.pasb;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner blescanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BLEService mBLEService;

    private static final long SCAN_PERIOD = 20000;

    private static final UUID UUID_ADC_SERVICE = UUID.fromString(Constants.ADC_SERVICE);

    private static final int PERMISSION_REQUEST = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final String BLE_TURNINGOFF_TEXT = "bluetooth adaptor turning off";
    private final String BLE_ON_TEXT = "bluetooth adaptor on";

    private ProgressDialog scanningDialog, connectingDialog;

    // managing service lifecycle here
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBLEService = ((BLEService.LocalBinder) service).getService();
            if (!mBLEService.initialize()) {
//                Log.e(LOG_TAG, "Unable to initialize Bluetooth!");
                finish();
            }
//            Log.d(LOG_TAG, "BLE Service connected!");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBLEService = null;
//            Log.d(LOG_TAG, "BLE Service disconnected!");
        }
    };

    // broadcast receiver for BLE service intents
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            TextView blestateText = (TextView)findViewById(R.id.blestateText);

            switch (action) {

                case BLEService.ACTION_GATT_CONNECTED : {
                    connectingDialog.dismiss();
                    scanLeDevice(false);
                    blestateText.setText("bluetooth connected");
                    break;
                }


                case BLEService.ACTION_GATT_DISCONNECTED : {
                    if (connectingDialog.isShowing()) {
                        connectingDialog.dismiss();
                    }
                    scanLeDevice(false);
                    blestateText.setText("bluetooth disconnected");
                    break;
                }

                case BLEService.ACTION_GATT_SERVICES_DISCOVERED : {
                    List<BluetoothGattService> services = mBLEService.getSupportedGattServices();
                    for (BluetoothGattService service : services) {
//                        Log.d(LOG_TAG, "Discovered service : " + service.getUuid());
                    }
                    break;
                }

            }
        }
    };

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            TextView blestateText = (TextView)findViewById(R.id.blestateText);

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF) {
                    blestateText.setText(BLE_TURNINGOFF_TEXT);
                } else if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                    blestateText.setText(BLE_ON_TEXT);
                }
            }

        }
    };

    private BroadcastReceiver displayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView Xtext = (TextView)findViewById(R.id.delX);
            TextView Ytext = (TextView)findViewById(R.id.delY);
            TextView Ztext = (TextView)findViewById(R.id.delZ);
            Xtext.setText(Float.toString(Calc.delX));
            Ytext.setText(Float.toString(Calc.delY));
            Ztext.setText(Float.toString(Calc.delZ));

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!Utils.hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST);
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        this.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(UUID_ADC_SERVICE)).build();
        filters.add(filter);

        // progress dialog for while scanning for devices
        scanningDialog = new ProgressDialog(this);
        scanningDialog.setMessage("Scanning...");
        scanningDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        scanningDialog.setCancelable(false);

        //progress dialog for while connecting to device
        connectingDialog = new ProgressDialog(this);
        connectingDialog.setMessage("Connecting...");
        connectingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        connectingDialog.setCancelable(false);


        Button connectbutton = (Button)findViewById(R.id.connectbutton);
        connectbutton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                scanningDialog.show();
                scanLeDevice(true);
            }
        });

        Button functionbutton = (Button)findViewById(R.id.functionbutton);
        functionbutton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SensorService.class);
                startService(intent);
            }
        });


        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, PERMISSION_REQUEST);
        } else {
            blescanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(bluetoothReceiver);

        if (mBLEService.getConnectionState() == BLEService.STATE_CONNECTED)
            disconnectDevice();
        unbindService(mServiceConnection);
        mBLEService = null;
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST && permissions.length > 0 && grantResults.length > 0) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                        grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    // close app if coarse location permission is denied
                    finish();
                }
            }
        }
    }

    // intent filter for BLE service broadcast intents
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
//        intentFilter.addAction(BLEService.ACC_DATA);
//        intentFilter.addAction(BLEService.GYRO_DATA);
//        intentFilter.addAction(BLEService.MAGNETIC_DATA);
        return intentFilter;
    }

    // BLE scanning
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stop scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    blescanner.stopScan(mScanCallback);
                    if(scanningDialog.isShowing())
                        scanningDialog.dismiss();
                }
            }, SCAN_PERIOD);
            blescanner.startScan(filters, settings, mScanCallback);
            // initialise found count when scanning starts
            found = 0;
        } else {
            blescanner.stopScan(mScanCallback);
        }
    }

    // callback for BLE scan results
    private static int found;
    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
//            Log.d(LOG_TAG, "Callback Type : " + String.valueOf(callbackType));
//            Log.d(LOG_TAG, "Result : " + result.toString());
            final BluetoothDevice btDevice = result.getDevice();
//            Log.d(LOG_TAG, "Identified device : " + btDevice.getAddress());
            String deviceName = btDevice.getName();
            // check device again by name
            if(deviceName != null && deviceName.equals(Constants.DEVICE_NAME)) {
                found++;
                if(scanningDialog.isShowing())
                    scanningDialog.dismiss();
                // show connect dialog only when found for the first time
                if(found == 1) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertDialogBuilder.setMessage("Device found. Do you want to connect?");
                    alertDialogBuilder.setCancelable(false);
                    alertDialogBuilder.setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    connectToDevice(btDevice);
                                }
                            });
                    alertDialogBuilder.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    // stop scanning if user does not want to connect to device
                                    scanLeDevice(false);
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results : ", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
//            Log.e(LOG_TAG, "Scan Failed! Error Code : " + errorCode);
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        mBLEService.connect(device.getAddress());
    }

    private void disconnectDevice() {
        mBLEService.close();
    }

}