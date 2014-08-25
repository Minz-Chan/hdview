package com.starnet.snview.realplay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.starnet.snview.R;
import com.starnet.snview.channelmanager.ChannelListActivity;
import com.starnet.snview.component.BaseActivity;
import com.starnet.snview.component.Toolbar;
import com.starnet.snview.component.VideoPager;
import com.starnet.snview.component.Toolbar.ActionImageButton;
import com.starnet.snview.component.VideoPager.ACTION;
import com.starnet.snview.component.liveview.LiveViewItemContainer;
import com.starnet.snview.component.liveview.LiveViewManager;
import com.starnet.snview.global.GlobalApplication;
import com.starnet.snview.protocol.Connection.StatusListener;
import com.starnet.snview.util.ActivityUtility;
import com.starnet.snview.util.ClickEventUtils;
import com.starnet.snview.util.PreviewItemXMLUtils;
import com.starnet.snview.util.ToastUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class RealplayActivity extends BaseActivity {

	private static final String TAG = "RealplayActivity";

	private Toolbar mToolbar;

	private VideoPager mPager;
	private LinearLayout mQualityControlbarMenu;
	private LinearLayout mPTZControlbarMenu;
	private LinearLayout mPTZPopFrame;
	
	private ImageButton mPTZPopFocalLengthIncrease;  
	private ImageButton mPTZPopFocalLengthDecrease;
	private ImageButton mPTZPopFocusIncrease;  
	private ImageButton mPTZPopFocusDecrease;
	private ImageButton mPTZPopApertureIncrease;  
	private ImageButton mPTZPopApertureDecrease;
	
	private PTZControl ptzControl;

//	private LiveViewItemContainer liveViewContainer1;
//	private LinearLayout mToolbarSubMenu;
//	private TextView mToolbarSubMenuText;

//	private LiveView mLiveView1;
//	private LiveView mLiveView2;
//	private LiveView mLiveView3;
//	private LiveView mLiveView4;
//	
//	
//	private int page = 1;
	
	private LiveViewManager liveViewManager;
	private FrameLayout mVideoRegion;
	
	
	
	

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle inState) {
		setContainerMenuDrawer(true);
		super.onCreate(inState);
		setContentView(R.layout.realplay_activity);
		
		setBackPressedExitEventValid(true);
		 

		GlobalApplication.getInstance().setScreenWidth(
				ActivityUtility.getScreenSize(this).x);

		initView();

		test();
		
		loadDataFromPreserved();
	}
	
	private void loadDataFromPreserved() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		List<PreviewDeviceItem> devices = null;
		
		devices = PreviewItemXMLUtils.getPreviewItemListInfoFromXML("/data/data/com.starnet.snview/LastDevicesList.xml");
		
		Log.i(TAG, "Devices size: " + devices.size());
		
		if (devices.size() != 0) {
			int mode = sharedPreferences.getInt("PREVIEW_MODE", -1);
			int page = sharedPreferences.getInt("PAGE", -1);
			int pageCount = sharedPreferences.getInt("PAGE_COUNT", -1);
			
			Log.i(TAG, "mode: " + mode + ", page: " + page);
			if (mode != -1 && page != -1) {
				liveViewManager.setDeviceList(devices);
				liveViewManager.setMultiMode(mode == 4 ? true : false);
				
				int newCurrPos = (page - 1) * liveViewManager.getPageCapacity() + 1;
				liveViewManager.setCurrenSelectedLiveViewtIndex(newCurrPos); 
				liveViewManager.selectLiveView(newCurrPos);
				
				liveViewManager.resetLiveView(pageCount);
				
				mPager.setAmount(devices.size());
				mPager.setNum(newCurrPos);
			}
			
		} else { // 首次进入
			liveViewManager.setMultiMode(null);
			mPager.setAmount(0);
			mPager.setNum(0);
		}

		
	}

	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		liveViewManager.invalidateLiveViews();

		super.onPostCreate(savedInstanceState);
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			
			
			
			super.handleMessage(msg);
		}
		
		
	};
	
	
	private void test() {
		Log.i(TAG, "In function test()");
		
		mVideoRegion = (FrameLayout) findViewById(R.id.video_region);
		liveViewManager = new LiveViewManager(this);
		ptzControl = new PTZControl(liveViewManager);

		
		// 视频控件点击事件实际处理方法
		ClickEventUtils.OnActionListener realVideoContainerClickListener = new ClickEventUtils.OnActionListener() {
	
			@Override
			public void OnAction(int clickCount, Object... params) {
				LiveViewItemContainer lvContainer = (LiveViewItemContainer) params[0];
				int pos = liveViewManager.getIndexOfLiveView(lvContainer);
				int index = (liveViewManager.getCurrentPageNumber() - 1) * liveViewManager.getPageCapacity() + pos;
				
				if (index > liveViewManager.getLiveViewTotalCount()) {  
					return;  // 非有效通道，不作处理 
				}
				
				liveViewManager.setCurrenSelectedLiveViewtIndex(index);  // 变更当前选择索引
				
				if (clickCount == ClickEventUtils.CLICK) { // 单击事件，视频选择
					liveViewManager.selectLiveView(index);
				} else if (clickCount == ClickEventUtils.DOUBLE_CLICK){ // 双击事件，视频模式切换
					liveViewManager.closeAllConnection(false);  // 关闭正在预览的设备
					
					if (liveViewManager.isMultiMode()) { // 切换到单通道模式
						liveViewManager.setMultiMode(false);							
						liveViewManager.preview(index);
					} else { // 切换到多通道模式
						liveViewManager.setMultiMode(true);
						
						int currPageStart = (liveViewManager.getCurrentPageNumber() - 1) * 4 + 1;
						int currPageEnd = (liveViewManager.getCurrentPageNumber() - 1) * 4 + liveViewManager.getCurrentPageCount();
						
						liveViewManager.preview(currPageStart, currPageEnd - currPageStart + 1);
						
					}
					
					liveViewManager.selectLiveView(index);					
				}
				
				mPager.setNum(liveViewManager.getSelectedLiveViewIndex());
				mPager.setAmount(liveViewManager.getLiveViewTotalCount());
				
				
				//currentSelected = index;  // 保存当前视频索引
			}
		};
		
		final ClickEventUtils clickUtils = new ClickEventUtils(realVideoContainerClickListener);
		
		// 视频控件点击事件
		final LiveViewItemContainer.OnLiveViewContainerClickListener onLiveViewContainerClickListener 
			= new LiveViewItemContainer.OnLiveViewContainerClickListener() {

			@Override
			public void onClick(View v) {
				if (liveViewManager.getPager() == null) {
					return;
				}
				
				int pos = liveViewManager.getIndexOfLiveView((LiveViewItemContainer)v);
				clickUtils.makeContinuousClickCalledOnce(pos, v);
			}
		};
		
		final LiveViewItemContainer.OnRefreshButtonClickListener onRefreshButtonClickListener
			= new LiveViewItemContainer.OnRefreshButtonClickListener() {
				
			@Override
			public void onClick(View v) {
				if (liveViewManager.getPager() == null) {
					return;
				}
				
				LiveViewItemContainer c = findVideoContainerByView(v);
				
				if (c != null) {
					int pos = liveViewManager.getIndexOfLiveView(c);
					int page = liveViewManager.getCurrentPageNumber();
					int index = (page - 1 ) * liveViewManager.getPageCapacity() + pos;
					
					liveViewManager.tryPreview(index);
				}
				
			}
		};
		
		final LiveViewItemContainer.OnGestureListener onGestureListener 
			= new LiveViewItemContainer.OnGestureListener() {
			
			@Override
			public void onSlidingLeft() {
				if (liveViewManager.getPager() != null) {
					liveViewManager.nextPage();
					
					mPager.setNum(liveViewManager.getSelectedLiveViewIndex());
					mPager.setAmount(liveViewManager.getLiveViewTotalCount());
				}
			}
			
			@Override
			public void onSlidingRight() {
				if (liveViewManager.getPager() != null) {
					liveViewManager.previousPage();
					
					mPager.setNum(liveViewManager.getSelectedLiveViewIndex());
					mPager.setAmount(liveViewManager.getLiveViewTotalCount());
				}
				
			}
			
		};
		
		

		// 初始化视频区域布局大小
		final int screenWidth = GlobalApplication.getInstance().getScreenWidth();
		RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(screenWidth, screenWidth);
		
		mVideoRegion.setLayoutParams(param);
		
		// 单通道/多通道模式切换 事件
		LiveViewManager.OnVideoModeChangedListener onVideoModeChangedListener
			= new LiveViewManager.OnVideoModeChangedListener() {

				@Override
				public void OnVideoModeChanged(boolean isMultiMode) {
					mVideoRegion.removeAllViews();
					liveViewManager.clearLiveView();
					
					Log.i(TAG, "VideoRegion, width: " + mVideoRegion.getWidth() + ", height: " + mVideoRegion.getHeight());
					
					List<PreviewDeviceItem> devices = liveViewManager.getDeviceList();
					if (devices == null || devices.size() == 0) {
						return;
					}
					
					
					if (isMultiMode) { // 多通道模式
						((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
								.inflate(R.layout.surfaceview_multi_layout,
										mVideoRegion, true);
						
						ViewFlipper flipper = (ViewFlipper) findViewById(R.id.viewflipperContainer);
						
						
						LinearLayout linear1 = (LinearLayout) findViewById(R.id.view_video_linear1);
						LinearLayout linear2 = (LinearLayout) findViewById(R.id.view_video_linear2);
						LinearLayout.LayoutParams param1 = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, screenWidth / 2);
						
						linear1.setLayoutParams(param1);
						linear2.setLayoutParams(param1);
						
						int surfaceHeight = (int) (screenWidth / 2.0 - getResources().getDimension(R.dimen.window_text_height) 
								- 2 * getResources().getDimension(R.dimen.surface_container_space));
						
						LiveViewItemContainer liveview1 = (LiveViewItemContainer) findViewById(R.id.liveview_liveitem1);
						LiveViewItemContainer liveview2 = (LiveViewItemContainer) findViewById(R.id.liveview_liveitem2);
						LiveViewItemContainer liveview3 = (LiveViewItemContainer) findViewById(R.id.liveview_liveitem3);
						LiveViewItemContainer liveview4 = (LiveViewItemContainer) findViewById(R.id.liveview_liveitem4);
						
						liveview1.setLiveViewContainerClickListener(onLiveViewContainerClickListener);
						liveview1.setRefreshButtonClickListener(onRefreshButtonClickListener);
						liveview1.setGestureListener(onGestureListener);
						liveview1.findSubViews();
						liveview1.init();
						//liveview1.getSurfaceView().setBackgroundColor(Color.RED);
						liveview1.getPlaywindowFrame().setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
								surfaceHeight));
						liveview1.setFlipper(flipper);
						
						liveview2.setLiveViewContainerClickListener(onLiveViewContainerClickListener);
						liveview2.setRefreshButtonClickListener(onRefreshButtonClickListener);
						liveview2.setGestureListener(onGestureListener);
						liveview2.findSubViews();
						liveview2.init();
						//liveview2.getSurfaceView().setBackgroundColor(Color.YELLOW);
						liveview2.getPlaywindowFrame().setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
								surfaceHeight));
						liveview2.setFlipper(flipper);
						
						liveview3.setLiveViewContainerClickListener(onLiveViewContainerClickListener);
						liveview3.setRefreshButtonClickListener(onRefreshButtonClickListener);
						liveview3.setGestureListener(onGestureListener);
						liveview3.findSubViews();
						liveview3.init();
						//liveview3.getSurfaceView().setBackgroundColor(Color.WHITE);
						liveview3.getPlaywindowFrame().setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
								surfaceHeight));
						liveview3.setFlipper(flipper);
						
						liveview4.setLiveViewContainerClickListener(onLiveViewContainerClickListener);
						liveview4.setRefreshButtonClickListener(onRefreshButtonClickListener);
						liveview4.setGestureListener(onGestureListener);
						liveview4.findSubViews();
						liveview4.init();
						//liveview4.getSurfaceView().setBackgroundColor(Color.GREEN);
						liveview4.getPlaywindowFrame().setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 
								surfaceHeight));
						liveview4.setFlipper(flipper);
						
						liveViewManager.addLiveView(liveview1);
						liveViewManager.addLiveView(liveview2);
						liveViewManager.addLiveView(liveview3);
						liveViewManager.addLiveView(liveview4);						
						
					} else { // 单通道模式
						((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.surfaceview_single_layout,
								mVideoRegion, true);
						
						ViewFlipper flipper = (ViewFlipper) findViewById(R.id.viewflipperContainer);
						
						int surfaceHeight = (int) (screenWidth - getResources().getDimension(R.dimen.window_text_height) 
								- 2 * getResources().getDimension(R.dimen.surface_container_space));
						
						LiveViewItemContainer liveview = (LiveViewItemContainer) findViewById(R.id.liveview_liveitem);
						
						liveview.setLiveViewContainerClickListener(onLiveViewContainerClickListener);
						liveview.setRefreshButtonClickListener(onRefreshButtonClickListener);
						liveview.setGestureListener(onGestureListener);
						liveview.findSubViews();
						liveview.init();
						//liveview.getSurfaceView().setBackgroundColor(Color.BLUE);
						liveview.getPlaywindowFrame().setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 
								surfaceHeight));
						liveview.setFlipper(flipper);
						
						liveViewManager.addLiveView(liveview);
					}
					
					onContentChanged();
					
				}
			
		};
		
		liveViewManager.setOnVideoModeChangedListener(onVideoModeChangedListener);
		
		// 初始化为多通道模式
		//onVideoModeChangedListener.OnVideoModeChanged(true);
		
		
				
				
		
		
		final StatusListener connectionStatusListener = new StatusListener() {

			@Override
			public void OnConnectionTrying(View v) {
				final LiveViewItemContainer c = (LiveViewItemContainer) v;
				
				c.setWindowInfoContent(getString(R.string.connection_status_connecting));
				
				//updateProgressbarStatus(c.getProgressBar(), true);
				mHandler.post( new Runnable() {
					@Override
					public void run() {
						if (c != null) {
							c.getRefreshImageView().setVisibility(View.GONE);
							c.getProgressBar().setVisibility(View.VISIBLE);
							c.getRefreshImageView().setVisibility(View.INVISIBLE); // 若使用View.GONE会导致部分情况下ProgressBar消失
							
							Log.i(TAG, "ProgressBar@" + c.getProgressBar() + ", visible");
						}
					}
				});
						
			}

			@Override
			public void OnConnectionFailed(View v) {
				Log.i(TAG, "OnConnectionFailed");
				final LiveViewItemContainer c = (LiveViewItemContainer) v;
				
				c.setWindowInfoContent(getString(R.string.connection_status_failed));
				
				mHandler.post( new Runnable() {
					@Override
					public void run() {
						if (c != null) {
							c.getProgressBar().setVisibility(View.INVISIBLE);
							c.getRefreshImageView().setVisibility(View.VISIBLE);
							
						}
					}
				});
				
			}

			@Override
			public void OnConnectionEstablished(View v) {
				final LiveViewItemContainer c = (LiveViewItemContainer) v;
				
				c.setWindowInfoContent(getString(R.string.connection_status_established));				
			}
			
			@Override
			public void OnConnectionBusy(View v) {
				final LiveViewItemContainer c = (LiveViewItemContainer) v;
				
				//updateProgressbarStatus(c.getProgressBar(), false);
				mHandler.post( new Runnable() {
					@Override
					public void run() {
						if (c != null) {
							c.getProgressBar().setVisibility(View.INVISIBLE);	
							c.getRefreshImageView().setVisibility(View.GONE);
							Log.i(TAG, "ProgressBar@" + c.getProgressBar() + ", invisible");
						}
					}
				});
				
			}

			@Override
			public void OnConnectionClosed(View v) {
				final LiveViewItemContainer c = (LiveViewItemContainer) v;
				int currPageCount = liveViewManager.getCurrentPageCount();
				int index = liveViewManager.getIndexOfLiveView(c);
				
				if (index > currPageCount) {
					return;
				}
				
				c.setWindowInfoContent(getString(R.string.connection_status_closed));
				
				mHandler.post( new Runnable() {
					@Override
					public void run() {
						if (c != null) {
							c.getProgressBar().setVisibility(View.INVISIBLE);
							c.getRefreshImageView().setVisibility(View.VISIBLE);
							
						}
					}
				});
				
			}

		};
		
		liveViewManager.setConnectionStatusListener(connectionStatusListener);
		
		
	}
	
	private LiveViewItemContainer findVideoContainerByView(View v) {
		View curr = v;
		
		while (curr != null) {
			if (curr instanceof LiveViewItemContainer) {
				break;
			}
			
			curr = (View) curr.getParent();
		}
		
		return (LiveViewItemContainer) curr;
	}


	private void initView() {
		Button deviceList = super.getRightButton();

		deviceList.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(RealplayActivity.this,
						ChannelListActivity.class);
				RealplayActivity.this.startActivityForResult(intent, 0);
				
				if (liveViewManager != null) {
					liveViewManager.stopPreview();
				}
			}
		});

		initToolbar();

		initToolbarExtendMenu();

		initToolbarSubMenu();
		
		
	}
	
	
	public FrameLayout getVideoRegion() {
		return mVideoRegion;
	}

	private VideoPager.OnActionClickListener mPagerOnActionClickListener = new VideoPager.OnActionClickListener() {
		@Override
		public void OnActionClick(View v, ACTION action) {
			if (liveViewManager.getPager() == null) {
				return;
			}
			
			switch (action) {
			case PREVIOUS:
				liveViewManager.previousPage();				
				break;
			case NEXT:
				liveViewManager.nextPage();
				break;
			default:
				break;
			}

			mPager.setNum(liveViewManager.getSelectedLiveViewIndex());
			mPager.setAmount(liveViewManager.getLiveViewTotalCount());
		}
	};

	private boolean bIsPlaying = false;
	private boolean bQualityPressed = false;
	private boolean bPTZPressed = false;
	private boolean bIsMicrophoneOpen = false;
	private boolean bIsSoundOpen = false;
	private boolean bVideoRecordPressed = false;
	private Toolbar.OnItemClickListener mToolbarOnItemClickListener = new Toolbar.OnItemClickListener() {

		@Override
		public void onItemClick(ActionImageButton imgBtn) {

			switch (imgBtn.getItemData().getActionID()) {
			case PLAY_PAUSE:
				if (!bIsPlaying) { // 播放
					bIsPlaying = true;
					mToolbar.setActionImageButtonBg(
							Toolbar.ACTION_ENUM.PLAY_PAUSE,
							R.drawable.toolbar_pause_selector);
				} else { // 暂停
					bIsPlaying = false;
					mToolbar.setActionImageButtonBg(
							Toolbar.ACTION_ENUM.PLAY_PAUSE,
							R.drawable.toolbar_play_selector);
				}

				break;
			case PICTURE:
				Toast.makeText(
						getBaseContext(),
						"Width: " + mPager.getPrevious().getWidth()
								+ ", height: "
								+ mPager.getPrevious().getHeight(),
						Toast.LENGTH_LONG).show();
				break;
			case QUALITY:
				bQualityPressed = !bQualityPressed;

				mPTZPopFrame.setVisibility(View.GONE);
				showPTZFrame(PTZ_POP_FRAME.SCAN, false);

				if (bQualityPressed) {
					showToolbarExtendMenu(TOOLBAR_EXTEND_MENU.MENU_QUALITY);
				} else {
					showToolbarExtendMenu(TOOLBAR_EXTEND_MENU.PAGER);
				}

				break;
			case PTZ:
				bPTZPressed = !bPTZPressed;

				mPTZPopFrame.setVisibility(View.GONE);
				showPTZFrame(PTZ_POP_FRAME.SCAN, false);
				mPTZMenuScan.setSelected(false);
				mPTZMenuFocalLength.setSelected(false);
				mPTZMenuFocus.setSelected(false);
				mPTZMenuAperture.setSelected(false);
				mPTZMenuPreset.setSelected(false);

				if (bPTZPressed) {
					showToolbarExtendMenu(TOOLBAR_EXTEND_MENU.MENU_PTZ);
				} else {
					showToolbarExtendMenu(TOOLBAR_EXTEND_MENU.PAGER);
				}
				break;
			case MICROPHONE:
				if (!bIsMicrophoneOpen) { // 开启麦克风
					bIsMicrophoneOpen = true;
					mToolbar.setActionImageButtonBg(
							Toolbar.ACTION_ENUM.MICROPHONE,
							R.drawable.toolbar_microphone_selector);
				} else { // 关闭麦克风
					bIsMicrophoneOpen = false;
					mToolbar.setActionImageButtonBg(
							Toolbar.ACTION_ENUM.MICROPHONE,
							R.drawable.toolbar_microphone_stop_selector);
				}

				break;
			case SOUND:
				if (!bIsSoundOpen) { // 开启扬声器
					bIsSoundOpen = true;
					mToolbar.setActionImageButtonBg(Toolbar.ACTION_ENUM.SOUND,
							R.drawable.toolbar_sound_selector);
				} else { // 关闭扬声器
					bIsSoundOpen = false;
					mToolbar.setActionImageButtonBg(Toolbar.ACTION_ENUM.SOUND,
							R.drawable.toolbar_sound_off_selector);
				}
				break;
			case VIDEO_RECORD:
				mToolbar.setActionImageButtonSelected(
						Toolbar.ACTION_ENUM.VIDEO_RECORD, !bVideoRecordPressed);
				bVideoRecordPressed = !bVideoRecordPressed;
				break;
			case ALARM:
				break;
			default:
				break;
			}
		}
	};

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initToolbar() {
		mToolbar = super.getBaseToolbar();

		ArrayList itemList = new ArrayList();
		itemList.add(new Toolbar.ItemData(Toolbar.ACTION_ENUM.PLAY_PAUSE,
				R.drawable.toolbar_play_selector));
		itemList.add(new Toolbar.ItemData(Toolbar.ACTION_ENUM.PICTURE,
				R.drawable.toolbar_take_picture_selector));
		itemList.add(new Toolbar.ItemData(Toolbar.ACTION_ENUM.QUALITY,
				R.drawable.toolbar_quality_high_selector));
		itemList.add(new Toolbar.ItemData(Toolbar.ACTION_ENUM.PTZ,
				R.drawable.toolbar_ptz_selector));
		itemList.add(new Toolbar.ItemData(Toolbar.ACTION_ENUM.MICROPHONE,
				R.drawable.toolbar_microphone_stop_selector));
		itemList.add(new Toolbar.ItemData(Toolbar.ACTION_ENUM.SOUND,
				R.drawable.toolbar_sound_off_selector));
		itemList.add(new Toolbar.ItemData(Toolbar.ACTION_ENUM.VIDEO_RECORD,
				R.drawable.toolbar_video_record_selector));
		itemList.add(new Toolbar.ItemData(Toolbar.ACTION_ENUM.ALARM,
				R.drawable.toolbar_alarm_selector));

		mToolbar.createToolbar(itemList, GlobalApplication.getInstance()
				.getScreenWidth(),
				getResources().getDimensionPixelSize(R.dimen.toolbar_height));

		this.mToolbar.setOnItemClickListener(mToolbarOnItemClickListener);
	}

	private enum TOOLBAR_EXTEND_MENU {
		PAGER, MENU_QUALITY, MENU_PTZ
	}

	private ImageButton mQualityMenuFluency;
	private ImageButton mQualityMenuStandard;
	private ImageButton mQualityMenuHigh;
	private ImageButton mQualityMenuCustom;
	private ImageButton mPTZMenuScan;
	private ImageButton mPTZMenuFocalLength;
	private ImageButton mPTZMenuFocus;
	private ImageButton mPTZMenuAperture;
	private ImageButton mPTZMenuPreset;

	private OnClickListener mOnQualityMenuClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.quality_controlbar_menu_fluency:
				mQualityMenuFluency.setSelected(true);
				mQualityMenuStandard.setSelected(false);
				mQualityMenuHigh.setSelected(false);
				mQualityMenuCustom.setSelected(false);
				mToolbar.setActionImageButtonBg(Toolbar.ACTION_ENUM.QUALITY,
						R.drawable.toolbar_quality_fluency_selector);
				break;
			case R.id.quality_controlbar_menu_standard:
				mQualityMenuFluency.setSelected(false);
				mQualityMenuStandard.setSelected(true);
				mQualityMenuHigh.setSelected(false);
				mQualityMenuCustom.setSelected(false);
				mToolbar.setActionImageButtonBg(Toolbar.ACTION_ENUM.QUALITY,
						R.drawable.toolbar_quality_standard_selector);
				break;
			case R.id.quality_controlbar_menu_high:
				mQualityMenuFluency.setSelected(false);
				mQualityMenuStandard.setSelected(false);
				mQualityMenuHigh.setSelected(true);
				mQualityMenuCustom.setSelected(false);
				mToolbar.setActionImageButtonBg(Toolbar.ACTION_ENUM.QUALITY,
						R.drawable.toolbar_quality_high_selector);
				break;
			case R.id.quality_controlbar_menu_custom:
				mQualityMenuFluency.setSelected(false);
				mQualityMenuStandard.setSelected(false);
				mQualityMenuHigh.setSelected(false);
				mQualityMenuCustom.setSelected(true);
				mToolbar.setActionImageButtonBg(Toolbar.ACTION_ENUM.QUALITY,
						R.drawable.toolbar_quality_custom_selector);
				break;
			default:
				break;
			}

			mToolbar.setActionItemSelected(Toolbar.ACTION_ENUM.QUALITY, true);
		}

	};

	private enum PTZ_POP_FRAME {
		SCAN, FOCAL_LENGTH, FOCUS, APERTURE, PRESET
	};

	private OnClickListener mOnPTZMenuClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {

			switch (v.getId()) {
			case R.id.ptz_controlbar_menu_scan:
				break;
			case R.id.ptz_controlbar_menu_focal_length:
				if (!mPTZMenuFocalLength.isSelected()) {
					// mToolbarSubMenu.setVisibility(View.VISIBLE);
					// mToolbarSubMenuText.setText(getString(R.string.toolbar_sub_menu_focal_length));
					showPTZFrame(PTZ_POP_FRAME.FOCAL_LENGTH, true);
					mPTZMenuScan.setSelected(false);
					mPTZMenuFocalLength.setSelected(true);
					mPTZMenuFocus.setSelected(false);
					mPTZMenuAperture.setSelected(false);
					mPTZMenuPreset.setSelected(false);
				} else {
					// mToolbarSubMenu.setVisibility(View.GONE);
					showPTZFrame(PTZ_POP_FRAME.FOCAL_LENGTH, false);
					mPTZMenuFocalLength.setSelected(false);
				}
				break;
			case R.id.ptz_controlbar_menu_focus:
				if (!mPTZMenuFocus.isSelected()) {
					// mToolbarSubMenu.setVisibility(View.VISIBLE);
					// mToolbarSubMenuText.setText(getString(R.string.toolbar_sub_menu_focus));
					showPTZFrame(PTZ_POP_FRAME.FOCUS, true);
					mPTZMenuScan.setSelected(false);
					mPTZMenuFocalLength.setSelected(false);
					mPTZMenuFocus.setSelected(true);
					mPTZMenuAperture.setSelected(false);
					mPTZMenuPreset.setSelected(false);
				} else {
					// mToolbarSubMenu.setVisibility(View.GONE);
					showPTZFrame(PTZ_POP_FRAME.FOCUS, false);
					mPTZMenuFocus.setSelected(false);
				}
				break;
			case R.id.ptz_controlbar_menu_aperture:
				if (!mPTZMenuAperture.isSelected()) {
					// mToolbarSubMenu.setVisibility(View.VISIBLE);
					// mToolbarSubMenuText.setText(getString(R.string.toolbar_sub_menu_aperture));
					showPTZFrame(PTZ_POP_FRAME.APERTURE, true);
					mPTZMenuScan.setSelected(false);
					mPTZMenuFocalLength.setSelected(false);
					mPTZMenuFocus.setSelected(false);
					mPTZMenuAperture.setSelected(true);
					mPTZMenuPreset.setSelected(false);
				} else {
					// mToolbarSubMenu.setVisibility(View.GONE);
					showPTZFrame(PTZ_POP_FRAME.APERTURE, false);
					mPTZMenuAperture.setSelected(false);
				}
				break;
			case R.id.ptz_controlbar_menu_preset:
				break;
			}

		}
	};
	
	private OnClickListener mOnPTZPopClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			
			switch (v.getId()) {
			case R.id.ptz_pop_focal_length_increase:
				Log.i(TAG, "ptz_pop_focal_length_increase");
				ptzControl.focalLengthIncrease();
				break;
			case R.id.ptz_pop_focal_length_decrease:
				Log.i(TAG, "ptz_pop_focal_length_decrease");
				ptzControl.focalLengthDecrease();
				break;
			case R.id.ptz_pop_focus_increase:
				Log.i(TAG, "ptz_pop_focus_increase");
				ptzControl.focusIncrease();
				break;
			case R.id.ptz_pop_focus_decrease:
				Log.i(TAG, "ptz_pop_focus_decrease");
				ptzControl.focusDecrease();
				break;
			case R.id.ptz_pop_aperture_increase:
				Log.i(TAG, "ptz_pop_aperture_increase");
				ptzControl.apertureIncrease();
				break;
			case R.id.ptz_pop_aperture_decrease:
				Log.i(TAG, "ptz_pop_aperture_decrease");
				ptzControl.apertureDecrease();
				break;
			}
			
		}
		
	};

	private void showPTZFrame(PTZ_POP_FRAME ppf, boolean isShow) {
		if (isShow) {
			switch (ppf) {
			case SCAN:
				break;
			case FOCAL_LENGTH:
				((LinearLayout) findViewById(R.id.ptz_pop_focal_length_frame))
						.setVisibility(View.VISIBLE);
				((LinearLayout) findViewById(R.id.ptz_pop_focus_frame))
						.setVisibility(View.GONE);
				((LinearLayout) findViewById(R.id.ptz_pop_aperture_frame))
						.setVisibility(View.GONE);
				break;
			case FOCUS:
				((LinearLayout) findViewById(R.id.ptz_pop_focal_length_frame))
						.setVisibility(View.GONE);
				((LinearLayout) findViewById(R.id.ptz_pop_focus_frame))
						.setVisibility(View.VISIBLE);
				((LinearLayout) findViewById(R.id.ptz_pop_aperture_frame))
						.setVisibility(View.GONE);
				break;
			case APERTURE:
				((LinearLayout) findViewById(R.id.ptz_pop_focal_length_frame))
						.setVisibility(View.GONE);
				((LinearLayout) findViewById(R.id.ptz_pop_focus_frame))
						.setVisibility(View.GONE);
				((LinearLayout) findViewById(R.id.ptz_pop_aperture_frame))
						.setVisibility(View.VISIBLE);
				break;
			case PRESET:
				break;
			}
		} else {
			((LinearLayout) findViewById(R.id.ptz_pop_focal_length_frame))
					.setVisibility(View.GONE);
			((LinearLayout) findViewById(R.id.ptz_pop_focus_frame))
					.setVisibility(View.GONE);
			((LinearLayout) findViewById(R.id.ptz_pop_aperture_frame))
					.setVisibility(View.GONE);
		}
	}

	private void initToolbarExtendMenu() {
		mPager = (VideoPager) findViewById(R.id.pager);
		mPager.initContent(ActivityUtility.getScreenSize(this).x,
				getResources().getDimensionPixelSize(R.dimen.toolbar_height));
		mPager.setOnActionClickListener(mPagerOnActionClickListener);

		mQualityControlbarMenu = (LinearLayout) findViewById(R.id.quality_controlbar_menu);
		mQualityMenuFluency = (ImageButton) findViewById(R.id.quality_controlbar_menu_fluency);
		mQualityMenuStandard = (ImageButton) findViewById(R.id.quality_controlbar_menu_standard);
		mQualityMenuHigh = (ImageButton) findViewById(R.id.quality_controlbar_menu_high);
		mQualityMenuCustom = (ImageButton) findViewById(R.id.quality_controlbar_menu_custom);

		mQualityMenuFluency.setOnClickListener(mOnQualityMenuClickListener);
		mQualityMenuStandard.setOnClickListener(mOnQualityMenuClickListener);
		mQualityMenuHigh.setOnClickListener(mOnQualityMenuClickListener);
		mQualityMenuCustom.setOnClickListener(mOnQualityMenuClickListener);

		mPTZControlbarMenu = (LinearLayout) findViewById(R.id.ptz_controlbar_menu);
		mPTZMenuScan = (ImageButton) findViewById(R.id.ptz_controlbar_menu_scan);
		mPTZMenuFocalLength = (ImageButton) findViewById(R.id.ptz_controlbar_menu_focal_length);
		mPTZMenuFocus = (ImageButton) findViewById(R.id.ptz_controlbar_menu_focus);
		mPTZMenuAperture = (ImageButton) findViewById(R.id.ptz_controlbar_menu_aperture);
		mPTZMenuPreset = (ImageButton) findViewById(R.id.ptz_controlbar_menu_preset);

		mPTZMenuScan.setOnClickListener(mOnPTZMenuClickListener);
		mPTZMenuFocalLength.setOnClickListener(mOnPTZMenuClickListener);
		mPTZMenuFocus.setOnClickListener(mOnPTZMenuClickListener);
		mPTZMenuAperture.setOnClickListener(mOnPTZMenuClickListener);
		mPTZMenuPreset.setOnClickListener(mOnPTZMenuClickListener);
		
		
		mPTZPopFocalLengthIncrease = (ImageButton) findViewById(R.id.ptz_pop_focal_length_increase);  
		mPTZPopFocalLengthDecrease = (ImageButton) findViewById(R.id.ptz_pop_focal_length_decrease);
		mPTZPopFocusIncrease = (ImageButton) findViewById(R.id.ptz_pop_focus_increase);  
		mPTZPopFocusDecrease = (ImageButton) findViewById(R.id.ptz_pop_focus_decrease);
		mPTZPopApertureIncrease = (ImageButton) findViewById(R.id.ptz_pop_aperture_increase);  
		mPTZPopApertureDecrease = (ImageButton) findViewById(R.id.ptz_pop_aperture_decrease);
		
		mPTZPopFocalLengthIncrease.setOnClickListener(mOnPTZPopClickListener);  
		mPTZPopFocalLengthDecrease.setOnClickListener(mOnPTZPopClickListener);
		mPTZPopFocusIncrease.setOnClickListener(mOnPTZPopClickListener);  
		mPTZPopFocusDecrease.setOnClickListener(mOnPTZPopClickListener);
		mPTZPopApertureIncrease.setOnClickListener(mOnPTZPopClickListener);  
		mPTZPopApertureDecrease.setOnClickListener(mOnPTZPopClickListener);
		

		mPTZPopFrame = (LinearLayout) findViewById(R.id.ptz_pop_frame);

		showToolbarExtendMenu(TOOLBAR_EXTEND_MENU.PAGER);
	}

	private void initToolbarSubMenu() {
		// mToolbarSubMenu = (LinearLayout) findViewById(R.id.toolbar_sub_menu);
		// mToolbarSubMenuText = (TextView)
		// findViewById(R.id.base_toolbar_sub_menu_text);

		// int menuWidth = (int)
		// (GlobalApplication.getInstance().getScreenWidth() / 5 * 2);
		// int menuHeight = getResources().getDimensionPixelSize(
		// R.dimen.toolbar_height);
		// LinearLayout.LayoutParams subMenuLayout = new
		// LinearLayout.LayoutParams(
		// menuWidth, menuHeight);
		// mToolbarSubMenu.setLayoutParams(subMenuLayout);
	}

	private void showToolbarExtendMenu(TOOLBAR_EXTEND_MENU menuId) {

		switch (menuId) {
		case PAGER:
			mPager.setVisibility(View.VISIBLE);
			mQualityControlbarMenu.setVisibility(View.GONE);
			mPTZControlbarMenu.setVisibility(View.GONE);

			mToolbar.setActionItemSelected(Toolbar.ACTION_ENUM.QUALITY, false);
			mToolbar.setActionItemSelected(Toolbar.ACTION_ENUM.PTZ, false);

			bQualityPressed = false;
			bPTZPressed = false;
			break;
		case MENU_QUALITY:
			mPager.setVisibility(View.GONE);
			mQualityControlbarMenu.setVisibility(View.VISIBLE);
			mPTZControlbarMenu.setVisibility(View.GONE);

			mToolbar.setActionItemSelected(Toolbar.ACTION_ENUM.QUALITY, true);
			mToolbar.setActionItemSelected(Toolbar.ACTION_ENUM.PTZ, false);

			bQualityPressed = true;
			bPTZPressed = false;
			break;
		case MENU_PTZ:
			mPager.setVisibility(View.GONE);
			mQualityControlbarMenu.setVisibility(View.GONE);
			mPTZControlbarMenu.setVisibility(View.VISIBLE);

			mPTZPopFrame.setVisibility(View.VISIBLE);

			mToolbar.setActionItemSelected(Toolbar.ACTION_ENUM.QUALITY, false);
			mToolbar.setActionItemSelected(Toolbar.ACTION_ENUM.PTZ, true);

			bQualityPressed = false;
			bPTZPressed = true;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		switch (resultCode) {
		case 8:
//			PreviewDeviceItem p = (PreviewDeviceItem) data.getExtras().get(
//					"DEVICE_ITEM");			
//			List<PreviewDeviceItem> devices = new ArrayList<PreviewDeviceItem>();
//			devices.add(p);
			
			Parcelable[] _devices = (Parcelable[]) data.getExtras().get("DEVICE_ITEM_LIST");
			
			List<PreviewDeviceItem> devices = new ArrayList<PreviewDeviceItem>();
			
			for (int i = 0; i < _devices.length; i++) {
				devices.add((PreviewDeviceItem)_devices[i]);
			}
			
			liveViewManager.setDeviceList(devices);
			
			mPager.setNum(liveViewManager.getSelectedLiveViewIndex());
			mPager.setAmount(liveViewManager.getLiveViewTotalCount());
			
			liveViewManager.preview();
			
			liveViewManager.selectLiveView(liveViewManager.getSelectedLiveViewIndex());
			
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	

	

	@Override
	protected void onRestart() {
		setActiveMenuId(R.id.menu_drawer_realtime_preview);
		super.onRestart();
	}
	
	

	@Override
	protected void onStart() {
		setActiveMenuId(R.id.menu_drawer_realtime_preview);
		super.onStart();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setActiveMenuId(R.id.menu_drawer_realtime_preview);
		
		super.onNewIntent(intent);
	}

	@Override
	protected void onDestroy() {
		liveViewManager.closeAllConnection(false);
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = sharedPreferences.edit();
		boolean isMultiMode = liveViewManager.isMultiMode(); 
		
		if (isMultiMode) {
			editor.putInt("PREVIEW_MODE", 4);  // 当前预览模式
		} else {
			editor.putInt("PREVIEW_MODE", 1);  
		}

		editor.putInt("PAGE", liveViewManager.getCurrentPageNumber());       // 当前页数
		editor.putInt("PAGE_COUNT", liveViewManager.getCurrentPageCount());  // 当前页项数
		editor.commit();
		
		// /data/data/com.starsecurity
		try {
			PreviewItemXMLUtils.writePreviewItemListInfoToXML(liveViewManager.getDeviceList(), "/data/data/com.starnet.snview/LastDevicesList.xml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Log.i(TAG, "onDestroy@Devices size: " + liveViewManager.getDeviceList().size() + ", mode: " + (isMultiMode ? 4 : 1) 
				+ ", page: " + liveViewManager.getCurrentPageNumber() + ", page count: " + liveViewManager.getCurrentPageCount() );
		
		super.onDestroy();
	}

	@Override
	protected void gotoRealtimePreview() {
		
	}

	@Override
	protected void gotoPictureManagement() {
		if (liveViewManager != null) {
			liveViewManager.closeAllConnection(true);
		}
		
		super.gotoPictureManagement();
	}

	@Override
	protected void gotoPlayback() {
		if (liveViewManager != null) {
			liveViewManager.closeAllConnection(true);
		}
		
		super.gotoPlayback();
	}

	@Override
	protected void gotoDeviceManagement() {
		if (liveViewManager != null) {
			liveViewManager.closeAllConnection(true);
		}
		
		super.gotoDeviceManagement();
	}

	@Override
	protected void gotoSystemSetting() {
		if (liveViewManager != null) {
			liveViewManager.closeAllConnection(true);
		}
		
		super.gotoSystemSetting();
	}
	
	
	
	
//	private GestureDetector detector;
//    //定义一个动画数组，用于为ViewFlipper指定切换动画效果
//	private Animation[] animations = new Animation[4];
//    //定义手势动作两点之间的最小距离
//	private final int FLIP_DISTANCE = 50;
//	
//	class GestureListener extends SimpleOnGestureListener  
//    {  
//  
//        @Override  
//        public boolean onDoubleTap(MotionEvent e)  
//        {  
//            // TODO Auto-generated method stub  
//            Log.i("TEST", "onDoubleTap");  
//            return super.onDoubleTap(e);  
//        }  
//  
//        @Override  
//        public boolean onDown(MotionEvent e)  
//        {  
//            // TODO Auto-generated method stub  
//            Log.i("TEST", "onDown");  
//            //return super.onDown(e);  
//            return true;
//        }  
//  
//        @Override  
//        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,  
//                float velocityY)  
//        {  
//        	/*
//    		 * 如果第一个触点事件的X座标大于第二个触点事件的X座标超过FLIP_DISTANCE 也就是手势从右向左滑。
//    		 */
//    		if (e1.getX() - e2.getX() > FLIP_DISTANCE) {
//    			// 为flipper设置切换的的动画效果
////    			flipper.setInAnimation(animations[0]);
////    			flipper.setOutAnimation(animations[1]);
////    			flipper.showPrevious();
//    			
//    			ToastUtils.show(RealplayActivity.this, "向左滑动");
//
//    			return true;
//    		}
//    		/*
//    		 * 如果第二个触点事件的X座标大于第一个触点事件的X座标超过FLIP_DISTANCE 也就是手势从右向左滑。
//    		 */
//    		else if (e1.getX() - e2.getX() > FLIP_DISTANCE) {
//    			// 为flipper设置切换的的动画效果
////    			flipper.setInAnimation(animations[2]);
////    			flipper.setOutAnimation(animations[3]);
////    			flipper.showNext();
//    			
//    			ToastUtils.show(RealplayActivity.this, "向右滑动");
//    			return true;
//    		}
//    		return false;  
//        }  
//  
//        @Override  
//        public void onLongPress(MotionEvent e)  
//        {  
//            // TODO Auto-generated method stub  
//            Log.i("TEST", "onLongPress");  
//            super.onLongPress(e);  
//        }  
//  
//        @Override  
//        public boolean onScroll(MotionEvent e1, MotionEvent e2,  
//                float distanceX, float distanceY)  
//        {  
//            // TODO Auto-generated method stub  
//            Log.i("TEST", "onScroll:distanceX = " + distanceX + " distanceY = " + distanceY);  
//            return super.onScroll(e1, e2, distanceX, distanceY);  
//        }  
//  
//        @Override  
//        public boolean onSingleTapUp(MotionEvent e)  
//        {  
//            // TODO Auto-generated method stub  
//            Log.i("TEST", "onSingleTapUp");  
//            return super.onSingleTapUp(e);  
//        }  
//          
//    }
	
	
}
