package it.giacomos.android.ecosmartscreenon;

import it.giacomos.android.ecosmartscreenon.service.Action;
import it.giacomos.android.ecosmartscreenon.service.MotionSensitivityValues;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class StateDetector  implements SensorEventListener
{
	public boolean motionDetectionEnabled;
	public MotionSensitivityValues mMotionSensitivityValues;
	public int yThresh;
	private boolean mMoved, mHorizontal;
	private StateDetectorListener mStateDetectorListener;
	int mTimeout;

	private float[] mPreviousValues;
	private SensorManager mSensorManager;
	
	public StateDetector(StateDetectorListener sdl)
	{
		super();
		mStateDetectorListener = sdl;
	}
	
	public void start(Context ctx)
	{
		mSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
		{
			mPreviousValues = null;
			Log.e("StateDetector.start()", "registering listener");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
					SensorManager.SENSOR_DELAY_NORMAL);
		}
	}
	
	public void stop()
	{
		Log.e("StateDetector.stop()", "unregister sensor manager listener");
		mSensorManager.unregisterListener(this);
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			Log.e("Accelerometer ", Math.round(event.values[0]) + " " + Math.round(event.values[1]) + " " +
					Math.round(event.values[2]) + " thresh " + yThresh + " motion sensitivity " +
					mMotionSensitivityValues.v0 + ", " + mMotionSensitivityValues.v2);
			
//			Log.e("Accelerometer ", (event.values[0]) + " " + (event.values[1]) + " " +
//					(event.values[2]));
			
			mMoved = false;
			mHorizontal = true;
			
			/* first of all: if the device is not horizontal, set mHorizontal to false and
			 * skip all other checks.
			 * Use mYThresh for simplicity for both axes.
			 */
			if(Math.round(Math.abs(event.values[0])) > yThresh || Math.round(event.values[1]) > yThresh)
			{
//				Log.e("onSensorChanged", "device not horizontal, " + Math.round(event.values[0]) +
//						"," + Math.round(event.values[1]) + ", " +
//						Math.round(event.values[2]) + ": unregistering listener");
				mHorizontal = false;
			}
			else if(mPreviousValues == null && motionDetectionEnabled)
			{
				mPreviousValues = new float[3];
				System.arraycopy(event.values, 0, mPreviousValues, 0, 3);
			}
			/* if, not else if here */
			if(motionDetectionEnabled && mPreviousValues != null)
			{
				if(Math.abs(event.values[0] - mPreviousValues[0]) > mMotionSensitivityValues.v0)
				{
//					Log.e("onSensorChanged", "device has moved HOR! " + (event.values[0] - mPreviousValues[0]));
					mMoved = true;
				}
				if(Math.abs(event.values[2] - mPreviousValues[2]) > mMotionSensitivityValues.v2)
				{
//					Log.e("onSensorChanged", "device has moved VER! " + (event.values[2] - mPreviousValues[2]));
					mMoved = true;
				}

				/* Device is not horizontal. Test if moved */
				if(mMoved)
				{
//					Log.e("onSensorChanged", "moved " + mMoved + ", horizontal " + mHorizontal + " unregistering ");
				}
				System.arraycopy(event.values, 0, mPreviousValues, 0, 3);

				
			}
			mStateDetectorListener.onDeviceMoved(mMoved);
			mStateDetectorListener.onDeviceInclinated(!mHorizontal);
		}
	}


	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

}
