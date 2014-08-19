/*
 * **********************************ASCOPERATOR.JAVA*************************************
 * This is the main class.
 * This class is responsible for managing all other classes.
 */

package com.android.ASC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.example.android.Module1.R;

import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("InlinedApi")
public class ASCOperator extends Activity {
    private static final String TAG = "Module1";
    public StringBuffer outStringBuffer;
    public static final int DATA_STATE_CHANGE = 1;
    public static final int DATA_READ = 2;
    public static final int DATA_WRITE = 3;
    public static final int DATA_DEVICE_NAME = 4;
    public static final int DATA_TOAST = 5;
    public static int MODE = 0, INDEX_ID = 55555;
    public static String EXTRA_SENSOR_ADDRESS = "device_address";  
    public static final String SENSOR_NAME = "sensor_name";
    public static final String TOAST = "toast"; 
    private static final int REQUEST_CONNECT_SENSOR = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private String connectedSensorName = null;    
    private BluetoothAdapter btAdapter = null;  
    private BluetoothConnectionManager btconnManager = null;
    public Thread asc;
    public static ArrayAdapter<String> name;
    public static ArrayList<String> sensorList, nameList;
    public static ArrayList<Integer> valueList;
    
    /////////////DATABASE ATTRIBUTES////////////////
    private static final String DATABASE_NAME = "SensorDatabase";
	private static final String TABLE_SENSOR_DATA = "Sensor Data";
	private static final String KEY_ID = "_id";
	private static final String KEY_NAME = "Name";
	private static final String KEY_MAC = "Mac Address";
	private static final String KEY_ENTRIES = "Entries";
	public File file = null;
	public FileOutputStream writer = null;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);   
        Notifier notifier = new Notifier();
        
        String selectQuery = "SELECT  * FROM " + TABLE_SENSOR_DATA;
		IDatabase iDataBase = new IDatabase(getApplicationContext());
		SQLiteDatabase db = iDataBase.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if (cursor.moveToFirst()) {
			do {
				String name = cursor.getString(1);
				String mac = cursor.getString(2);
				String entries = cursor.getString(3);
				file = getFileStreamPath("sendata.txt");
				try {
					writeSensorDatatoFile(file, entries);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (cursor.moveToNext());
		}
		db.close();
        
        sensorList = new ArrayList<String>();
        name = new ArrayAdapter<String>(ASCOperator.this, android.R.layout.simple_spinner_item, sensorList);
        valueList = new ArrayList<Integer>();
        nameList = new ArrayList<String>();
        btAdapter = BluetoothAdapter.getDefaultAdapter();          
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);       
        } else {
            if (btconnManager == null){
            	btconnManager = new BluetoothConnectionManager(this, mHandler);
            };           
        }
        FileInputStream fis;
		try {
			fis = openFileInput("sendata.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
	        String temp = "", data = "";
	        String[] datatemp;
	        while((temp = reader.readLine()) != null){
	        	data = data + "\t" + temp;
				datatemp = data.split("\t");
				DatabaseParser.dataParse(ASCOperator.this, datatemp);
	        }
	        temp = "";
			data = "";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }

    private void writeSensorDatatoFile(File file, String arg0) throws IOException{
    	Notifier notifier = new Notifier();
    	String nline = "\n";
    	if (!file.exists()) {
     	   try {
				file.createNewFile();
			} catch (Exception e) {
				notifier.notifyUser(getApplicationContext(),
						"Sensor data file open failed!", Notifier.LONG_TERM);
			}
     }
     try {
			writer = openFileOutput(file.getName(), Context.MODE_APPEND);
		} catch (FileNotFoundException e) {
			notifier.notifyUser(getApplicationContext(),
					"Sensor data file open failed!", Notifier.LONG_TERM);
		}
     writer.write(arg0.getBytes());
     writer.write(nline.getBytes());
     writer.close();
    }
    
    private void ensureDiscoverable() {        
        if (btAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
   
    @SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message Dat) {
            switch (Dat.what) {         
            case DATA_READ:
            	if(MODE == 0){
            		LinearLayout lo1 = (LinearLayout) findViewById(R.id.linearLayout1);
            		LinearLayout lo2 = (LinearLayout) findViewById(R.id.linearLayout2);
					for(int i=0; i<nameList.size(); i++){
						TextView txt1 = new TextView(ASCOperator.this);
						txt1.setText(nameList.get(i));
						lo1.addView(txt1, i);
						txt1.setVisibility(View.VISIBLE);
						TextView txt2 = new TextView(ASCOperator.this);
						lo2.addView(txt2, i);
						txt2.setVisibility(View.VISIBLE);
					}
            		MODE = 1;
            	}
            	else if(MODE == 1){
	        		byte[] readBuf = (byte[]) Dat.obj;
            		LinearLayout lo2 = (LinearLayout) findViewById(R.id.linearLayout2);            		
            		int j=0;
            		for(int i=0; i<lo2.getChildCount(); i++){
	        			String tempData = convertByteToString(Arrays.copyOfRange(readBuf, valueList.get(j), valueList.get(j+1)));
	        			((TextView) lo2.getChildAt(i)).setText(tempData);
	        			j=j+2;	        			
	        		}	        		
            	}
				break;           
            }
        }
    };
    
    public String convertByteToString(byte[] b){          
        int value= 0;
        for(int i=0;i<b.length;i++){                
        int n=(b[i]<0?(int)b[i]+256:(int)b[i])<<(8*i);             
            value+=n;
        }         
        return String.valueOf(value);       
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_SENSOR:
          
            if (resultCode == Activity.RESULT_OK) {                
                String address = data.getExtras().getString(SensorManipulator.SPARE_SENSOR_ADDRESS);                
                BluetoothDevice device = btAdapter.getRemoteDevice(address);                
                btconnManager.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:            
            if (resultCode == Activity.RESULT_OK) {             
            	btconnManager = new BluetoothConnectionManager(this, mHandler);
            } else {                               
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private  void addSensor(){
    	final Notifier notifier = new Notifier();
    	final Dialog d = new Dialog(ASCOperator.this);
        d.setTitle("Add Sensor");
        d.setCancelable(true);
        d.setContentView(R.layout.addsensor);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
        d.show();
        d.getWindow().setAttributes(lp);
        ((Button)d.findViewById(R.id.button1))
        .setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				int index =((LinearLayout)arg0.getParent()).getChildCount()-2;
				TextView v1 = new TextView(getApplicationContext());
				v1.setLayoutParams(new LinearLayout.LayoutParams
						(LinearLayout.LayoutParams.WRAP_CONTENT, 
								LinearLayout.LayoutParams.WRAP_CONTENT));
				v1.setText("Field name: ");
				EditText t1 = new EditText(getApplicationContext());
				t1.setLayoutParams(new LinearLayout.LayoutParams
						(LinearLayout.LayoutParams.MATCH_PARENT, 
								LinearLayout.LayoutParams.WRAP_CONTENT));
				
				TextView v2= new TextView(getApplicationContext());
				v2.setLayoutParams(new LinearLayout.LayoutParams
						(LinearLayout.LayoutParams.WRAP_CONTENT, 
								LinearLayout.LayoutParams.WRAP_CONTENT));
				v2.setText("Field index: ");
				EditText t2 = new EditText(getApplicationContext());
				t2.setLayoutParams(new LinearLayout.LayoutParams
						(LinearLayout.LayoutParams.MATCH_PARENT, 
								LinearLayout.LayoutParams.WRAP_CONTENT));
				
				TextView v3 = new TextView(getApplicationContext());
				v3.setLayoutParams(new LinearLayout.LayoutParams
						(LinearLayout.LayoutParams.WRAP_CONTENT, 
								LinearLayout.LayoutParams.WRAP_CONTENT));
				v3.setText("Field length: ");
				EditText t3 = new EditText(getApplicationContext());
				t3.setLayoutParams(new LinearLayout.LayoutParams
						(LinearLayout.LayoutParams.MATCH_PARENT, 
								LinearLayout.LayoutParams.WRAP_CONTENT));
				
				((LinearLayout)arg0.getParent()).addView(v1, index);
				((LinearLayout)arg0.getParent()).addView(t1, index+1);
				((LinearLayout)arg0.getParent()).addView(v2, index+2);
				((LinearLayout)arg0.getParent()).addView(t2, index+3);
				((LinearLayout)arg0.getParent()).addView(v3, index+4);
				((LinearLayout)arg0.getParent()).addView(t3, index+5);
				index=index+6;
			}
		});
        
        ((Button)d.findViewById(R.id.button1))
        .setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				int fieldCount = (((LinearLayout)v.getParent())
						.getChildCount()-6)/2;
				StringBuilder entries = new StringBuilder();
				for(int i=0, j=5; i<fieldCount; i++, j=j+6){
					entries.append(((EditText)((LinearLayout)v.getParent())
							.getChildAt(j)).getText().toString()).append("\t")
							.append(((EditText)((LinearLayout)v.getParent())
							.getChildAt(j+2)).getText().toString()).append("\t")
							.append(((EditText)((LinearLayout)v.getParent())
							.getChildAt(j+4)).getText().toString()).append("\t");
				}
				IDatabase iDataBase = new IDatabase(getApplicationContext());
				SQLiteDatabase db = iDataBase.getWritableDatabase();
				iDataBase.getWritableDatabase();			
				ContentValues cv = new ContentValues();
				cv.put(KEY_NAME, ((EditText) d.findViewById(R.id.editText1))
						.getText().toString());
				cv.put(KEY_MAC, ((EditText) d.findViewById(R.id.editText2))
						.getText().toString());
				cv.put(KEY_ENTRIES, entries.toString());
				if(db.insert(TABLE_SENSOR_DATA, null, cv) < 0){
					notifier.notifyUser(ASCOperator.this,
							"Sensor data saving failed!", notifier.SHORT_TERM);
				}else{
					notifier.notifyUser(ASCOperator.this,
							"Sensor data saving successfull!", notifier.SHORT_TERM);
				}
				db.close();
			}
			});
    	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:          
            Intent serverIntent = new Intent(this, SensorManipulator.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_SENSOR);
            return true;
        case R.id.discoverable:          
            ensureDiscoverable();
            return true;     
        case R.id.addsensormenu:
        	addSensor();
        }
        return false;
    }

}