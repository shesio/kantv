package com.cdeos.kantv.ui.activities.play;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.cdeos.kantv.bean.event.SendMsgEvent;
import com.cdeos.kantv.ui.fragment.LocalMediaFragment;
import com.cdeos.kantv.utils.Settings;
import com.cdeos.kantv.bean.FileManagerExtraItem;
import com.cdeos.kantv.bean.PlayHistoryBean;
import com.cdeos.kantv.bean.event.SaveCurrentEvent;
import com.cdeos.kantv.bean.event.UpdateFragmentEvent;
import com.cdeos.kantv.bean.params.PlayParam;
import com.cdeos.kantv.ui.activities.ShellActivity;
import com.cdeos.kantv.ui.fragment.settings.PlaySettingFragment;
import com.cdeos.kantv.ui.weight.dialog.CommonDialog;
import com.cdeos.kantv.ui.weight.dialog.FileManagerDialog;
import com.cdeos.kantv.utils.AppConfig;
import com.cdeos.kantv.utils.Constants.DefaultConfig;
import com.cdeos.kantv.utils.database.DataBaseManager;
import com.cdeos.kantv.utils.net.CommJsonEntity;
import com.cdeos.kantv.utils.net.CommJsonObserver;
import com.cdeos.kantv.utils.net.NetworkConsumer;

import com.cdeos.kantv.player.common.listener.PlayerViewListener;
import com.cdeos.kantv.player.common.receiver.BatteryBroadcastReceiver;
import com.cdeos.kantv.player.common.receiver.HeadsetBroadcastReceiver;
import com.cdeos.kantv.player.common.receiver.PlayerReceiverListener;
import com.cdeos.kantv.player.common.receiver.ScreenBroadcastReceiver;
import com.cdeos.kantv.player.common.utils.Constants;
import com.cdeos.kantv.player.ffplayer.FFPlayerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


import java.util.ArrayList;
import java.util.List;

import cdeos.media.player.FFmpegMediaPlayer;
import cdeos.media.player.IMediaPlayer;
import cdeos.media.player.CDELog;
import cdeos.media.player.CDEUtils;
import cdeos.media.player.bean.SubtitleBean;


public class PlayerActivity extends AppCompatActivity implements PlayerReceiverListener {
    private static final String TAG = PlayerActivity.class.getName();
    PlayerViewListener mPlayer;

    private boolean isInit = false;
    private String videoPath;
    private String videoTitle;
    private String zimuPath;
    private long currentPosition;
    private int episodeId;
    private int sourceOrigin;

    private List<SubtitleBean> subtitleList;


    private BatteryBroadcastReceiver batteryReceiver;
    private ScreenBroadcastReceiver screenReceiver;
    private HeadsetBroadcastReceiver headsetReceiver;
    private IMediaPlayer.OnOutsideListener onOutsideListener;

    public String subtitleDownloadPath;

    private boolean isKeepScreenOn = false;

    private Settings mSettings;
    private PlayerActivity mActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreen();

        mActivity = this;
        Context context = this.getBaseContext();
        mSettings = new Settings(this);

        CDEUtils.perfGetPlaybackBeginTime();


