package com.example.ble_search_device;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private TextView textView;
    private ListView listView;
    private Button startScanBtn;
    private Button stopScanBtn;
    private ListAdapter listAdapter;
    private ArrayList<String> deviceName;
    private boolean mScanning = false;

    private BluetoothManager bluetoothManager = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SCAN_TIME = 10000;
    private ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();
    private Handler handler;

    private BluetoothLeScanner bluetoothLeScanner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("MainActivity", "onCreate_Start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
        listView = (ListView) findViewById(R.id.listview);
        startScanBtn = (Button) findViewById(R.id.startScanBtn);

        startScanBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanFunction(true);
            }
        });

        stopScanBtn = (Button) findViewById(R.id.stopScanBtn);

        stopScanBtn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanFunction(false);
            }
        });

        if(!getPackageManager().hasSystemFeature(getPackageManager().FEATURE_BLUETOOTH_LE)){
            Toast.makeText(getBaseContext(), "No Support BLE", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if(bluetoothAdapter == null){
            Toast.makeText(getBaseContext(), "Bluetooth Not Support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        else if(bluetoothAdapter.isEnabled() == false){
            Toast.makeText(getBaseContext(), "Please Open Bluetooth", Toast.LENGTH_SHORT).show();
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }

        bluetoothManager = (BluetoothManager) this.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    }
                });
                builder.show();
            }
        }

        deviceName = new ArrayList<String>();
        listAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_expandable_list_item_1, deviceName);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new onItemClickLister());
        handler = new Handler();
        Log.v("MainActivity", "onCreate_End");
    }

    protected void onResume(){
        Log.v("MainActivity", "onResume_Start");
        super.onResume();
        if(!bluetoothAdapter.isEnabled()){
            Toast.makeText(getBaseContext(), "Please Open Bluetooth", Toast.LENGTH_SHORT).show();
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
            Log.v("MainActivity","Enable_start");
        }
        //ScanFunction(true);

        Log.v("MainActivity", "onResume_End");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.v("MainActivity", "onActivityResult_Start");
        if(REQUEST_ENABLE_BT == 1 && resultCode == Activity.RESULT_CANCELED){
            Log.e("MainActivity", "OnActivityResult_Canceled");
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
        Log.v("MainActivity", "onActivityResult_End");
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.v("MainActivity", "onScanResult_Start");
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();

            if (!bluetoothDevices.contains(device)){
                bluetoothDevices.add(device);

                deviceName.add((device.getName() == null ? "(null)" : device.getName()) + " rssi: " + result.getRssi() + "\r\n" + device.getAddress());
                ((BaseAdapter)listAdapter).notifyDataSetChanged();
            }
            Log.v("MainActivity", "onScanResult_End");
        }
    };



    private void ScanFunction(boolean enable){
        Log.v("MainActivity", "ScanFunction_Start");
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if(enable){
            Log.v("MainActivity", "mScanning");
            mScanning = true;
            bluetoothLeScanner.startScan(scanCallback);
            Log.v("MainActivity", "mScanninginginging");
            textView.setText("Scanning");

            final ScanCallback finalScanCallback = scanCallback;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                    mScanning = false;
                    bluetoothLeScanner.stopScan(finalScanCallback);
                    textView.setText("Stop Scan");
                }
            }, SCAN_TIME);
        }
        else{
            mScanning = false;
            textView.setText("Stop Scan");
            bluetoothLeScanner.stopScan(scanCallback);
        }
        Log.v("MainActivity", "ScanFunction_End");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.v("MainActivity", "OnPause(): Stop Scan");
        //bluetoothLeScanner.stopScan(scanCallback);
    }

    private class onItemClickLister implements AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id){
            final BluetoothDevice mBluetoothDevice = bluetoothDevices.get(position);
        }
    }


}
