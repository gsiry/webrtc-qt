package com.video.play;

import java.io.File;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.support.v4.view.ViewPager.LayoutParams;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.video.R;
import com.video.data.Value;
import com.video.service.BackstageService;
import com.video.service.MainApplication;
import com.video.socket.ZmqThread;
import com.video.utils.Utils;

public class PlayerActivity  extends Activity implements OnClickListener  {

	private static Context mContext;
	private boolean isLocalDevice = false;
	private String localDeviceIPandPort = "";
	
	private static VideoView videoView = null; //视频对象
	private static AudioThread audioThread = null; //音频对象
	private static TalkThread talkThread = null; //对讲对象
	private WakeLock wakeLock = null; //锁屏对象
	
	private TimeCountThread timeCountThread = null; // 随机更新SeekBar进度
	private int videoLoadedPercent = 0;
	private TextView tv_loaded = null;
	
//	private PlayerReceiver playerReceiver;

	private TextView tv_title = null;
	private static String deviceName = null;
	private static String dealerName = null;
	
	private int screenWidth = 0;
	private int screenHeight = 0;
	private int titleHeight = 80;
	private int bottomHeight = 100;
	
	private static boolean isRecordVideo = false;
	private boolean isVoiceEnable = true;
	private boolean isTalkEnable = false;
	private boolean isPlayMusic = false;
	private boolean isPopupWindowShow = false;
	
	private final int SHOW_TIME_MS = 6000;
	private final int HIDE_POPUPWINDOW = 1;
	private final static int REQUEST_TIMEOUT = 2;
	private final static int DISPLAY_VIDEO_VIEW = 3;
	private final static int CHANGE_LOADED_VIEW = 4;
	
	private GestureDetector mGestureDetector = null; // 手势识别
	
	private View titleView = null; // 标题视图
	private static PopupWindow titlePopupWindow = null; // 标题弹出框
	private View bottomView = null;// 底部视图
	private static PopupWindow bottomPopupWindow = null; // 底部弹出框
	private View recordView = null; // 录像视图
	private static PopupWindow recordPopupWindow = null; // 录像弹出框

