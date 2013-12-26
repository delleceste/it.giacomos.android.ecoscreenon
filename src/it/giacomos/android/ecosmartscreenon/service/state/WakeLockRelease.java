package it.giacomos.android.ecosmartscreenon.service.state;

import android.os.PowerManager.WakeLock;
import android.util.Log;

public class WakeLockRelease extends IdleSensorsWakelockHandler {

	private WakeLock mWakeLock;

	public WakeLockRelease(int timeout, StateListener sl, WakeLock screenWL) {
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
