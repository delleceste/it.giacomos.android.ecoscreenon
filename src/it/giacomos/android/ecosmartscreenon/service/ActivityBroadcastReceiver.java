package it.giacomos.android.ecosmartscreenon.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ActivityBroadcastReceiver extends BroadcastReceiver 
{
	private ActivityBroadcastReceiverListener mActivityBroadcastReceiverListener;
	
	public ActivityBroadcastReceiver(ActivityBroadcastReceiverListener l)
	{
		super();
		mActivityBroadcastReceiverListener = l;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		if(mActivityBroadcastReceiverListener != null)
			mActivityBroadcastReceiverListener.onMessageReceived(intent);

	}

}
