package it.giacomos.android.ecosmartscreenon.service;

public interface StateListener {

	public void onStateLeaving(StateType t, Action a);
	
	public void onKeepOnRenewal(Action a);
}
