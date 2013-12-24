package it.giacomos.android.ecosmartscreenon.service;

import android.os.Handler;


public class IdleState implements State, Runnable
{

	private Handler mHandler;
	private StateListener mStateListener;

	public IdleState(int timeout, StateListener sl)
	{
		mHandler = new Handler();
		mHandler.postDelayed(this, timeout);
		mStateListener = sl;
		
	}
	
	@Override
	public StateType getType() {
		return StateType.IDLE;
	}

	@Override
	public void cancel()
	{
		mHandler.removeCallbacks(this);
		mStateListener.onStateLeaving(StateType.IDLE, Action.CANCELLED);
	}

	@Override
	public void run() 
	{
		mStateListener.onStateLeaving(StateType.IDLE, Action.IDLE_LEFT);
	}

	@Override
	public Action getAction() {
		return Action.NONE;
	}
}
