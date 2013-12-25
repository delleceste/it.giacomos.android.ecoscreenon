package it.giacomos.android.ecosmartscreenon.service;

import android.os.PowerManager.WakeLock;
import android.util.Log;

public class WakeLockReleaseState extends IdleSensorsWakelockHandlerState {

	private WakeLock mWakeLock;

	public WakeLockReleaseState(int timeout, StateListener sl, WakeLock screenWL) {
		super(timeout, sl);
		mWakeLock = screenWL;
		Log.e("WakeLockReleaseState", "released wakelock on " + screenWL + " is held " + screenWL.isHeld());
		if(screenWL.isHeld())
			screenWL.release();
	}

	@Override
	public StateType getType() 
	{
		return StateType.WAKELOCK_RELEASE;
	}
	
	public WakeLock getWakeLock()
	{
		return mWakeLock;
	}

}
