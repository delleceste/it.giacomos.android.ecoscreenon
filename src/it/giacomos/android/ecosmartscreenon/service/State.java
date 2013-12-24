package it.giacomos.android.ecosmartscreenon.service;

public interface State 
{
	public StateType getType();
	
	public Action getAction();
	
	public void cancel();
}
