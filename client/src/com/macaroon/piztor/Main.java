package com.macaroon.piztor;

import java.util.Timer;
import java.util.TimerTask;
import android.util.Log;
import android.util.AttributeSet;
import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.app.Activity;  
import android.content.res.Configuration;  
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;  
import android.widget.Toast;  
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.BMapManager;  
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.map.MKMapViewListener;  
import com.baidu.mapapi.map.MapController;  
import com.baidu.mapapi.map.MapPoi;  
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.PopupOverlay;
import com.baidu.platform.comapi.basestruct.GeoPoint; 

public class Main extends PiztorAct {

	/**
	 * Painting component
	 */
	BMapManager mBMapManager = null;
	MapView mMapView = null;
	private MapController mMapController = null;

	/**
	 * Locating component
	 */
	LocationClient mLocClient;
	LocationData locData = null;
	public MyLocationListener myListener = new MyLocationListener();

	/**
	 * My location layer
	 */
	locationOverlay myLocationOverlay = null;

	/**
	 * Request location button
	 */
	boolean isFirstLoc = true;

	final static int SearchButtonPress = 1;
	final static int FetchButtonPress = 2;
	final static int FocuseButtonPress = 3;
	final static int SuccessFetch = 4;
	final static int FailedFetch = 5;
	final static int TimerFlush = 6;
	ActMgr actMgr;
	ImageButton btnSearch, btnFetch, btnFocus, btnSettings;
	Timer autodate;

