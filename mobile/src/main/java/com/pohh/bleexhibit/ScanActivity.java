package com.pohh.bleexhibit;

import android.app.Activity;
import android.app.SearchManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.gimbal.proximity.Proximity;
import com.gimbal.proximity.ProximityError;
import com.gimbal.proximity.ProximityFactory;
import com.gimbal.proximity.ProximityListener;
import com.gimbal.proximity.ProximityOptions;
import com.gimbal.proximity.Transmitter;
import com.gimbal.proximity.Visit;
import com.gimbal.proximity.VisitListener;
import com.gimbal.proximity.VisitManager;

import java.util.Date;


public class ScanActivity extends Activity implements ProximityListener, VisitListener {

	public static final int REQUEST_ENABLE_BT = 1;
	public static final int RSSI_THRESHOLD = -50;
	public static final int APP_SHOPPING_CATEGORY_VIEWPAGER_PAGE = 1;

	private TextView rssiView;
	private TextView idView;
	private ToggleButton proximityToggleView;

	String queryTerm;

	VisitManager visitMgr;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scan);
		rssiView = (TextView)findViewById(R.id.beacon_rssi);
		idView = (TextView)findViewById(R.id.beacon_identifier);
		proximityToggleView = (ToggleButton)findViewById(R.id.proximity_toggle);

		setProximityEnabled(false);

		Proximity.initialize(this, getString(R.string.gimbal_app_id), getString(R.string.gimbal_app_secret));
		Proximity.startService(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.scan, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void serviceStarted() {
		Log.d("POH", "ScanActivity::serviceStarted()::enter");
		setProximityEnabled(true);
		Log.d("POH", "ScanActivity::serviceStarted()::exit");
	}

	@Override
	public void startServiceFailed(int errorCode, String errorMessage) {
		Log.d("POH", String.format("ScanActivity::startServiceFailed(%s,%s)::enter", errorCode, errorMessage));
		if (errorCode == ProximityError.PROXIMITY_BLUETOOTH_IS_OFF.getCode())
		{
			turnOnBlueTooth();
		}
		Log.d("POH", String.format("ScanActivity::startServiceFailed(%s,%s)::exit", errorCode, errorMessage));
	}

	private void turnOnBlueTooth()
	{
		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK)
		{
			Proximity.startService(this);
		}
		else
		{
			setProximityEnabled(false);
		}
	}

	@Override
	public void didArrive(Visit visit) {
		Log.d("POH", "ScanActivity::didArrive()::enter");

		logVisit(visit);

		Log.d("POH", "ScanActivity::didArrive()::exit");
	}

	private void logVisit(Visit visit) {
		Log.d("POH", "ScanActivity::logVisit()::enter");

		if (visit != null)
		{
			Log.d("POH", String.format("ScanActivity::logVisit::%s=%s", "       dwell time", visit.getDwellTime()));
			Log.d("POH", String.format("ScanActivity::logVisit::%s=%s", "last updated time", visit.getLastUpdateTime().toString()));
			Log.d("POH", String.format("ScanActivity::logVisit::%s=%s", "       start time", visit.getStartTime().toString()));
			logTransmitter(visit.getTransmitter());
		}

		Log.d("POH", "ScanActivity::logVisit()::exit");
	}

	private void logTransmitter(Transmitter transmitter) {
		Log.d("POH", "ScanActivity::logTransmitter()::enter");

		if (transmitter != null)
		{
			setQueryTerm(transmitter.getName());
			Log.d("POH", String.format("ScanActivity::logTransmitter::%s=%s", "identifier", transmitter.getIdentifier()));
			Log.d("POH", String.format("ScanActivity::logTransmitter::%s=%s", "      name", transmitter.getName()));
			Log.d("POH", String.format("ScanActivity::logTransmitter::%s=%s", "  owner id", transmitter.getOwnerId()));
		}

		Log.d("POH", "ScanActivity::logTransmitter()::exit");
	}

	private void setQueryTerm(String queryTerm) {
		this.queryTerm = queryTerm;
		idView.setText(queryTerm);
	}

	@Override
	public void receivedSighting(Visit visit, Date date, Integer integer) {
		Log.d("POH", "ScanActivity::receivedSighting()::enter");

		rssiView.setText(integer.toString());
		Log.d("POH", String.format("ScanActivity::receivedSighting::%s=%s", "   date", date.toString()));
		Log.d("POH", String.format("ScanActivity::receivedSighting::%s=%s", "integer", integer));
		logVisit(visit);
		if (integer > RSSI_THRESHOLD)
		{
			searchZoo(queryTerm);
		}
		Log.d("POH", "ScanActivity::receivedSighting()::exit");
	}

	private void searchZoo(String query) {
		if (TextUtils.isEmpty(query))
		{
			return;
		}
		setProximityEnabled(false);
		Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
		searchIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		searchIntent.putExtra(SearchManager.QUERY, query);
		searchIntent.putExtra(SearchManager.EXTRA_DATA_KEY, APP_SHOPPING_CATEGORY_VIEWPAGER_PAGE);
		startActivity(searchIntent);
		finish();
	}

	private void setProximityEnabled(boolean isEnabled)
	{
		if(isEnabled)
		{
			startVisitMgr();
		}
		else
		{
			stopVisitMgr();
		}
		proximityToggleView.setChecked(isEnabled);
	}

	private void stopVisitMgr() {
		Log.d("POH", "ScanActivity::stopVisitMgr()::enter");
		if (visitMgr != null)
		{
			visitMgr.setVisitListener(new VisitListener() {
				@Override
				public void didArrive(Visit visit) {

				}

				@Override
				public void receivedSighting(Visit visit, Date date, Integer integer) {

				}

				@Override
				public void didDepart(Visit visit) {

				}
			});
			// TODO: this causes a crash
			//visitMgr.stop();
		}
		setQueryTerm("");
		Log.d("POH", "ScanActivity::stopVisitMgr()::exit");
	}

	private void startVisitMgr()
	{
		Log.d("POH", "ScanActivity::startVisitMgr()::enter");
		if (visitMgr == null)
		{
			visitMgr = ProximityFactory.getInstance().createVisitManager();
		}
//		ProximityOptions options = new ProximityOptions();
//		options.setOption(ProximityOptions.VisitOptionArrivalRSSIKey, RSSI_THRESHOLD);
//		options.setOption(ProximityOptions.VisitOptionDepartureRSSIKey, RSSI_THRESHOLD);
//		options.setOption(ProximityOptions.VisitOptionBackgroundDepartureIntervalInSecondsKey, 5);
//		options.setOption(ProximityOptions.VisitOptionForegroundDepartureIntervalInSecondsKey, 5);
//		options.setOption(ProximityOptions.VisitOptionSignalStrengthWindowKey, ProximityOptions.VisitOptionSignalStrengthWindowSmall);
//		visitMgr.startWithOptions(options);
		visitMgr.setVisitListener(this);
		visitMgr.start();
		Log.d("POH", "ScanActivity::startVisitMgr()::exit");
	}

	@Override
	public void didDepart(Visit visit) {
		Log.d("POH", "ScanActivity::didDepart()::enter");

		logVisit(visit);

		Log.d("POH", "ScanActivity::didDepart()::exit");
	}

	public void searchZoo(View view) {
		searchZoo(queryTerm);
	}

	public void toggleProximityService(View view) {
		if (visitMgr == null)
		{
			return;
		}
		ToggleButton btn = (ToggleButton)view;
		setProximityEnabled(btn.isChecked());
	}
}
