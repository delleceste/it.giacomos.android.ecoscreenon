package it.giacomos.android.ecosmartscreenon.service.state;

import it.giacomos.android.ecosmartscreenon.service.Action;

public interface StateListener 
{
	public void onStateLeaving(StateType t, Action a);
}
