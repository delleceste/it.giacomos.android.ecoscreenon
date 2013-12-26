package it.giacomos.android.ecosmartscreenon.service;



import it.giacomos.android.ecosmartscreenon.R;
import it.giacomos.android.ecosmartscreenon.SettingsActivity;
import it.giacomos.android.ecosmartscreenon.service.state.State;
import it.giacomos.android.ecosmartscreenon.service.state.Detecting;
import it.giacomos.android.ecosmartscreenon.service.state.DetectingProximity;
import it.giacomos.android.ecosmartscreenon.service.state.StateListener;
import it.giacomos.android.ecosmartscreenon.service.state.StateType;
import it.giacomos.android.ecosmartscreenon.service.state.WakeLockHold;
import it.giacomos.android.ecosmartscreenon.service.state.WakeLockRelease;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class EcoScreenService extends Service implements StateListener, ActivityBroadcastReceiverListener
{
	public static String CONFIG_CHANGE_INTENT = "config-change-intent";

	private Configuration mConfiguration;
	private State mState;
	private String mNotificationTitle;
	private final int mNotificationID = 1;
	private WakeLock mScreenWL;
	private ActivityBroadcastReceiver mActivityBroadcastReceiver;

	public EcoScreenService()
	{
		super();
		mState = null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		/* start the service only if not previously started.
		 * Avoid multiple onStartCommand executions 
		 */
		if(mState == null)
		{
			mConfiguration = new Configuration(this);		
			mNotificationTitle = getResources().getString(R.string.app_name);

			Log.e("EcoScreenService.onStartCommand", "service started, screen timeo = " + mConfiguration.mScreenTimeo);

			if(mConfiguration.isValid())
			{
				/* when screen goes on we start in IDLE state */
				WakeLockHold wakeLockHoldState  = new WakeLockHold(this, mConfiguration.mDetectingTime, this, mScreenWL);
				mScreenWL = wakeLockHoldState.getWakeLock();
				mState = wakeLockHoldState; /* update reference to new wakelock */
				/* start with the green notification icon */
				mBuildNotification(Action.KEEP_ON);
			}
			else
			{
				Toast.makeText(this, "Timeout " + mConfiguration.mScreenTimeo + " too short!\nStopping EcoScreenService", 
						Toast.LENGTH_LONG).show();

				this.stopSelf();
			}

			/* broadcast receiver aimed at receiving configuration messages from the settings activity */
			if(mActivityBroadcastReceiver == null) /* be careful to register only once */
			{
				Log.e("EcoScreenService.onStartCommand", "registering Local broadcast receiver");
				mActivityBroadcastReceiver = new ActivityBroadcastReceiver(this);
				LocalBroadcastManager.getInstance(this).registerReceiver(mActivityBroadcastReceiver, 
						new IntentFilter(CONFIG_CHANGE_INTENT));
			}

		}
		else
			Log.e("EcoScreenService.onStartCommand", "state already " + mState.getType());
		return Service.START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onStateLeaving(StateType t, Action a) 
	{
		int idleTime = mConfiguration.mScreenTimeo - mConfiguration.mDetectingTime -
				DetectingProximity.PROXIMITY_DETECTION_TIME;
		
		Log.e("onStateLeaving", "type " + t + " action " + a);
		/* Action.CANCELLED cannot be set */
		if(a == Action.NONE)
		{
			Log.e("EcoScreenService.onStateLeaving", "RELEASING WAKE LOCK BRIGHT Action " + a);
			//			Toast.makeText(this, "No user action detected", Toast.LENGTH_LONG).show();
			mState = new WakeLockRelease(idleTime, this, mScreenWL);
		}
		else if(t == StateType.DETECTING && a == Action.KEEP_ON)
		{
			/* must test proximity. This state lasts  DetectingProximity.PROXIMITY_DETECTION_TIME */
			mState = new DetectingProximity(this, this);
		}
		else if(t == StateType.DETECTING_PROXIMITY && a == Action.KEEP_ON)
		{
			//			Toast.makeText(this, "Triggering user action and waiting other " + mdetectingTime + "ms", Toast.LENGTH_LONG).show();
			/* Otherwise the screen lock is already allocated and held */
			/* restart cycle */
			WakeLockHold wakeLockHoldState = new WakeLockHold(this, idleTime, this, mScreenWL);
			mState = wakeLockHoldState;
			/* keep reference to the new wakelock */
			mScreenWL = wakeLockHoldState.getWakeLock();
		}
		else if(mState.isSensorsIdleState() && a == Action.IDLE_SENSORS_TIMEOUT)
		{
			//			Toast.makeText(this, "Going to DetectMode ", Toast.LENGTH_SHORT).show();
			Detecting sDetect = new Detecting(this, mConfiguration.mDetectingTime, 
					this, mConfiguration.getMotionSensitivityValues());
			sDetect.setThresholds(mConfiguration.mXInclinationThresh, mConfiguration.mYInclinationThresh);
			mState = sDetect;
		}
		mBuildNotification(a);
	}

	@Override
	public void onDestroy()
	{
		Log.e("EcoScreenService.onDestroy", "cancelling state, RELEASING LOCK");
		if(mState != null) /* may be null if system screen off timeout is too short */
			mState.cancel();
		
		Log.e("EcoScreenService.onDestroy", "cancelling state, RELEASING LOCK "
				+ mScreenWL);
		
		if(mScreenWL != null && mScreenWL.isHeld())
		{
			mScreenWL.release();
			Log.e("EcoScreenService.onDestroy", " RELEASING LOCK ");
		}
		
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(mNotificationID);

		/* unregister local broadcast receiver */
		if(mActivityBroadcastReceiver != null) 
		{
			Log.e("EcoScreenService.onDestroy", "unregistering Local broadcast receiver");
			LocalBroadcastManager.getInstance(this).unregisterReceiver(mActivityBroadcastReceiver);
			mActivityBroadcastReceiver = null;
		}
		
		/* if we get destroyed, mark mState as null so that onStartCommand
		 * can be fully executed
		 */
		mState = null;
	}

	private void mBuildNotification(Action a)
	{
		boolean doNotify = true;
		if(mConfiguration.mNotificationMode == Configuration.NOTIFICATION_MODE_ALWAYS_OFF)
			doNotify = false;

		Resources res = getResources();
		String msg = res.getString(R.string.goingoff);
		StateType st = mState.getType();
		boolean detecting = !mState.isSensorsIdleState();
		int iconId = R.drawable.ic_statusbar_off;
		if(detecting)
		{
			iconId = R.drawable.ic_statusbar_detecting;
			msg = res.getString(R.string.shake);
		}
		else if(st == StateType.WAKELOCK_HOLD)
		{
			msg = res.getString(R.string.stayingon);
			iconId = R.drawable.ic_statusbar_on;
		}

		if(detecting)
			doNotify = (doNotify && (mConfiguration.mNotificationMode == Configuration.NOTIFICATION_MODE_ON_DETECT));
		else if(!mState.isSensorsIdleState() && (mConfiguration.mNotificationMode == Configuration.NOTIFICATION_MODE_ON_DETECT))
			doNotify = false;

		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if(doNotify || mConfiguration.mNotificationMode == Configuration.NOTIFICATION_MODE_ALWAYS_ON)
		{
			NotificationCompat.Builder mBuilder =
					new NotificationCompat.Builder(this)
			.setSmallIcon(iconId)
			.setContentTitle(mNotificationTitle)
			.setContentText(msg);
			// Creates an explicit intent for an Activity in your app
			Intent resultIntent = new Intent(this, SettingsActivity.class);

			// The stack builder object will contain an artificial back stack for the
			// started Activity.
			// This ensures that navigating backward from the Activity leads out of
			// your application to the Home screen.
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
			// Adds the back stack for the Intent (but not the Intent itself)
			stackBuilder.addParentStack(SettingsActivity.class);
			// Adds the Intent that starts the Activity to the top of the stack
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent( 0,
					PendingIntent.FLAG_UPDATE_CURRENT );
			mBuilder.setContentIntent(resultPendingIntent);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(mNotificationID, mBuilder.build());
		}
		else /* cancel if present */
			mNotificationManager.cancel(mNotificationID);
	}

	@Override
	public void onMessageReceived(Intent intent) 
	{
		Log.e("EcoScreenService.onMessageReceived", intent.getAction());
		if(intent.getAction() == CONFIG_CHANGE_INTENT)
		{
			mConfiguration.update(intent);
			if(intent.hasExtra("notificationMode")) /* refresh icon state */
				mBuildNotification(mState.getAction());
		}
	}
}
