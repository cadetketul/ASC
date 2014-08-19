/*
 * ************************************SENSORMANIPULATOR.JAVA**************************
 * This class is responsible to discover available sensors in the range of device
 * Upon finding any sensor, it compares the sensor address with the address present in the database.
 * When proper match found, it gives particular data frame to the ASC class.
 */

package com.android.ASC;

import java.util.Set;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.example.android.Module1.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SensorManipulator extends Activity {
    
    private static final String TAG = "SensorManipulator";
    public static String SPARE_SENSOR_ADDRESS = "device_address";
    private BluetoothAdapter btAdapter;
    private ArrayAdapter<String> pairedSensorAdapter;
    private ArrayAdapter<String> newSensorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);
        setResult(Activity.RESULT_CANCELED);
        
//*********************************SEARCH AND ENUMERATE SENSORS*******************************
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                discover();
                v.setVisibility(View.GONE);
            }
        });
       
        pairedSensorAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        newSensorAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedSensorAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(newSensorAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedSensorAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedSensorAdapter.add(noDevices);
        }
//**********************************************************************************************
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btAdapter != null) {
            btAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }

    private void discover() {
        Log.d(TAG, "doDiscovery()");
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();
    }
   
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            btAdapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            String name = info.substring(0, info.length() - 18);
            for(int i=0; i<ASCOperator.sensorList.size(); i++){
            	if(ASCOperator.sensorList.get(i).equals(address)){
            		collectSensorData(i+1);
            		break;
            	}
            }
            Intent intent = new Intent();
            intent.putExtra(SPARE_SENSOR_ADDRESS, address);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
    
    public void collectSensorData(int INDEX){
    	int TEMP_INDEX = INDEX;
    	for(int i=0; i<Integer.valueOf(ASCOperator.sensorList.get(INDEX)); i++){
    		ASCOperator.nameList.add(ASCOperator.sensorList.get(TEMP_INDEX +1));
    		ASCOperator.valueList.add(Integer.valueOf(ASCOperator.sensorList.get(TEMP_INDEX+2)));
    		ASCOperator.valueList.add(Integer.valueOf(ASCOperator.sensorList.get(TEMP_INDEX+2))+Integer.valueOf(ASCOperator.sensorList.get(TEMP_INDEX+3)));
    		TEMP_INDEX = TEMP_INDEX + 3;
    	}
    }
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);             
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    newSensorAdapter.add(device.getName() + "\n" + device.getAddress());
                }            
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (newSensorAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    newSensorAdapter.add(noDevices);
                }
            }
        }
    };

}