        CDELog.d(TAG, "ffplayer view");
        mPlayer = new FFPlayerView(this);
        setContentView((FFPlayerView) mPlayer);


        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();
        }

        subtitleList = new ArrayList<>();

        batteryReceiver = new BatteryBroadcastReceiver(this);
        screenReceiver = new ScreenBroadcastReceiver(this);
        headsetReceiver = new HeadsetBroadcastReceiver(this);
        PlayerActivity.this.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        PlayerActivity.this.registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        PlayerActivity.this.registerReceiver(headsetReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        PlayParam playParam = getIntent().getParcelableExtra("video_data");

        if (playParam == null) {
            ToastUtils.showShort("解析播放参数失败");
            new CommonDialog.Builder(this)
                    .setDismissListener(dialog -> PlayerActivity.this.finish())
                    .setAutoDismiss()
                    .setNightSkin()
                    .setTouchNotCancel()
                    .hideOk()
                    .build()
                    .show("解析播放参数失败", "", "退出重试");
            return;
        }

        videoPath = playParam.getVideoPath();
        videoTitle = playParam.getVideoTitle();
        zimuPath = playParam.getZimuPath();
        currentPosition = playParam.getCurrentPosition();
        episodeId = playParam.getEpisodeId();
        sourceOrigin = playParam.getSourceOrigin();

        CDELog.d(TAG, "video path:" + videoPath);
        CDELog.d(TAG, "video name:" + videoTitle);

        subtitleDownloadPath = AppConfig.getInstance().getDownloadFolder()
                + com.cdeos.kantv.utils.Constants.DefaultConfig.subtitleFolder;

        initListener();

        initPlayer();

        CDEUtils.umStartPlayback(videoTitle, videoPath);
        EventBus.getDefault().register(this);
    }


    private void initPlayer() {
        CDELog.d(TAG, "init player: playengine:" + mSettings.getPlayerEngine());

        CDELog.d(TAG, "init ff player");
        initFFmpegPlayer();


        isInit = true;
        if (episodeId > 0)
            addPlayHistory(episodeId);
    }

    private void initListener() {
        onOutsideListener = (what, extra) -> {
            switch (what) {
                case Constants.INTENT_SAVE_CURRENT:

                    SaveCurrentEvent event = new SaveCurrentEvent();
                    event.setCurrentPosition(extra);
                    event.setFolderPath(FileUtils.getDirName(videoPath));
                    event.setVideoPath(videoPath);
                    EventBus.getDefault().post(event);

                    DataBaseManager.getInstance()
                            .selectTable("file")
                            .update()
                            .param("current_position", event.getCurrentPosition())
                            .where("folder_path", event.getFolderPath())
                            .where("file_path", event.getVideoPath())
                            .postExecute();
                    break;

                case Constants.INTENT_PLAY_FAILED:
                    CommonDialog.Builder builder = new CommonDialog
                            .Builder(this)
                            .setDismissListener(dialog -> {
                                onOutsideListener.onAction(Constants.INTENT_PLAY_END, 0);
                                PlayerActivity.this.finish();
                            })
                            .setOkListener(dialog -> {
                                Intent intent = new Intent(PlayerActivity.this, ShellActivity.class);
                                intent.putExtra("fragment", PlaySettingFragment.class.getName());
                                startActivity(intent);
                            })
                            .setAutoDismiss()
                            .setNightSkin()
                            .setTouchNotCancel();
                    if (sourceOrigin == PlayerManagerActivity.SOURCE_ONLINE_PREVIEW) {
                        builder.setHideOk(false).build()
                                .show("播放失败，资源无法下载", "", "退出播放");
                    } else {
                        builder.setHideOk(true).build()
                                .show("播放失败，请尝试更改播放器设置", "播放器设置", "退出播放");
                    }
                    break;

                case Constants.INTENT_PLAY_END:
                    if (sourceOrigin == PlayerManagerActivity.SOURCE_ONLINE_PREVIEW) {
                        FileUtils.deleteAllInDir(DefaultConfig.cacheFolderPath);
                    }
                    break;

                case Constants.INTENT_PLAY_COMPLETE:
                    if (isKeepScreenOn && extra == 1) {

                        Window window = getWindow();
                        if (window != null) {
                            isKeepScreenOn = false;
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    } else if (!isKeepScreenOn && extra == 0) {

                        Window window = getWindow();
                        if (window != null) {
                            isKeepScreenOn = true;
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }
                    }

                    if (extra == 1) {
                        finish();
                    }
                    break;
            }
        };
    }

    private void initFFmpegPlayer() {
        FFPlayerView ffPlayerView = (FFPlayerView) mPlayer;

        boolean autoLoadLocalSubtitle = AppConfig.getInstance().isAutoLoadLocalSubtitle()
                && TextUtils.isEmpty(zimuPath);
        boolean autoLoadNetworkSubtitle = AppConfig.getInstance().isAutoLoadNetworkSubtitle()
                && TextUtils.isEmpty(zimuPath);
        CDELog.d(TAG, "video path:" + videoPath);
        CDELog.d(TAG, "video name:" + videoTitle);
        CDELog.d(TAG, "FFmpegMediaPlayer.loadLibrariesOnce");
        FFmpegMediaPlayer.loadLibrariesOnce(null);
        CDELog.d(TAG, "after FFmpegMediaPlayer.loadLibrariesOnce");

        ffPlayerView
                .setOnInfoListener(onOutsideListener)
                .setNetworkSubtitle(AppConfig.getInstance().isUseNetWorkSubtitle())
                .setAutoLoadLocalSubtitle(autoLoadLocalSubtitle)
                .setAutoLoadNetworkSubtitle(autoLoadNetworkSubtitle)
                .setSubtitleFolder(subtitleDownloadPath);

        if (mSettings.getContinuedPlayback()) {
            ffPlayerView.setSkipTip(currentPosition);
        }

        ffPlayerView.setTitle(videoTitle);
        ffPlayerView.setVideoPath(videoPath);
        ffPlayerView.start();

        mPlayer.setSubtitlePath(zimuPath);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SendMsgEvent event) {
        String msg = event.getMsg();
        CDELog.d(TAG, "msg: " + msg);
    }

    @Override
    protected void onDestroy() {
        mPlayer.onDestroy();
        this.unregisterReceiver(batteryReceiver);
        this.unregisterReceiver(screenReceiver);
        this.unregisterReceiver(headsetReceiver);
        super.onDestroy();

        CDEUtils.umStopPlayback(videoTitle, videoPath);

        if (mSettings.getPlayMode() != CDEUtils.PLAY_MODE_NORMAL) {
            CDELog.d(TAG, "send event");
            EventBus.getDefault().post(new SendMsgEvent("PlaybackStop" + "_KANTV_" +
                    mSettings.getPlayMode() + "_KANTV_" + videoTitle.trim() + "_KANTV_" + videoPath.trim()));
        }
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        if (isInit)
            mPlayer.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (isInit)
            mPlayer.onPause();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        CDELog.d(TAG, "keyCode: " + keyCode);
        return mPlayer.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (mPlayer.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mPlayer.configurationChanged(newConfig);
    }

    @Override
    public void onBatteryChanged(int status, int progress) {
        mPlayer.setBatteryChanged(status, progress);
    }

    @Override
    public void onScreenLocked() {
        mPlayer.onScreenLocked();
    }

    @Override
    public void onHeadsetRemoved() {
        mPlayer.onPause();
    }


    private void setFullScreen() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //开启屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        isKeepScreenOn = true;
    }


    private void addPlayHistory(int episodeId) {
        if (episodeId > 0) {
            PlayHistoryBean.addPlayHistory(episodeId, new CommJsonObserver<CommJsonEntity>(this) {
                @Override
                public void onSuccess(CommJsonEntity commJsonEntity) {
                    LogUtils.d("add history success: episodeId：" + episodeId);
                }

                @Override
                public void onError(int errorCode, String message) {
                    LogUtils.e("add history fail: episodeId：" + episodeId + "  message：" + message);
                }
            }, new NetworkConsumer());
        }
    }


    private void selectSubtitle() {
        String folderPath = sourceOrigin == PlayerManagerActivity.SOURCE_ORIGIN_LOCAL
                ? videoPath
                : DefaultConfig.downloadPath;

        FileManagerExtraItem extraItem = null;

        new FileManagerDialog(
                PlayerActivity.this,
                folderPath,
                FileManagerDialog.SELECT_SUBTITLE,
                this::updateSubtitle
        ).addExtraItem(extraItem).show();
    }


    private void updateSubtitle(String subtitleFilePath) {
        if (sourceOrigin == PlayerManagerActivity.SOURCE_ORIGIN_LOCAL) {
            DataBaseManager.getInstance()
                    .selectTable("file")
                    .update()
                    .param("zimu_path", subtitleFilePath)
                    .where("file_path", videoPath)
                    .postExecute();
            EventBus.getDefault().post(UpdateFragmentEvent.updatePlay(LocalMediaFragment.UPDATE_DATABASE_DATA));
        }
        mPlayer.setSubtitlePath(subtitleFilePath);
    }
}
