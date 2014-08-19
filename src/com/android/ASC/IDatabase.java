package com.android.ASC;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class IDatabase extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "SensorDatabase";
	private static final String TABLE_SENSOR_DATA = "Sensor Data";
	private static final String KEY_ID = "_id";
	private static final String KEY_NAME = "Name";
	private static final String KEY_MAC = "Mac Address";
	private static final String KEY_ENTRIES = "Entries";
	
	public static ContentValues eventReg;

	public IDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_SENSOR_DATA_TABLE = "CREATE TABLE " + TABLE_SENSOR_DATA + "("
				+ KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
				+ KEY_MAC + " TEXT," + KEY_ENTRIES + " TEXT," + ")";
		db.execSQL(CREATE_SENSOR_DATA_TABLE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSOR_DATA);
		onCreate(db);
	}

}
