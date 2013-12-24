package it.giacomos.android.ecosmartscreenon.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

public class StateDetecting implements State, SensorEventListener, Runnable {

	private SensorManager mSensorManager;
	private StateListener mStateListener;
	private Context mContext;
	private boolean mMotionDetectionEnabled;
	private MotionSensitivityValues mMotionSensitivityValues;
	private int mXThresh, mYThresh;
	private boolean mMoved, mHorizontal;
	private Action mAction;
	
	int mTimeout;

	private float[] mPreviousValues;
	private Handler mHandler;


	public StateDetecting(Context ctx,
			int timeoutMillis, 
			StateListener sl, 
			MotionSensitivityValues msv)
	{
		mContext = ctx;
		mMoved = false;
		mHorizontal = true;
		mStateListener = sl;
		mXThresh = 1;
		mYThresh = 0;
		mTimeout = Math.round(timeoutMillis * 0.90f);
		mMotionSensitivityValues = msv;
		mAction = Action.NONE; /* init */
		
//		Log.e("timeout will be ", "timeot " + mTimeout);
		
		mHandler = new Handler();
		mHandler.postDelayed(this, mTimeout);
		
		mSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
		{
			mPreviousValues =	 null;

			Log.e("StateActive", "registering listener");
			mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
					SensorManager.SENSOR_DELAY_NORMAL);
		}

		/* whenever this setting changes, the service is reloaded, so it is enough to initialize 
		 * this value here.
		 */
		SharedPreferences sp = mContext.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE);
		mMotionDetectionEnabled = sp.getBoolean(NotificationViewIntentListener.TOGGLE_MOTION_DETECTION_ENABLE, true);

	}

	public void setThresholds(int xt, int yt)
	{
		mXThresh = xt;
		mYThresh = yt;
	}
	
	public void setXThreshold(int t)
	{
		mXThresh = t;
	}
	
	public void setYThreshold(int t)
	{
		mYThresh = t;
	}
	
	@Override
	public StateType getType() 
	{
		return StateType.DETECTING;
	}
	
	public Action getAction()
	{
		return mAction;
	}

	@Override
	public void cancel() 
	{
		mAction = Action.CANCELLED;
		mHandler.removeCallbacks(this);
		mSensorManager.unregisterListener(this);
		mStateListener.onStateLeaving(StateType.DETECTING, Action.CANCELLED);
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) 
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) 
	{
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			Log.e("Accelerometer ", Math.round(event.values[0]) + " " + Math.round(event.values[1]) + " " +
					Math.round(event.values[2]) + " thresh " + mYThresh + " motion sensitivity " +
					mMotionSensitivityValues.v0 + ", " + mMotionSensitivityValues.v2);
			
//			Log.e("Accelerometer ", (event.values[0]) + " " + (event.values[1]) + " " +
//					(event.values[2]));
			
			mMoved = false;
			mHorizontal = true;
			
			/* first of all: if the device is not horizontal, set mHorizontal to false and
			 * skip all other checks.
			 * Use mYThresh for simplicity for both axes.
			 */
			if(Math.round(Math.abs(event.values[0])) > mYThresh || Math.round(event.values[1]) > mYThresh)
			{
//				Log.e("onSensorChanged", "device not horizontal, " + Math.round(event.values[0]) +
//						"," + Math.round(event.values[1]) + ", " +
//						Math.round(event.values[2]) + ": unregistering listener");
				mHorizontal = false;
				mSensorManager.unregisterListener(this);
			}
			else if(mPreviousValues == null && mMotionDetectionEnabled)
			{
				mPreviousValues = new float[3];
				System.arraycopy(event.values, 0, mPreviousValues, 0, 3);
			}
			else if (mMotionDetectionEnabled)
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
					mSensorManager.unregisterListener(this);
				}
				else /* not horizontal, not moved, save data for next sensor update */
					System.arraycopy(event.values, 0, mPreviousValues, 0, 3);


				

				//			Log.e("Accelerometer ", event.values[0] + " " + event.values[1] + " " +
				//								event.values[2]);
				//mRecalculateAngles();
				
			}
			if(mMoved || !mHorizontal)
			{
				mAction = Action.KEEP_ON;
				mStateListener.onKeepOnRenewal(Action.KEEP_ON);
			}
		}
	}


	@Override
	public void run() {
		
		mSensorManager.unregisterListener(this);
		Log.e("StateActive.run()", "leaving state DETECTING");
		if(mMoved || !mHorizontal)
		{
			mAction = Action.KEEP_ON;
			this.mStateListener.onStateLeaving(getType(), mAction);
		}
		else
		{
			mAction = Action.NONE;
			this.mStateListener.onStateLeaving(getType(), mAction);
		}

	}

//	private void mRecalculateAngles() 
//	{
//		if(mAccelChanged && mMagFChanged)
//		{
//			SensorManager.getRotationMatrix(mRotationMatrix, null, mValuesAccel, mValuesMagnet);
//			SensorManager.getOrientation(mRotationMatrix, mValuesOrientation);
//			final String test;
//			test = "results: " + Math.toDegrees(mValuesOrientation[0])
//					+" "+ Math.toDegrees(mValuesOrientation[1])+ " "+ Math.toDegrees(mValuesOrientation[2]);
//			// Log.e("mRecalculateAngles", test);
//		}
//	}
	
}