	@SuppressLint("HandlerLeak")
	Handler fromTransam = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case 1:
				ResUpdate update = (ResUpdate) m.obj;
				if (update.s == 0)
					System.out.println("update success");
				else {
					System.out.println("update failed");
					actMgr.trigger(AppMgr.errorToken);
				}
				break;
			case 2:
				ResLocation location = (ResLocation) m.obj;
				if (location.s == 0) {
					for (int i = 0; i < location.n; i++) {
						System.out.println(location.l.get(i).i + " : "
								+ location.l.get(i).lat + " "
								+ location.l.get(i).lot);
					}
					actMgr.trigger(SuccessFetch);
				} else {
					System.out
							.println("resquest for location must be wrong!!!");
					actMgr.trigger(AppMgr.errorToken);
				}
				break;
			case 3:
				ResUserinfo r = (ResUserinfo) m.obj;
				if (r.s == 0) {
					System.out.println("id : " + r.uid + " sex :  " + r.sex
							+ " group : " + r.gid);
				} else {
					System.out.println("reqest for userInfo must be wrong!!!");
					actMgr.trigger(AppMgr.errorToken);
				}
				break;
			default:
				break;
			}
		}
	};

	String cause(int t) {
		switch (t) {
		case SearchButtonPress:
			return "Search Button Press";
		case FetchButtonPress:
			return "Fetch Button Press";
		case FocuseButtonPress:
			return "Focuse Button Press";
		case SuccessFetch:
			return "Success Fetch";
		case FailedFetch:
			return "Failed Fetch";
		case TimerFlush:
			return "TimerFlush";
		default:
			return "Fuck!!!";
		}
	}

	// TODO flush map view
	void flushMap() {
	}

	class StartStatus extends ActStatus {

		@Override
		void enter(int e) {
			System.out.println("enter start status!!!!");
			if (e == ActMgr.Create) {
				AppMgr.transam.send(new ReqUserinfo(UserInfo.token,
						UserInfo.username, UserInfo.id, System
								.currentTimeMillis(), 5000));
			}

			if (e == TimerFlush) {
				ReqLocation r = new ReqLocation(UserInfo.token,
						UserInfo.username, 1, System.currentTimeMillis(), 1000);
				AppMgr.transam.send(r);
			}
			if (e == SuccessFetch)
				flushMap();
		}

		@Override
		void leave(int e) {
			System.out.println("leave start status!!!! because" + cause(e));
		}
	}

	class FetchStatus extends ActStatus {

		@Override
		void enter(int e) {
			System.out.println("enter Fetch status!!!!");
			if (e == FetchButtonPress) {
				ReqLocation r = new ReqLocation(UserInfo.token,
						UserInfo.username, 1, System.currentTimeMillis(), 1000);
				AppMgr.transam.send(r);
			}
		}

		@Override
		void leave(int e) {
			System.out.println("leave fetch status!!!! because" + cause(e));
		}
	}

	class FocusStatus extends ActStatus {

		@Override
		void enter(int e) {
			System.out.println("enter focus status!!!!");
		}

		@Override
		void leave(int e) {
			System.out.println("leave focus status!!!! because" + cause(e));
		}
	}

	class AutoUpdate extends TimerTask {

		@Override
		public void run() {
			actMgr.trigger(Main.TimerFlush);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	
		id = "Main";
		super.onCreate(savedInstanceState);
		
		//AppMgr.tracker.setHandler(fromGPS);
		ActStatus[] r = new ActStatus[3];
		r[0] = new StartStatus();
		r[1] = new FetchStatus();
		r[2] = new FocusStatus();
		actMgr = new ActMgr(this, r[0], r);
		actMgr.add(r[0], FocuseButtonPress, r[2]);
		actMgr.add(r[0], FetchButtonPress, r[1]);
		actMgr.add(r[0], SuccessFetch, r[0]);
		actMgr.add(r[1], FetchButtonPress, r[0]);
		actMgr.add(r[1], FailedFetch, r[0]);
		actMgr.add(r[1], SuccessFetch, r[0]);
		actMgr.add(r[2], FocuseButtonPress, r[0]);
		actMgr.add(r[0], TimerFlush, r[0]);
		actMgr.add(r[2], TimerFlush, r[2]);
		autodate = new Timer();
		AppMgr.transam.setHandler(fromTransam);	
		
		/*
		mBMapManager = new BMapManager(getApplicationContext());
		mBMapManager.init("8a0ae50048d103b2b8b12b7066f4ea7d",new MKGeneralListener(){
			@Override
			public void onGetNetworkState(int arg0) {
				System.out.println("Network !!!!!!     " + arg0);
			}
			@Override
			public void onGetPermissionState(int arg0) {
				System.out.println("Permission !!!!!!!     " +  arg0);
			}});
		
		mBMapManager.start();
		*/Log.d("你妹","xxxxxxxxx");
		setContentView(R.layout.activity_main);
		Log.d("你妹","xxxxxxxxx");
		/**
		 * Initialize MapView
		 */
		mMapView = (MapView)findViewById(R.id.bmapView);
		mMapController = mMapView.getController();
		mMapView.setBuiltInZoomControls(false);	
		GeoPoint point = new GeoPoint((int)(31.032247* 1E6),(int)(121.445937* 1E6));
		mMapController.setCenter(point);
		mMapController.setZoom(17);
		mMapController.setRotation(-22);

		mLocClient = new LocationClient(this);
		locData = new LocationData();
		mLocClient.registerLocationListener(myListener);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);
		option.setCoorType("bd09ll"); 
		option.setScanSpan(5000);
		mLocClient.setLocOption(option);
		mLocClient.start();
		myLocationOverlay = new locationOverlay(mMapView);
		myLocationOverlay.setData(locData);
		mMapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableCompass();
		mMapView.refresh();
	}


	public class MyLocationListener implements BDLocationListener {

		@Override
		public void onReceiveLocation(BDLocation location) {
			Log.d("GPS", "Gotten");
			if (location == null) {
				return;
			}

			locData.latitude = location.getLatitude();
			locData.longitude = location.getLongitude();

			locData.accuracy = location.getRadius();
			locData.direction = location.getDerect();

			myLocationOverlay.setData(locData);
			mMapView.refresh();

			if (isFirstLoc) {
				mMapController.animateTo(new GeoPoint((int)(locData.latitude * 1E6), (int)(locData.longitude * 1E6)));
			}
			isFirstLoc = false;
		}
		
		@Override
		public void onReceivePoi(BDLocation poiLocation) {
			if (poiLocation == null) {
				return;
			}
		}
	}

	public class locationOverlay extends MyLocationOverlay {

		public locationOverlay(MapView mapView) {
			super(mapView);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		btnFetch = (ImageButton) findViewById(R.id.footbar_btn_fetch);
		btnFocus = (ImageButton) findViewById(R.id.footbar_btn_focus);
		btnSearch = (ImageButton) findViewById(R.id.footbar_btn_search);
		btnSettings = (ImageButton) findViewById(R.id.footbar_btn_settings);
		btnFetch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				//	actMgr.trigger(FetchButtonPress);
			}
		});
		btnFocus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//	actMgr.trigger(FocuseButtonPress);
			}
		});
		btnSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				actMgr.trigger(AppMgr.toSettings);
			}
		});
		//autodate.schedule(new AutoUpdate(), 0, 5000);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			AppMgr.exit();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }
    
    @Override
    protected void onDestroy() {
    	//退出时销毁定位
        if (mLocClient != null)
            mLocClient.stop();
        mMapView.destroy();
        super.onDestroy();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return false;
	}

}