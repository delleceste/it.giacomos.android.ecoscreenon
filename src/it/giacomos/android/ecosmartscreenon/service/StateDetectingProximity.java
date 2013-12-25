package it.giacomos.android.ecosmartscreenon.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

public class StateDetectingProximity implements State, 
SensorEventListener, Runnable
{
	public static final int PROXIMITY_DETECTION_TIME = 2000;
	
	private Action mAction;
	private SensorManager mSensorManager;
	boolean mNear;
	private Handler mHandler;
	private StateListener mStateListener;
	
	public StateDetectingProximity(Context ctx, StateListener sl)
	{
		mNear = false;
		mAction = Action.NONE;
		mSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		mStateListener = sl;
		mHandler = new Handler();
		mHandler.postDelayed(this, PROXIMITY_DETECTION_TIME);
		
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null)
		{
			Log.e("ProximitySensorListener", "registering PROXIMITY listener");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), 
					SensorManager.SENSOR_DELAY_NORMAL);
		}
		else
			mLeave();
	}
	
	@Override
	public StateType getType() 
	{
		return StateType.DETECTING_PROXIMITY;
	}

	@Override
	public Action getAction() 
	{
		return mAction;
	}

	@Override
	public void cancel() 
	{
		mClear();
	}
	
	private void mLeave()
	{
		mClear();
		mStateListener.onStateLeaving(StateType.DETECTING_PROXIMITY, mAction);
	}

	private void mClear()
	{
		mHandler.removeCallbacks(this);
		mSensorManager.unregisterListener(this);	
	}
	
	@Override
	public void run() 
	{
		mLeave();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
	{
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float distance = event.values[0];
		Log.e("StateDetectingProximity.onSensorChanged", " value " + distance);
		if(distance < event.sensor.getMaximumRange())
			mAction = Action.NONE;
		else
			mAction = Action.KEEP_ON;
	}
}
