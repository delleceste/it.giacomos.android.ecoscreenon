package it.giacomos.android.ecosmartscreenon.service;

import android.util.SparseArray;

public class SensitivitiesArray extends SparseArray<MotionSensitivityValues> 
{
	public SensitivitiesArray()
	{
		super();
		
		put(0, new MotionSensitivityValues(0.2f, 0.3f));
		put(1, new MotionSensitivityValues(0.35f, 0.45f));
		put(2, new MotionSensitivityValues(0.45f, 0.55f));
		put(3, new MotionSensitivityValues(0.55f, 0.65f));
		put(4, new MotionSensitivityValues(0.6f, 0.7f));
		put(5, new MotionSensitivityValues(0.65f, 0.75f));
		put(6, new MotionSensitivityValues(0.95f, 1.15f));
		put(7, new MotionSensitivityValues(1.1f, 1.25f));
		put(8, new MotionSensitivityValues(1.5f, 1.6f));
		put(9, new MotionSensitivityValues(1.65f, 1.7f));
		put(10, new MotionSensitivityValues(1.8f, 1.9f));
	}
	
	
}