	private Button player_capture = null;
	private Button player_record = null;
	private Button player_screen = null;
	private Button player_talkback = null;
	private Button player_sound = null;
	private Button video_clarity = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.FULL_WAKE_LOCK, "WakeLock");
		wakeLock.acquire(); // 设置屏幕保持唤醒
		
		initPlayer();
	}
	
	private void initPlayer() {
		mContext = PlayerActivity.this;
		getScreenSize();
		
		//注册广播
//		playerReceiver = new PlayerReceiver();
//		IntentFilter filter = new IntentFilter();
//		filter.addAction(BackstageService.TUNNEL_REQUEST_ACTION);
//		registerReceiver(playerReceiver, filter);
		
		// 视频
		videoView = new VideoView(this);
		setContentView(R.layout.video_player_startup);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// 音频
		audioThread = new AudioThread();
		
		// 加载页面
		tv_loaded = (TextView) this.findViewById(R.id.tv_video_loaded_count);
		
		// 视频标题弹出框
		titleView = getLayoutInflater().inflate(R.layout.player_title_view, null);
		titlePopupWindow = new PopupWindow(titleView, screenWidth, titleHeight, false);
		tv_title = (TextView) titleView.findViewById(R.id.tv_player_title);
		tv_title.setText(deviceName);
		ImageButton player_back = (ImageButton) titleView.findViewById(R.id.ib_player_back);
		player_back.setOnClickListener(this);
		
		// 视频底部弹出框
		bottomView = getLayoutInflater().inflate(R.layout.player_bottom_view, null);
		bottomPopupWindow = new PopupWindow(bottomView, screenWidth, bottomHeight, false);
		player_capture = (Button) bottomView.findViewById(R.id.btn_player_capture);
		player_capture.setOnClickListener(this);
		player_record = (Button) bottomView.findViewById(R.id.btn_player_record);
		player_record.setOnClickListener(this);
		player_screen = (Button) bottomView.findViewById(R.id.btn_player_screen);
		player_screen.setOnClickListener(this);
		player_talkback = (Button) bottomView.findViewById(R.id.btn_player_talkback);
		player_talkback.setOnClickListener(this);
		player_sound = (Button) bottomView.findViewById(R.id.btn_player_sound);
		player_sound.setOnClickListener(this);
		video_clarity = (Button) bottomView.findViewById(R.id.btn_player_clarity);
		video_clarity.setOnClickListener(this);
		
		// 录像弹出框
		recordView = getLayoutInflater().inflate(R.layout.player_video_record_view, null);
		recordPopupWindow = new PopupWindow(recordView, screenWidth, bottomHeight, false);
		
		Intent intent = this.getIntent();
		deviceName = (String) intent.getCharSequenceExtra("deviceName");
		dealerName = (String) intent.getCharSequenceExtra("dealerName");
		if (intent.hasExtra("isLocalDevice")) {
			isLocalDevice = intent.getBooleanExtra("isLocalDevice", false);
		}
		if (isLocalDevice) {
			localDeviceIPandPort = (String) intent.getCharSequenceExtra("localDeviceIPandPort");
		}
		
		if (!isLocalDevice) {
			if (TunnelCommunication.getInstance().IsTunnelOpened(dealerName)) {
				System.out.println("MyDebug: 【通道已打开】");
				Value.isTunnelOpened = true;
				//【播放视频】 1:主通道高清  2:子通道标清  3:子通道流畅
				TunnelCommunication.getInstance().startMediaData(dealerName, 2);
				videoView.playVideo();
				sendHandlerMsg(DISPLAY_VIDEO_VIEW, 3000);
			} else {
				System.out.println("MyDebug: 【再次打开通道】");
				//【打开通道】
				TunnelCommunication.getInstance().openTunnel(dealerName);
			}
		} else {
			//【本地设备】
			TunnelCommunication.getInstance().connectLocalDevice(localDeviceIPandPort);
		}
		if (timeCountThread == null) {
			timeCountThread = new TimeCountThread(true);
			timeCountThread.start();
		}
		sendHandlerMsg(REQUEST_TIMEOUT, Value.REQ_TIME_30S);
		
		mGestureDetector = new GestureDetector(new SimpleOnGestureListener(){
			// 单击屏幕
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				// TODO Auto-generated method stub
				if (isPopupWindowShow) {
					hidePopupWindow();
				} else {
					showPopupWindow();
					hidePopupWindowDelay();
				}
				return false;
			}
		});
	}
	
	public class TimeCountThread extends Thread {
		
		private boolean runFlag = false;
		private boolean isPaused = false;
		private int randomCount = 0;
		
		public TimeCountThread(boolean isRun) {
			runFlag = isRun;
			randomCount = (int)(Math.random()*9+4);
		}
		
		/**
		 * 开始计数
		 */
		public void startCount() {
			isPaused = false;
		}
		
		/**
		 * 暂停计数
		 */
		public void pauseCount() {
			isPaused = true;
		}
		
		/**
		 * 停止计数
		 */
		public void stopCount() {
			isPaused = true;
			runFlag = false;
			if (timeCountThread != null)
				timeCountThread = null;
		}
		
		public void run() {
			int timeCount = 0;

			while (runFlag) {
				try {
					Thread.sleep(100);
					if ((videoLoadedPercent >= 85) && (Value.isTunnelOpened)) {
						startCount();
					}
					if (isPaused) {
						continue;
					}
					if (!Value.isTunnelOpened) {
						// 未打开通道
						if (videoLoadedPercent < 85) {
							timeCount ++;
							if (timeCount >= 5) {
								timeCount = 0;
								randomCount = (int)(Math.random()*9+4);
								videoLoadedPercent += randomCount;
								sendHandlerMsg(CHANGE_LOADED_VIEW);
							}
						} else {
							pauseCount();
						}
					} else {
						// 打开通道
						if (videoLoadedPercent < 85) {
							videoLoadedPercent = 85;
							sendHandlerMsg(CHANGE_LOADED_VIEW);
						} else {
							timeCount ++;
							if (timeCount >= 5) {
								timeCount = 0;
								videoLoadedPercent += (100 - videoLoadedPercent)/4;
								if (videoLoadedPercent > 99) {
									videoLoadedPercent = 99;
								}
								sendHandlerMsg(CHANGE_LOADED_VIEW);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
				// 隐藏弹出框
				case HIDE_POPUPWINDOW:
					hidePopupWindow();
					break;
				// 请求超时
				case REQUEST_TIMEOUT:
					closePlayer();
					finish();
					break;
				// 显示实时视频
				case DISPLAY_VIDEO_VIEW:
					timeCountThread.stopCount();
					setContentView(videoView);
					if (audioThread != null) {
						audioThread.start();
					}
					isPopupWindowShow = true;
					if (titlePopupWindow != null && videoView.isShown()) {
						showTitlePopupWindow();
					}
					if (bottomPopupWindow != null && videoView.isShown()) {
						showBottomPopupWindow();
					}
					hidePopupWindowDelay();
					break;
				// 更新加载页面进度
				case CHANGE_LOADED_VIEW:
					tv_loaded.setText("已加载("+videoLoadedPercent+"%)，请稍后...");
					break;
			}
		}
	};
	
	/**
	 * 发送Handler消息
	 */
	public void sendHandlerMsg(int what) {
		Message msg = new Message();
		msg.what = what;
		handler.sendMessage(msg);
	}
	public void sendHandlerMsg(int what, int timeout) {
		Message msg = new Message();
		msg.what = what;
		handler.sendMessageDelayed(msg, timeout);
	}
	public void sendHandlerMsg(Handler handler, int what, HashMap<String, String> obj) {
		Message msg = new Message();
		msg.what = what;
		msg.obj = obj;
		handler.sendMessage(msg);
	}
	
	/**
	 * 显示PopupWindow
	 */
	private void showPopupWindow() {
		isPopupWindowShow = true;
		cancelDelayHide();
		showTitlePopupWindow();
		showBottomPopupWindow();
	}
	
	/**
	 * 显示顶部PopupWindow
	 */
	private void showTitlePopupWindow() {
		titlePopupWindow.setHeight(LayoutParams.WRAP_CONTENT);
		titlePopupWindow.setBackgroundDrawable(new BitmapDrawable());
		titlePopupWindow.setOutsideTouchable(true);

		titlePopupWindow.setAnimationStyle(R.style.PopupAnimationTop);
		titlePopupWindow.showAtLocation(videoView, Gravity.TOP, 0, 0);
		titlePopupWindow.update();
	}
	
	/**
	 * 显示底部PopupWindow
	 */
	private void showBottomPopupWindow() {
		bottomPopupWindow.setHeight(LayoutParams.WRAP_CONTENT); 
		bottomPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		bottomPopupWindow.setOutsideTouchable(true);
		
		bottomPopupWindow.setAnimationStyle(R.style.PopupAnimationBottom);
		bottomPopupWindow.showAtLocation(videoView, Gravity.BOTTOM, 0, 0);
		bottomPopupWindow.update();
	}
	
	/**
	 * 显示录像PopupWindow
	 */
	private void showRecordPopupWindow() {
		if (recordPopupWindow != null && videoView.isShown()) {
			recordPopupWindow.setWidth(LayoutParams.WRAP_CONTENT);
			recordPopupWindow.setHeight(LayoutParams.WRAP_CONTENT); 
			recordPopupWindow.setBackgroundDrawable(new BitmapDrawable());
			recordPopupWindow.setOutsideTouchable(false);
			recordPopupWindow.setTouchable(true);
			
			recordPopupWindow.showAtLocation(videoView, Gravity.TOP | Gravity.LEFT, 30, 60);
			recordPopupWindow.update();
		}
	}
	
	/**
	 * 隐藏录像PopupWindow
	 */
	private void hideRecordPopupWindow() {
		if (recordPopupWindow.isShowing()) {
			recordPopupWindow.dismiss();
		}
	}
	
	/**
	 * 隐藏PopupWindow
	 */
	private void hidePopupWindow() {
		isPopupWindowShow = false;
		cancelDelayHide();
		if (titlePopupWindow.isShowing()) {
			titlePopupWindow.dismiss();
		}
		if (bottomPopupWindow.isShowing()) {
			bottomPopupWindow.dismiss();
		}
	}
	
	/**
	 * 延迟隐藏控制器
	 */
	private void hidePopupWindowDelay() {
		cancelDelayHide();
		handler.sendEmptyMessageDelayed(HIDE_POPUPWINDOW, SHOW_TIME_MS);
	}
	
	/**
	 * 取消延迟隐藏
	 */
	private void cancelDelayHide() {
		if (handler.hasMessages(HIDE_POPUPWINDOW)) {
			handler.removeMessages(HIDE_POPUPWINDOW);
		}
	}
	
	/**
	 * 获得屏幕尺寸大小
	 */
	private void getScreenSize() {
		Display display = getWindowManager().getDefaultDisplay();
		screenWidth = display.getWidth();
		screenHeight = display.getHeight();
		if (screenHeight > screenWidth) {
			screenWidth = screenHeight;
		}
	}
	
	/**
	 * 自定义Toast显示
	 */
	private void toastNotify(Context context, String notify_text, int duration) {	
		LayoutInflater Inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View tx_view = Inflater.inflate(R.layout.toast_layout, null);
		
		TextView textView = (TextView)tx_view.findViewById(R.id.toast_text_id);
		textView.setText(notify_text);
		
		Toast toast = new Toast(context);
		toast.setDuration(duration);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.setView(tx_view);
		toast.show();
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getScreenSize();
        if (isPopupWindowShow) {
			showPopupWindow();
			hidePopupWindowDelay();
		} else {
			hidePopupWindow();
		}
	}
	
	/**
	 * 播放抓拍声音
	 */
	private void playMyMusic(int resid) {
		if (!isPlayMusic) {
			MediaPlayer mediaPlayer = null;
			if (mediaPlayer == null) {
				mediaPlayer = MediaPlayer.create(MainApplication.getInstance(), resid);
				mediaPlayer.stop();
			}
			mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					mp.release();
					mp = null;
					isPlayMusic = false;
				}
			});
			try {
				mediaPlayer.prepare();
				mediaPlayer.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
			// 关闭播放器
			case R.id.ib_player_back:
				if (!isLocalDevice) {
					TunnelCommunication.getInstance().stopMediaData(dealerName);
					closePlayer();
					PlayerActivity.this.finish();
				} else {
					// 本地设备
					TunnelCommunication.getInstance().stopLocalVideo(localDeviceIPandPort);
					TunnelCommunication.getInstance().disconnectLocalDevice(localDeviceIPandPort);
				}
				break;
			// 截屏
			case R.id.btn_player_capture:
				if (videoView.captureVideo()) {
					playMyMusic(R.raw.capture);
					toastNotify(mContext, "抓拍成功", Toast.LENGTH_SHORT);
				} else {
					toastNotify(mContext, "抓拍失败", Toast.LENGTH_SHORT);
				}
				break;
			// 录像
			case R.id.btn_player_record:
				if (!isLocalDevice) {
					try {
						if (!isRecordVideo) {
							String videoName = videoView.captureThumbnails();
							if (videoName != null) {
								// 开始录视频
								isRecordVideo = true;
								playMyMusic(R.raw.record);
								showRecordPopupWindow();
								player_record.setBackgroundResource(R.drawable.player_record_enable);
								
								String SDPath = Environment.getExternalStorageDirectory().getAbsolutePath();
								String filePath1 = SDPath + File.separator + "KaerVideo";
								File videoFilePath1 = new File(filePath1);
								if(!videoFilePath1.exists()){
									videoFilePath1.mkdir();
								} 
								String filePath2 = filePath1 + File.separator + "video";
								File videoFilePath2 = new File(filePath2);
								if(!videoFilePath2.exists()){
									videoFilePath2.mkdir();
								} 
								String filePath3 = filePath2 + File.separator +Utils.getNowTime("yyyy-MM-dd");
								File videoFilePath3 = new File(filePath3);
								if(!videoFilePath3.exists()){
									videoFilePath3.mkdir();
								} 
								String videoFile = filePath3 + File.separator +videoName + ".avi";
								TunnelCommunication.getInstance().startRecordVideo(dealerName, videoFile);
							} else {
								toastNotify(mContext, "录像失败", Toast.LENGTH_SHORT);
							}
						} else {
							// 停止录视频
							isRecordVideo = false;
							hideRecordPopupWindow();
							player_record.setBackgroundResource(R.drawable.player_record_disable);
							TunnelCommunication.getInstance().stopRecordVideo(dealerName);
						}
					} catch (Exception e) {
						System.out.println("MyDebug: 录像异常！");
						e.printStackTrace();
					}
				} else {
					toastNotify(mContext, "暂时无法使用该功能", Toast.LENGTH_SHORT);
				}
				break;
			// 是否全屏
			case R.id.btn_player_screen:
				// 手机震动
				Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
				vibrator.vibrate(100);
				if (videoView.isFullScreen) {
					player_screen.setBackgroundResource(R.drawable.player_screen_scale);
					toastNotify(mContext, "保持宽高比播放", Toast.LENGTH_SHORT);
				} else {
					player_screen.setBackgroundResource(R.drawable.player_screen_full);
					toastNotify(mContext, "全屏播放", Toast.LENGTH_SHORT);
				}
				videoView.isFullScreen = !videoView.isFullScreen;
				break;
			// 对讲
			case R.id.btn_player_talkback:
				if (Value.isSharedUser) {
					toastNotify(mContext, "您无权使用对讲功能！", Toast.LENGTH_SHORT);
				} else {
					if (!isLocalDevice) {
						playMyMusic(R.raw.di);
						if (isTalkEnable) {
							// 停止对讲
							isTalkEnable =false;
							player_talkback.setBackgroundResource(R.drawable.player_talkback_disable);
							if (talkThread != null) {
								talkThread.stopTalkThread();
							}
						} else {
							// 开始对讲
							isTalkEnable = true;
							player_talkback.setBackgroundResource(R.drawable.player_talkback_enable);
							talkThread = new TalkThread();
							if (talkThread != null) {
								talkThread.start();
							}
						}
					} else {
						toastNotify(mContext, "暂时无法使用该功能", Toast.LENGTH_SHORT);
					}
				}
				break;
			// 声音
			case R.id.btn_player_sound:
				if (isVoiceEnable) {
					isVoiceEnable = false;
					player_sound.setBackgroundResource(R.drawable.player_sound_disable);
					audioThread.closeAudioTrackVolume();
				} else {
					isVoiceEnable = true;
					player_sound.setBackgroundResource(R.drawable.player_sound_enable);
					audioThread.openAudioTrackVolume();
				}
				break;
		}
		if (v.getId() != R.id.ib_player_back) {
			cancelDelayHide();
			hidePopupWindowDelay();
		}
	}
	
	/**
	 * @param order: "move_left", "move_right", "move_up", "move_down", "stop" 
	 * @return 返回生成JSON云台控制的字符串
	 */
	private String generatePtzControlJson(String order) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("type", "tunnel");
			jsonObj.put("command", "ptz");
			jsonObj.put("control", order);
			jsonObj.put("param", 0);
			return jsonObj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void sendPtzControlOrder(String order) {
		String data = generatePtzControlJson(order);
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("peerId", dealerName);
		map.put("peerData", data);
		sendHandlerMsg(ZmqThread.zmqThreadHandler, R.id.send_to_peer_id, map); 
	}
	
	private boolean isPtzControling = false;
	private long startTime = 0;
	private long endTime = 0;
	private long spaceTime = 0;
	
	private final String PTZ_UP = "move_up";
	private final String PTZ_DOWN = "move_down";
	private final String PTZ_LEFT = "move_left";
	private final String PTZ_RIGHT = "move_right";
	private final String PTZ_STOP = "stop";
	
	private float startPointX = 0.0f;
	private float startPointY = 0.0f;
	
	private float endPointX = 0.0f;
	private float endPointY = 0.0f;
	
	private float spaceX = 0.0f;
	private float spaceY = 0.0f;
	
	//处理云台事件
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		boolean result = mGestureDetector.onTouchEvent(event);

		if (!isLocalDevice) {
			if (!result) {
				if (!Value.isSharedUser) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_UP:
							if (isPtzControling) {
								//停止
								isPtzControling = false;
								sendPtzControlOrder(PTZ_STOP);
							}
							break;
						case MotionEvent.ACTION_DOWN:
							isPtzControling = false;
							startTime = System.currentTimeMillis();
							startPointX = event.getX();
							startPointY = event.getY();
							break;
						case MotionEvent.ACTION_MOVE:
							endTime = System.currentTimeMillis();
							spaceTime = endTime - startTime;
							if (!isPtzControling && (spaceTime > 200)) {
								isPtzControling = true;
								
								endPointX = event.getX();
								endPointY = event.getY();
								spaceX = endPointX - startPointX;
								spaceY = endPointY - startPointY;
								
								if (Math.abs(spaceX) >= Math.abs(spaceY)) {
									if (spaceX < -1) {
										//向左
										sendPtzControlOrder(PTZ_LEFT);
									} 
									else if (spaceX > 1) {
										//向右
										sendPtzControlOrder(PTZ_RIGHT);
									}
								} else {
									if (spaceY < -1) {
										//向上
										sendPtzControlOrder(PTZ_UP);
									} 
									else if (spaceY > 1) {
										//向下
										sendPtzControlOrder(PTZ_DOWN);
									}
								}
							}
							break;
					}
				}
				result = super.onTouchEvent(event);
			}
		} else {
			
		}
		return result;
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		closePlayer();
		finish();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		closePlayer();
		PlayerActivity.this.finish();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//注销广播
