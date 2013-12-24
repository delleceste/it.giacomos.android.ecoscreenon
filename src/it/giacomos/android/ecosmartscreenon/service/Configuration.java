package it.giacomos.android.ecosmartscreenon.service;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.SparseArray;

public class Configuration 
{
	public static int NOTIFICATION_MODE_ALWAYS_ON = 0;
	public static int NOTIFICATION_MODE_ALWAYS_OFF = 1;
	public static int NOTIFICATION_MODE_ON_DETECT = 2;
	
	public int mNotificationMode;
	public SensitivitiesArray mMotionSensitivities;
	public int mMotionSensitivityLevel;
	public int mXInclinationThresh, mYInclinationThresh;
	public int mScreenTimeo, mMigrateToActiveTimeo;
	public boolean mMotionDetectionEnabled;
	
	public Configuration(Context ctx)
	{
		SharedPreferences mSharedPrefs = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
		mNotificationMode = mSharedPrefs.getInt("NOTIFICATION_MODE", NOTIFICATION_MODE_ALWAYS_ON);
		mMotionSensitivityLevel = mSharedPrefs.getInt("MOTION_SENSITIVITY_LEVEL", 0);
		mXInclinationThresh = mSharedPrefs.getInt("X_INCLINATION_THRESH", 1);
		mYInclinationThresh = mSharedPrefs.getInt("Y_INCLINATION_THRESH", 1);
		
		mMotionDetectionEnabled = mSharedPrefs.getBoolean(NotificationViewIntentListener.TOGGLE_MOTION_DETECTION_ENABLE, true);
		
		try {
			mScreenTimeo = Settings.System.getInt(ctx.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
		} 
		catch (SettingNotFoundException e) 
		{
			mScreenTimeo = 60000;
			e.printStackTrace();
		}
		
		mMotionSensitivities = new SensitivitiesArray();
	}

	public MotionSensitivityValues getMotionSensitivityValues()
	{
		MotionSensitivityValues msv = mMotionSensitivities.get(mMotionSensitivityLevel);
		if(msv == null)
			return mMotionSensitivities.get(0);
		return msv;
	}
	
	public void store(Context ctx)
	{
		SharedPreferences mSharedPrefs = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
		SharedPreferences.Editor e = mSharedPrefs.edit();
		e.putInt("NOTIFICATION_MODE", mNotificationMode);
		e.putInt("X_INCLINATION_THRESH", mXInclinationThresh);
		e.putInt("Y_INCLINATION_THRESH", mYInclinationThresh);
		e.putInt("MOTION_SENSITIVITY_LEVEL", mMotionSensitivityLevel);
		e.commit();
	}

	public void update(Intent intent) {

		/* mScreenTimeo, mMigrateToActiveTimeo */
		if(intent.hasExtra("migrateToActiveTimeo"))
			mMigrateToActiveTimeo = intent.getIntExtra("migrateToActiveTimeo", 10000);
		if(intent.hasExtra("notificationMode"))
			mNotificationMode = intent.getIntExtra("notificationMode", NOTIFICATION_MODE_ALWAYS_ON);
		if(intent.hasExtra("motionSensitivityLevel"))
			mMotionSensitivityLevel = intent.getIntExtra("motionSensitivityLevel", 0);
		if(intent.hasExtra("yInclinationThresh"))
			mYInclinationThresh = intent.getIntExtra("yInclinationThresh", 0);
		
	}
}
