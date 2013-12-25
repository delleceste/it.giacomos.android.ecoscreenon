package it.giacomos.android.ecosmartscreenon.service;

import android.os.Handler;


public abstract class IdleSensorsWakelockHandlerState implements State, Runnable
{

	private Handler mHandler;
	private StateListener mStateListener;
	private Action mAction;
	
	public IdleSensorsWakelockHandlerState(int timeout, StateListener sl)
	{
		mHandler = new Handler();
		mHandler.postDelayed(this, timeout);
		mStateListener = sl;
		mAction = Action.NONE;
	}
	
	@Override
	public abstract StateType getType();

	@Override
	public void cancel()
	{
		mHandler.removeCallbacks(this);
	}

	@Override
	public void run() 
	{
		mStateListener.onStateLeaving(getType(), Action.IDLE_SENSORS_TIMEOUT);
	}

	@Override
	public Action getAction() {
		return mAction;
	}
	
	@Override
	public boolean isSensorsIdleState()
	{
		return true;
	}
}
