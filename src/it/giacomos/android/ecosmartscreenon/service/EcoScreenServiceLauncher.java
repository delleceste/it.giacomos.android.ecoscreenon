package it.giacomos.android.ecosmartscreenon.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class EcoScreenServiceLauncher extends Service {

	private ScreenOnOffReceiver mScreenOnOffReceiver;
	
	public EcoScreenServiceLauncher()
	{
		super();
		mScreenOnOffReceiver = null;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		Bundle extras = intent.getExtras();
		if(extras != null && extras.getBoolean("onBoot"))
		{
			boolean startBypassingPrefs = false;
			startBypassingPrefs = (extras.containsKey("startBypassingPrefs") 
				&& extras.getBoolean("startBypassingPrefs"));
			SharedPreferences shPref = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
		
			Log.e("EcoScreenServiceLauncher.onStartCommand()", "onBoot"
				+ (shPref.getBoolean("EcoScreenServiceEnable", true) == true));

			if(shPref.getBoolean("EcoScreenServiceEnable", true) == true || startBypassingPrefs)
			{
				mRegisterReceiver();
				/* on boot: service will be started on action user present */
		        Log.e("EcoScreenServiceLauncher.onStartCommand()", "registering receivers");
			}
		}
		else if(extras != null && extras.getBoolean("restartService"))
		{
			Log.e("EcoScreenServiceLauncher.onStartCommand", "RESTARTING EcoScreenService");
			Intent myIntent = new Intent(this, EcoScreenService.class);
			stopService(myIntent);
			startService(myIntent);
			mRegisterReceiver();
		}
		else if(extras != null && extras.getBoolean("startService"))
		{
			Intent myIntent = new Intent(this, EcoScreenService.class);
			startService(myIntent);
			mRegisterReceiver();
		}
		return Service.START_STICKY;
	}

	public boolean isScreenOnOffReceiverRegistered()
	{
		if(mScreenOnOffReceiver == null)
			return false;
		else
			return mScreenOnOffReceiver.isRegistered();
	}
	
	private void mRegisterReceiver()
	{
		IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //filter.addAction(Intent.ACTION_SCREEN_ON);
        
        if(mScreenOnOffReceiver == null)
        {
        	mScreenOnOffReceiver = new ScreenOnOffReceiver();
        	mScreenOnOffReceiver.setIsRegistered(true);
        	registerReceiver(mScreenOnOffReceiver , filter);
        }
	}
	
	@Override
	public void onDestroy()
	{
		Intent myIntent = new Intent(this, EcoScreenService.class);
		stopService(myIntent);
		if(this.mScreenOnOffReceiver != null)
		{
			unregisterReceiver(mScreenOnOffReceiver);
        	mScreenOnOffReceiver.setIsRegistered(false);
			mScreenOnOffReceiver = null;
		}
	}
	
}
