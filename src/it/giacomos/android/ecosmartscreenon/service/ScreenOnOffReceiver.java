package it.giacomos.android.ecosmartscreenon.service;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class ScreenOnOffReceiver extends BroadcastReceiver {
	
	private boolean mIsRegistered;

	public ScreenOnOffReceiver()
	{
		super();
		mIsRegistered = false;
	}
	
	public boolean isRegistered()
	{
		return mIsRegistered;
	}
	
	public void setIsRegistered(boolean reg)
	{
		mIsRegistered = reg;
	}
	
	@Override
	/** 
	 * a) Intent.ACTION_SCREEN_ON:
	 *   a1) Screen locked: do not do anything and wait for ACTION_USER_PRESENT, if will happen
	 *   a2) Screen is not locked, maybe the user does not lock the screen or the screen has 
	 *       a timeout between the time it is turned off and the locker is activated: we must
	 *       start the service.
	 *      
	 * b) Intent.ACTION_USER_PRESENT: start the service because it should not have been started
	 *    during Intent.ACTION_SCREEN_ON action.
	 *    
	 * c) Intent.ACTION_SCREEN_OFF: always stop the service
	 */
	public void onReceive(Context context, Intent intent) 
	{
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
		{
	    	
	    	KeyguardManager keyGuardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
	    	if(keyGuardManager.inKeyguardRestrictedInputMode())
	    	{
	    		Log.e("+ ScreenOnOffReceiver ACTION_SCREEN_ON", "screen is locked!!");
	    	}
	    	else
	    	{
	    		Log.e("+ ScreenOnOffReceiver ACTION_SCREEN_ON", "starting ScreenOnOffService");
	    		Intent myIntent = new Intent(context, EcoScreenService.class);
	    		context.startService(myIntent);
	    	}
		}
		else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT))
		{
	    	Log.e("+ ScreenOnOffReceiver ACTION_USER_PRESENT", "starting ScreenOnOffService");
	    	Toast.makeText(context, "starting EcoScreenService", Toast.LENGTH_SHORT).show();
	        Intent myIntent = new Intent(context, EcoScreenService.class);
			context.startService(myIntent);
		}
		else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
		{
			Log.e("- ScreenOnOffReceiver ACTION_SCREEN_OFF", "stopping EcoScreenService");
			Intent myIntent = new Intent(context, EcoScreenService.class);
			context.stopService(myIntent);
		}
		

	}

}
