package it.giacomos.android.ecosmartscreenon.service;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class WakeLockHoldState extends IdleSensorsWakelockHandlerState 
{
	private WakeLock mWakeLock;
	
	public WakeLockHoldState(Context ctx, int timeout, StateListener sl, WakeLock screenWL) 
	{
		super(timeout, sl);
		if(screenWL == null)
		{
			PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
			/* acquire lock bright in order that the system does not dim while we keep the
			 * screen active.
			 */
			screenWL = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "EcoScreenService");
		}
		if(!screenWL.isHeld())
		{
			screenWL.acquire();
			Log.e("WakeLockHoldState", "acquired wakelock on " +screenWL);
		}
		else
		{
			Log.e("WakeLockHoldState", "wakelock was already held " +screenWL);
		}
		mWakeLock = screenWL;
		
	}

	public WakeLock getWakeLock()
	{
		return mWakeLock;
	}
	
	@Override
	public StateType getType() {
		
		return StateType.WAKELOCK_HOLD;
	}
	
	@Override
	public void cancel()
	{
		super.cancel();
		Log.e("WakeLockHoldState", "releasing  wakelock if held " + mWakeLock);
		if(mWakeLock.isHeld())
			mWakeLock.release();
	}

}