//		unregisterReceiver(playerReceiver);
		//解除屏幕保持唤醒
		if ((wakeLock != null) && (wakeLock.isHeld())) {
			wakeLock.release(); 
			wakeLock = null;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK  && event.getRepeatCount() == 0) {
			if (timeCountThread != null) {
				timeCountThread.stopCount();
			}
			Value.isTunnelOpened = false;
			if (!isLocalDevice) {
				TunnelCommunication.getInstance().stopMediaData(dealerName);
				closePlayer();
				PlayerActivity.this.finish();
			} else {
				// 本地设备
				TunnelCommunication.getInstance().stopLocalVideo(localDeviceIPandPort);
				TunnelCommunication.getInstance().disconnectLocalDevice(localDeviceIPandPort);
			}
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void destroyDialogView() {
		//销毁弹出框
		if (titlePopupWindow.isShowing()) {
			titlePopupWindow.dismiss();
		}
		if (bottomPopupWindow.isShowing()) {
			bottomPopupWindow.dismiss();
		}
		if (recordPopupWindow.isShowing()) {
			recordPopupWindow.dismiss();
		}
	}

	/**
	 * 关闭播放器
	 */
	private void closePlayer() {
		try {
			destroyDialogView();
			if (timeCountThread != null) {
				timeCountThread.stopCount();
			}
			//关闭实时音视频
			try {
				videoView.stopVideo();
				audioThread.stopAudioThread();
				if (talkThread != null) {
					talkThread.stopTalkThread();
				}
			} catch (Exception e) {
				System.out.println("MyDebug: 关闭音视频对讲异常！");
				e.printStackTrace();
			}
		} catch (Exception e) {
			System.out.println("MyDebug: 关闭实时播放器异常！");
			e.printStackTrace();
		}
	}
	
	/**
	 * @author sunfusheng
	 * 播放器的广播接收
	 */
	public class PlayerReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (action.equals(BackstageService.TUNNEL_REQUEST_ACTION)) {
				int TunnelEvent = intent.getIntExtra("TunnelEvent", 1);
				switch (TunnelEvent) {
					case 0:
						if (handler.hasMessages(REQUEST_TIMEOUT)) {
							handler.removeMessages(REQUEST_TIMEOUT);
						}
						Value.isTunnelOpened = true;
						if (!isLocalDevice) {
							//【播放视频】
							TunnelCommunication.getInstance().startMediaData(dealerName, 0);
						} else {
							//【本地设备】
							TunnelCommunication.getInstance().startLocalVideo(localDeviceIPandPort);
							if (videoView != null) {
								videoView.isDisplayView = true;
							}
						}
						videoView.playVideo();
						sendHandlerMsg(DISPLAY_VIDEO_VIEW, 3000);
						break;
					case 1:
						Value.isTunnelOpened = false;
						closePlayer();
						PlayerActivity.this.finish();
						break;
				}
			}
		}
	}
	
}
	
