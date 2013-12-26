package it.giacomos.android.ecosmartscreenon.service.state;

import it.giacomos.android.ecosmartscreenon.service.Action;

public interface State 
{
	public StateType getType();
	
	public Action getAction();
	
	public void cancel();
	
	public boolean isSensorsIdleState();
}
