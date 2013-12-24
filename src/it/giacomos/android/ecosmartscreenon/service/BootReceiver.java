package it.giacomos.android.ecosmartscreenon.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) 
	{
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		{
			SharedPreferences sharedPrefs = 
					context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
			if(sharedPrefs.getBoolean("BOOT_ENABLE", true))
			{
				Log.e("BootReceiver.onReceive", "Starting EcoScreenServiceLauncher");
				Intent myIntent = new Intent(context, EcoScreenServiceLauncher.class);
				myIntent.putExtra("onBoot", true);
				context.startService(myIntent);
			}
		}

	}
}
