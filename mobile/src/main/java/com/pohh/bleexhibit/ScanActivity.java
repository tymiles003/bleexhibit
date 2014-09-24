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
import com.gimbal.proximity.Transmitter;
import com.gimbal.proximity.Visit;
import com.gimbal.proximity.VisitListener;
import com.gimbal.proximity.VisitManager;

import java.util.Date;


public class ScanActivity extends Activity implements ProximityListener, VisitListener {

	public static final int REQUEST_ENABLE_BT = 1;
	public static final int RSSI_THRESHOLD = -50;

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
		if (visitMgr == null) {
			visitMgr = ProximityFactory.getInstance().createVisitManager();
		}
		visitMgr.setVisitListener(this);
		visitMgr.start();
		proximityToggleView.setChecked(true);
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
			proximityToggleView.setChecked(false);
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
		}
		logTransmitter(visit.getTransmitter());

		Log.d("POH", "ScanActivity::logVisit()::exit");
	}

	private void logTransmitter(Transmitter transmitter) {
		Log.d("POH", "ScanActivity::logTransmitter()::enter");

		if (transmitter != null)
		{
			setQueryTerm(transmitter);
			Log.d("POH", String.format("ScanActivity::logTransmitter::%s=%s", "identifier", transmitter.getIdentifier()));
			Log.d("POH", String.format("ScanActivity::logTransmitter::%s=%s", "      name", transmitter.getName()));
			Log.d("POH", String.format("ScanActivity::logTransmitter::%s=%s", "  owner id", transmitter.getOwnerId()));
		}

		Log.d("POH", "ScanActivity::logTransmitter()::exit");
	}

	private void setQueryTerm(Transmitter transmitter) {
		queryTerm = transmitter.getName();
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
			searchZoo();
		}
		Log.d("POH", "ScanActivity::receivedSighting()::exit");
	}

	private void searchZoo() {
		if (TextUtils.isEmpty(queryTerm))
		{
			return;
		}
		visitMgr.stop();
		proximityToggleView.setChecked(false);
		Intent searchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
		searchIntent.putExtra(SearchManager.QUERY, queryTerm);
		searchIntent.putExtra(SearchManager.EXTRA_DATA_KEY, "web");
		startActivity(searchIntent);
		finish();
	}

	@Override
	public void didDepart(Visit visit) {
		Log.d("POH", "ScanActivity::didDepart()::enter");

		logVisit(visit);

		Log.d("POH", "ScanActivity::didDepart()::exit");
	}

	public void searchZoo(View view) {
		searchZoo();
	}

	public void toggleProximityService(View view) {
		if (visitMgr == null)
		{
			return;
		}
		ToggleButton btn = (ToggleButton)view;
		if (btn.isChecked())
		{
			visitMgr.start();
		}
		else
		{
			visitMgr.stop();
		}
	}
}
