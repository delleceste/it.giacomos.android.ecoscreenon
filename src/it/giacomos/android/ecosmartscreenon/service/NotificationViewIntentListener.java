package it.giacomos.android.ecosmartscreenon.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class NotificationViewIntentListener extends BroadcastReceiver {

	public static String TOGGLE_MOTION_DETECTION_ENABLE = "TOGGLE_MOTION_DETECTION_ENABLE";
	
	public NotificationViewIntentListener() 
	{
		
	}


	@Override
	public void onReceive(Context ctx, Intent intent) {
		
		String action = intent.getAction();
		if(action.compareTo(TOGGLE_MOTION_DETECTION_ENABLE) == 0)
		{
			SharedPreferences sp = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
			boolean motionDetectionEnabled = sp.getBoolean(TOGGLE_MOTION_DETECTION_ENABLE, true);
			Editor e = sp.edit();
			/* toggle the value */
			e.putBoolean(TOGGLE_MOTION_DETECTION_ENABLE, !motionDetectionEnabled);
			e.commit();
			
			/* now restart the service */
			Intent myOnOffServiceIntent = new Intent(ctx, EcoScreenServiceLauncher.class);
			ctx.stopService(myOnOffServiceIntent);
			myOnOffServiceIntent.putExtra("restartService", true);
			ctx.startService(myOnOffServiceIntent);
		}
	}

}
