package it.giacomos.android.ecosmartscreenon;

import it.giacomos.android.ecosmartscreenon.service.Action;
import it.giacomos.android.ecosmartscreenon.service.Configuration;
import it.giacomos.android.ecosmartscreenon.service.EcoScreenService;
import it.giacomos.android.ecosmartscreenon.service.EcoScreenServiceLauncher;
import it.giacomos.android.ecosmartscreenon.service.MotionSensitivityValues;
import it.giacomos.android.ecosmartscreenon.service.SensitivitiesArray;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class SettingsActivity extends Activity implements OnCheckedChangeListener,
OnSeekBarChangeListener, OnItemSelectedListener, StateDetectorListener, EditTextListener
{	
	private StateDetector mStateDetector;
	private MyTextWatcher mMyTextWatcher;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
	}

	protected void onResume()
	{
		super.onResume();

		/* initialize gui */
		CheckBox serviceEnableCb = (CheckBox)findViewById(R.id.cbEnableService);
		CheckBox bootEnableCb = (CheckBox)findViewById(R.id.cbStartAtBoot);
		SharedPreferences sharedPrefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
		bootEnableCb.setChecked(sharedPrefs.getBoolean("BOOT_ENABLE", true));
		/* check if EcoScreenServiceLauncher is currently running */
		serviceEnableCb.setChecked(isMyServiceRunning());

		Configuration conf = new Configuration(this);
		SeekBar sb  = (SeekBar) findViewById(R.id.seekBarAngle);
		sb.setProgress(conf.mYInclinationThresh);
		((TextView) findViewById(R.id.tvInclinationAngle)).setText(String.valueOf(threshToDeg(sb.getProgress(), sb.getMax())));

		sb = (SeekBar) findViewById(R.id.seekBarMotionSensitivity);
		sb.setProgress(conf.mMotionSensitivityLevel);
		((TextView) findViewById(R.id.tvMotionSensitivity)).setText(String.valueOf(sb.getProgress()));

		Spinner spinner = (Spinner) findViewById(R.id.spinNotificationMode);
		spinner.setSelection(conf.mNotificationMode);

		/* screen timeout */
		spinner =(Spinner) findViewById(R.id.spScreenTimeo);
		try {
			int screenTimeout = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
			screenTimeout = screenTimeout / 1000; /* ms to seconds */
			spinner.setSelection(this.mGetSpinnerTimeoutPosition(screenTimeout));
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*  detecting time */
		EditText etDetectionTime = (EditText) findViewById(R.id.etDetectionTime);
		int detectionTimeSecs = sharedPrefs.getInt("DETECTING_TIME", Configuration.DEFAULT_DETECTING_TIME);
		detectionTimeSecs /= 1000; /* ms to secs */
		etDetectionTime.setText(String.valueOf(detectionTimeSecs));
		mSetupListeners();

		mStateDetector = new StateDetector(this);
		mStateDetector.motionDetectionEnabled = conf.mMotionDetectionEnabled;
		mStateDetector.yThresh = conf.mYInclinationThresh;
		mStateDetector.mMotionSensitivityValues = conf.getMotionSensitivityValues();
		mStateDetector.start(this);
	}

	protected void onPause()
	{
		super.onPause();

		/* stop sensors!! */
		mStateDetector.stop();

		mRemoveListeners();
	}

	protected void onDestroy()
	{
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	private boolean isMyServiceRunning() 
	{
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) 
		{
			if (EcoScreenServiceLauncher.class.getName().equals(service.service.getClassName())) 
				return true;
		}
		return false;
	}

	@Override
	public void onCheckedChanged(CompoundButton v, boolean checked) 
	{
		if(v.getId() == R.id.cbEnableService)
		{
			Intent myEcoScreenServiceLauncherIntent = new Intent(this, EcoScreenServiceLauncher.class);
			Log.e("onCheckedChanged", " checked " + checked + " running actually.. " + isMyServiceRunning());
			if(checked && !isMyServiceRunning()) /* previously not checked: service not running */
			{
				Log.e("onCheckedChanged", "startin serviice");
				myEcoScreenServiceLauncherIntent.putExtra("startService", true);
				startService(myEcoScreenServiceLauncherIntent);
			}
			else if(!checked) /* previously checked: service running! */
			{
				Log.e("onCheckedChanged", "stopService serviice");
				stopService(myEcoScreenServiceLauncherIntent);
			}
		}
		else if(v.getId() == R.id.cbStartAtBoot)
		{
			SharedPreferences sp = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = sp.edit();
			edit.putBoolean("BOOT_ENABLE", checked);
			edit.commit();
		}
	}

	private void mSetupListeners()
	{
		CheckBox serviceEnableCb = (CheckBox)findViewById(R.id.cbEnableService);
		CheckBox bootEnableCb = (CheckBox)findViewById(R.id.cbStartAtBoot);
		SeekBar sb = (SeekBar) findViewById(R.id.seekBarAngle);
		sb.setOnSeekBarChangeListener(this);
		sb = (SeekBar) findViewById(R.id.seekBarMotionSensitivity);
		sb.setOnSeekBarChangeListener(this);
		serviceEnableCb.setOnCheckedChangeListener(this);
		bootEnableCb.setOnCheckedChangeListener(this);
		Spinner sp = (Spinner) findViewById(R.id.spinNotificationMode);
		sp.setOnItemSelectedListener(this);
		sp =(Spinner) findViewById(R.id.spScreenTimeo);
		sp.setOnItemSelectedListener(this);
		EditText etDetectionTime = (EditText) findViewById(R.id.etDetectionTime);
		mMyTextWatcher = new MyTextWatcher(this);
		etDetectionTime.addTextChangedListener(mMyTextWatcher);
	}

	private void mRemoveListeners()
	{
		CheckBox serviceEnableCb = (CheckBox)findViewById(R.id.cbEnableService);
		CheckBox bootEnableCb = (CheckBox)findViewById(R.id.cbStartAtBoot);
		SeekBar sb = (SeekBar) findViewById(R.id.seekBarAngle);
		sb.setOnSeekBarChangeListener(null);
		serviceEnableCb.setOnCheckedChangeListener(null);
		bootEnableCb.setOnCheckedChangeListener(null);
		sb = (SeekBar) findViewById(R.id.seekBarMotionSensitivity);
		sb.setOnSeekBarChangeListener(null);
		Spinner sp = (Spinner) findViewById(R.id.spinNotificationMode);
		sp.setOnItemSelectedListener(null);
		sp =(Spinner) findViewById(R.id.spScreenTimeo);
		sp.setOnItemSelectedListener(null);
		EditText etDetectionTime = (EditText) findViewById(R.id.etDetectionTime);
		etDetectionTime.removeTextChangedListener(mMyTextWatcher);
	}

	@Override
	public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) 
	{
		Log.e("onProgressChanged" , "progress");
		if(fromUser && sb.getId() == R.id.seekBarAngle)
		{
			Intent i = new Intent(EcoScreenService.CONFIG_CHANGE_INTENT);
			i.putExtra("yInclinationThresh", progress); /* see Configuration for extra string keys */
			LocalBroadcastManager.getInstance(this).sendBroadcast(i);

			/* convert to degrees */
			((TextView) findViewById(R.id.tvInclinationAngle)).setText(String.valueOf(threshToDeg(progress, sb.getMax())));
			/* update simulation values */
			mStateDetector.yThresh = progress;
		}
		else if(fromUser &&  sb.getId() == R.id.seekBarMotionSensitivity)
		{
			Intent i = new Intent(EcoScreenService.CONFIG_CHANGE_INTENT);
			i.putExtra("motionSensitivityLevel", progress); /* see Configuration for extra string keys */
			LocalBroadcastManager.getInstance(this).sendBroadcast(i);
			((TextView) findViewById(R.id.tvMotionSensitivity)).setText(String.valueOf(progress));
			SensitivitiesArray sensitivitiesA = new SensitivitiesArray();
			MotionSensitivityValues msv = sensitivitiesA.get(progress);
			mStateDetector.mMotionSensitivityValues = msv;
		}

	}

	private int threshToDeg(int thresh, int max)
	{
		return Math.round(90 * thresh / max);
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) 
	{

	}

	@Override
	public void onStopTrackingTouch(SeekBar sb) 
	{
		SharedPreferences sp = this.getSharedPreferences(getPackageName(), MODE_PRIVATE);
		SharedPreferences.Editor e = sp.edit();
		if(sb.getId() == R.id.seekBarAngle)
			e.putInt("Y_INCLINATION_THRESH", sb.getProgress());
		else if(sb.getId() == R.id.seekBarMotionSensitivity)
			e.putInt("MOTION_SENSITIVITY_LEVEL", sb.getProgress());
		e.commit();
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long rowIdOfSelectedItem) 
	{
		if(parent.getId() == R.id.spinNotificationMode)
		{
			/* see Configuration for extra string keys */
			Intent i = new Intent(EcoScreenService.CONFIG_CHANGE_INTENT);
			i.putExtra("notificationMode", position);
			LocalBroadcastManager.getInstance(this).sendBroadcast(i);
			SharedPreferences sp = this.getSharedPreferences(getPackageName(), MODE_PRIVATE);
			SharedPreferences.Editor e = sp.edit();
			Log.e("SettingsActivity.onItemSelected", "saving notification mode to  "  + position);
			e.putInt("NOTIFICATION_MODE", position);
			e.commit();
		}
		else if(parent.getId() == R.id.spScreenTimeo)
		{
			int time;
			switch (position) {
			case 0:
				time = 15000;
				break;
			case 1:
				time = 30000;
				break;
			case 2:
			default:
				time = 60000;
				break;
			case 3:
				time = 120000;
				break;
			case 4:
				time = 5 * 60000;
				break;
			case 5:
				time = 10 * 60000;
				break;
			}
			boolean success = android.provider.Settings.System.putInt(getContentResolver(),
					Settings.System.SCREEN_OFF_TIMEOUT, time);
			
			if(success)
			{
				Intent i = new Intent(EcoScreenService.CONFIG_CHANGE_INTENT);
				i.putExtra("screenTimeout", time);
				LocalBroadcastManager.getInstance(this).sendBroadcast(i);
			}
		}

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) 
	{

	}

	@Override
	public void onDeviceMoved(boolean moved) 
	{
		int resId, stringId;
		if(moved)
		{
			resId = R.drawable.ic_statusbar_on;
			stringId = R.string.device_movement_activates_screen;
		}
		else
		{
			resId = R.drawable.ic_statusbar_off;
			stringId = R.string.device_movement_doesnt_activate_screen;
		}
		ImageView iv = (ImageView) findViewById(R.id.imageViewSensitivityLed);
		iv.setImageResource(resId);	
		TextView tv = (TextView) findViewById(R.id.tvMotion);
		tv.setText(stringId);
	}

	@Override
	public void onDeviceInclined(boolean inclinated) 
	{
		int resId, stringId;
		if(inclinated)
		{
			resId = R.drawable.ic_statusbar_on;
			stringId = R.string.inclination_activates_screen;
		}
		else
		{
			resId = R.drawable.ic_statusbar_off;
			stringId = R.string.inclination_doesnt_activate_screen;
		}
		ImageView iv = (ImageView) findViewById(R.id.imageViewAngleLed);
		iv.setImageResource(resId);
		TextView tv = (TextView) findViewById(R.id.tvInclination);
		tv.setText(stringId);
	}

	private int mGetSpinnerTimeoutPosition(int timeo)
	{
		if(timeo <= 15)
			return 0;
		if (timeo <= 30)
			return 1;
		if(timeo <= 60)
			return 2;
		if(timeo <= 120)
			return 3;
		if(timeo <= 5 * 60)
			return 4;
		else if(timeo <= 600)
			return 5;
		return 6;
	}

	@Override
	public void onEditTextValueChanged(int detectionTime) 
	{
		Intent i = new Intent(EcoScreenService.CONFIG_CHANGE_INTENT);
		i.putExtra("detectionTime", detectionTime); /* see Configuration for extra string keys */
		LocalBroadcastManager.getInstance(this).sendBroadcast(i);
		
		SharedPreferences sp = getSharedPreferences(getPackageName(), MODE_PRIVATE);
		SharedPreferences.Editor e = sp.edit();
		e.putInt("DEFAULT_DETECTING_TIME", detectionTime);
		e.commit();
	}
}
