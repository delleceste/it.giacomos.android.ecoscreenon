package it.giacomos.android.ecosmartscreenon.service;



import it.giacomos.android.ecosmartscreenon.R;
import it.giacomos.android.ecosmartscreenon.SettingsActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.WindowManager;
import android.widget.Toast;

public class EcoScreenService extends Service implements StateListener, ActivityBroadcastReceiverListener
{
	public static String CONFIG_CHANGE_INTENT = "config-change-intent";

	private Configuration mConfiguration;
	private int mEcoScreenServiceId = 100;
	private State mState;
	private String mNotificationTitle;
	private final int mNotificationID = 1;
	private WakeLock mScreenWL;
	private SharedPreferences mSharedPrefs;
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


			/// TEMP!!
			/// mScreenTimeo = 20000;

			if(mConfiguration.mScreenTimeo > 10000)
			{
				mConfiguration.mMigrateToActiveTimeo = mConfiguration.mScreenTimeo - 10000;

				/* when screen goes on we start in IDLE state */
				mState = new IdleState(mConfiguration.mMigrateToActiveTimeo, this);
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
		Log.e("onStateLeaving", "type " + t + " action " + a);
		if(a == Action.CANCELLED)
		{
		}
		if(t == StateType.DETECTING && a == Action.KEEP_ON)
		{
			//			Toast.makeText(this, "Triggering user action and waiting other " + mMigrateToActiveTimeo + "ms", Toast.LENGTH_LONG).show();
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			Log.e("EcoScreenService.onStateLeaving", "GETTING WAKE LOCK BRIGHT");
			if(mScreenWL == null || !mScreenWL.isHeld())
			{
				mScreenWL = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "EcoScreenService");
				//				pm.userActivity(SystemClock.uptimeMillis(), false);
				mScreenWL.acquire();
			}
			/* Otherwise the screen lock is already allocated and held */
			/* restart cycle */
			mState = new IdleState(mConfiguration.mMigrateToActiveTimeo, this);
		}
		else if(t == StateType.IDLE && a == Action.IDLE_LEFT)
		{
			//			Toast.makeText(this, "Going to DetectMode ", Toast.LENGTH_SHORT).show();
			StateDetecting sDetect = new StateDetecting(this, mConfiguration.mScreenTimeo - mConfiguration.mMigrateToActiveTimeo, 
					this, mConfiguration.getMotionSensitivityValues());
			sDetect.setThresholds(mConfiguration.mXInclinationThresh, mConfiguration.mYInclinationThresh);
			mState = sDetect;
		}
		else if(t == StateType.DETECTING && a == Action.NONE)
		{
			Log.e("EcoScreenService.onStateLeaving", "RELEASINGs WAKE LOCK BRIGHT");
			if(mScreenWL != null && mScreenWL.isHeld())
			{
				mScreenWL.release();
				mScreenWL = null;
			}
			//			Toast.makeText(this, "No user action detected", Toast.LENGTH_LONG).show();
			mState = new IdleState(mConfiguration.mMigrateToActiveTimeo, this);
		}
		mBuildNotification(a);
	}

	@Override
	public void onDestroy()
	{
		Log.e("EcoScreenService.onDestroy", "cancelling state, RELEASING LOCK");
		if(mState != null) /* may be null if system screen off timeout is too short */
			mState.cancel();

		if(mScreenWL != null && mScreenWL.isHeld())
			mScreenWL.release();

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
		int iconId = R.drawable.ic_statusbar_off;
		if(mState.getType() == StateType.DETECTING && a != Action.KEEP_ON)
		{
			iconId = R.drawable.ic_statusbar_detecting;
			msg = res.getString(R.string.shake);
		}
		else if(mState.getType() == StateType.DETECTING && a == Action.KEEP_ON)
		{
			/* when invoked by onKeepOnRenewal */
			iconId = R.drawable.ic_statusbar_on;
			msg = res.getString(R.string.shake);
		}
		else if(mState.getType() == StateType.IDLE && a == Action.KEEP_ON)
		{
			msg = res.getString(R.string.stayingon);
			iconId = R.drawable.ic_statusbar_on;
		}

		if(mState.getType() == StateType.DETECTING)
			doNotify = (doNotify && (mConfiguration.mNotificationMode == Configuration.NOTIFICATION_MODE_ON_DETECT));
		else if(mState.getType() == StateType.IDLE && (mConfiguration.mNotificationMode == Configuration.NOTIFICATION_MODE_ON_DETECT))
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
	/* update the icon */
	public void onKeepOnRenewal(Action a) 
	{
		mBuildNotification(a);
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