//	{
//		private boolean isClarityPopupWindowShow = false;
//		private Button clarity_high = null;
//		private Button clarity_normal = null;
//		private Button clarity_low = null;
//		private View clarityView = null;//底部视图
//		private PopupWindow clarityPopupWindow = null;//底部弹出框
		
		//视频质量弹出框
//		clarityView = getLayoutInflater().inflate(R.layout.player_clarity_view, null);
//		clarityPopupWindow = new PopupWindow(clarityView);
//		clarity_high = (Button) clarityView.findViewById(R.id.btn_video_clarity_high);
//		clarity_high.setOnClickListener(this);
//		clarity_normal = (Button) clarityView.findViewById(R.id.btn_video_clarity_normal);
//		clarity_normal.setOnClickListener(this);
//		clarity_low = (Button) clarityView.findViewById(R.id.btn_video_clarity_low);
//		clarity_low.setOnClickListener(this);
		
//		if (clarityPopupWindow != null) {
//			clarityPopupWindow.dismiss();
//			isClarityPopupWindowShow = false;
//		}
		
//		//视频清晰度选择
//	case R.id.btn_player_clarity:
//		if (isClarityPopupWindowShow) {
//			isClarityPopupWindowShow = false;
//			clarityPopupWindow.update(0, 0, 0, 0);
//		} else {
//			isClarityPopupWindowShow = true;
//			if (clarityPopupWindow != null && videoView != null) {
//				clarityPopupWindow.showAtLocation(videoView, Gravity.BOTTOM|Gravity.RIGHT, 0, 0);
//				clarityPopupWindow.update(10, (bottomHeight+10), LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//			}
//		}
//		break;
//	//高清
//	case R.id.btn_video_clarity_high:
//		System.out.println("MyDebug: ---> btn_video_clarity_high");
//		if (isClarityPopupWindowShow) {
//			isClarityPopupWindowShow = false;
//			clarityPopupWindow.update(0, 0, 0, 0);
//		}
//		break;
//	//标准
//	case R.id.btn_video_clarity_normal:
//		System.out.println("MyDebug: ---> btn_video_clarity_normal");
//		if (isClarityPopupWindowShow) {
//			isClarityPopupWindowShow = false;
//			clarityPopupWindow.update(0, 0, 0, 0);
//		}
//		break;
//	//流畅
//	case R.id.btn_video_clarity_low:
//		System.out.println("MyDebug: ---> btn_video_clarity_low");
//		if (isClarityPopupWindowShow) {
//			isClarityPopupWindowShow = false;
//			clarityPopupWindow.update(0, 0, 0, 0);
//		}
//		break;
		
//		if (clarityPopupWindow != null) {
//			clarityPopupWindow.dismiss();
//		}
//	}
