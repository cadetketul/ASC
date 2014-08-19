/*
 *****************************DATAPARSER.JAVA***********************************************
 *This class is responsible for fetching data from the file located in res/raw.
 *Fetched data is stored in sensorList(ArrayList) located in ASC class.
 *While fetching data it is simultaneously displayed in the log window.
 *Once data is fetched completely, log screen will disappear.
 */


package com.android.ASC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.example.android.Module1.R;

import java.io.InputStreamReader;
import java.util.Arrays;

import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ListView;

public class DatabaseParser{
	
	public static BufferedReader reader;
	static FileInputStream inputStream = null;
	static InputStreamReader inputreader = null;
	
	public static boolean dataParse(Context c, String[] datatemp){
	
		//********************Show log screen for displaying Data parsing************************
		final Dialog dialog = new Dialog(c);
    	dialog.setTitle("Parsing Data... "+"\n"+" Please wait...");
    	dialog.setCancelable(true);
    	dialog.setContentView(R.layout.dataparser);
    	WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
    	lp.copyFrom(dialog.getWindow().getAttributes());
    	dialog.show();
    	dialog.getWindow().setAttributes(lp);
    	ListView view = (ListView) dialog.findViewById(R.id.listView2);
    	view.setAdapter(ASCOperator.name);
    	//***************************************************************************************
		
    	//********************Fetching data from Resource file***********************************
			ASCOperator.sensorList.clear(); 	
			String dev_name = datatemp[0];
			String addressMAC = datatemp[1];
			ASCOperator.sensorList.add(dev_name);
			ASCOperator.name.notifyDataSetChanged();
			ASCOperator.sensorList.add(addressMAC);
			ASCOperator.name.notifyDataSetChanged();
			for(int i=2; i<datatemp.length; i++){
				ASCOperator.sensorList.add(datatemp[i]);
				ASCOperator.name.notifyDataSetChanged();
			}
			Arrays.fill( datatemp, null );
			System.out.println("Data parsing successfull...");		
    	//***************************************************************************************
		
    	//**************************Disappear the Log screen*************************************
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			
			public void run() {
				 dialog.dismiss();
			}
		}, 500);
		return true;
    	//***************************************************************************************
	}
}
