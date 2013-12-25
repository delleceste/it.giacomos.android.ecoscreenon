package it.giacomos.android.ecosmartscreenon.service;

import android.os.Handler;


public class IdleState implements State, Runnable
{

	private Handler mHandler;
	private StateListener mStateListener;
	private Action mAction;
	
	public IdleState(int timeout, StateListener sl)
	{
		mHandler = new Handler();
		mHandler.postDelayed(this, timeout);
		mStateListener = sl;
		mAction = Action.NONE;
	}
	
	@Override
	public StateType getType() {
		return StateType.IDLE;
	}

	@Override
	public void cancel()
	{
		mHandler.removeCallbacks(this);
	}

	@Override
	public void run() 
	{
		mStateListener.onStateLeaving(StateType.IDLE, Action.IDLE_TIMEOUT);
	}

	@Override
	public Action getAction() {
		return mAction;
	}
}
