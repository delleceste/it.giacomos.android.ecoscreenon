package it.giacomos.android.ecosmartscreenon.service;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.util.SparseArray;

public class Configuration 
{
	public static int NOTIFICATION_MODE_ALWAYS_ON = 0;
	public static int NOTIFICATION_MODE_ALWAYS_OFF = 1;
	public static int NOTIFICATION_MODE_ON_DETECT = 2;
	public static int DEFAULT_DETECTING_TIME = 6000;
	
	public int mNotificationMode;
	public SensitivitiesArray mMotionSensitivities;
	public int mMotionSensitivityLevel;
	public int mXInclinationThresh, mYInclinationThresh;
	public int mScreenTimeo, mDetectingTime;
	public boolean mMotionDetectionEnabled;
	
	public Configuration(Context ctx)
	{
		int previousScreenTimeo;
		SharedPreferences mSharedPrefs = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
		mNotificationMode = mSharedPrefs.getInt("NOTIFICATION_MODE", NOTIFICATION_MODE_ALWAYS_ON);
		mMotionSensitivityLevel = mSharedPrefs.getInt("MOTION_SENSITIVITY_LEVEL", 0);
		mXInclinationThresh = mSharedPrefs.getInt("X_INCLINATION_THRESH", 1);
		mYInclinationThresh = mSharedPrefs.getInt("Y_INCLINATION_THRESH", 1);
		mDetectingTime = mSharedPrefs.getInt("DETECTING_TIME", DEFAULT_DETECTING_TIME);
		mMotionDetectionEnabled = mSharedPrefs.getBoolean(NotificationViewIntentListener.TOGGLE_MOTION_DETECTION_ENABLE, true);
		previousScreenTimeo =  mSharedPrefs.getInt("PREVIOUS_SCREEN_TIMEO", -1);

		try {
			mScreenTimeo = Settings.System.getInt(ctx.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
			if(previousScreenTimeo != mScreenTimeo)
			{
				SharedPreferences.Editor e = mSharedPrefs.edit();
				e.putInt("PREVIOUS_SCREEN_TIMEO", mScreenTimeo);
				e.commit(); /* save for next run */
			}
			if(previousScreenTimeo > 0 && previousScreenTimeo > mScreenTimeo)
			{
				Log.e("Configuration.Configuration", "Timeout is shorter than last time, was "
						+ previousScreenTimeo + " now is " + mScreenTimeo);
				/* restore detecting time to default */
				mDetectingTime = DEFAULT_DETECTING_TIME;
				SharedPreferences.Editor e = mSharedPrefs.edit();
				e.putInt("DEFAULT_DETECTING_TIME", DEFAULT_DETECTING_TIME);
				e.commit(); /* save for next run */
			}
		} 
		catch (SettingNotFoundException e) 
		{
			mScreenTimeo = 60000;
			e.printStackTrace();
		}
		mMotionSensitivities = new SensitivitiesArray();
	}

	public boolean isValid()
	{
		return mScreenTimeo > mDetectingTime;
	}
	
	public MotionSensitivityValues getMotionSensitivityValues()
	{
		MotionSensitivityValues msv = mMotionSensitivities.get(mMotionSensitivityLevel);
		if(msv == null)
			return mMotionSensitivities.get(0);
		return msv;
	}

	public void update(Intent intent) {

		/* mScreenTimeo, mdetectingTime */
		if(intent.hasExtra("detectionTime"))
			mDetectingTime = intent.getIntExtra("detectingTime", 8000);
		if(intent.hasExtra("notificationMode"))
			mNotificationMode = intent.getIntExtra("notificationMode", NOTIFICATION_MODE_ALWAYS_ON);
		if(intent.hasExtra("motionSensitivityLevel"))
			mMotionSensitivityLevel = intent.getIntExtra("motionSensitivityLevel", 0);
		if(intent.hasExtra("yInclinationThresh"))
			mYInclinationThresh = intent.getIntExtra("yInclinationThresh", 0);
		if(intent.hasExtra("screenTimeout"))
			mScreenTimeo = intent.getIntExtra("screenTimeout", 60000);
		
	}
}
