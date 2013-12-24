package it.giacomos.android.ecosmartscreenon;


public interface StateDetectorListener 
{
	public void onDeviceMoved(boolean moved);
	
	public void onDeviceInclinated(boolean inclinated);
}
