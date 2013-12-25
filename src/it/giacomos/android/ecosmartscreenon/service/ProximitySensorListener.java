package it.giacomos.android.ecosmartscreenon.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ProximitySensorListener implements SensorEventListener
{
	private ProximitySensorListenerListener mProximitySensorListenerListener;
	private SensorManager mSensorManager;
	private boolean mSensorRegistered;
	private int mUpdateCount;

	public ProximitySensorListener(ProximitySensorListenerListener l)
	{
		mProximitySensorListenerListener = l;	
	}
	
	public void start(Context ctx)
	{
		mSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null)
		{
			Log.e("ProximitySensorListener", "registering PROXIMITY listener");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), 
					SensorManager.SENSOR_DELAY_NORMAL);
			mSensorRegistered = true;
			mUpdateCount = 0;
		}
	}
	
	public void stop()
	{
		if(mSensorRegistered)
		{
			mSensorManager.unregisterListener(this);
			mSensorRegistered = false;
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		float distance = event.values[0];
		mUpdateCount++;
		Log.e("ProximitySensorListener", " value " + distance);
		mProximitySensorListenerListener.onProximityChanged(distance < event.sensor.getMaximumRange(),
				mUpdateCount);
	}



}
