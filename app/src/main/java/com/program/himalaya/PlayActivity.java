package com.program.himalaya;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.program.himalaya.adapters.PlayerTrackPagerAdapter;
import com.program.himalaya.base.BaseActivity;
import com.program.himalaya.interfaces.IPlayerCallback;
import com.program.himalaya.presenters.PlayerPresenter;
import com.program.himalaya.utils.LogUtil;
import com.program.himalaya.views.SobPopWindow;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_LIST;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_LIST_LOOP;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_RANDOM;
import static com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl.PlayMode.PLAY_MODEL_SINGLE_LOOP;

public class PlayActivity extends BaseActivity implements IPlayerCallback, ViewPager.OnPageChangeListener {

    private static final String TAG = "PlayActivity";
    private ImageView mControlBtn;
    private PlayerPresenter mPlayerPresenter;

    private SimpleDateFormat mMinFormt = new SimpleDateFormat("mm:ss");
    private SimpleDateFormat mHourFormt = new SimpleDateFormat("hh:mm:ss");
    private TextView mTotalDuration;
    private TextView mCurrentPosition;
    private SeekBar mDurationBar;
    private int mCurrentProgress = 0;
    private boolean mIsUserTouchProgressBar = false;
    private ImageView mPlayNextBtn;
    private ImageView mPlayPreBtn;
    private TextView mTrackTitleTv;
    private String mTrackTitleText;
    private ViewPager mTrackPageView;
    private PlayerTrackPagerAdapter mTrackPagerAdapter;
    private boolean mIsUserSlidePager = false;
    private ImageView mPlayModeSwitchBtn;

    private XmPlayListControl.PlayMode mCurrentMode = PLAY_MODEL_LIST;
    private static Map<XmPlayListControl.PlayMode, XmPlayListControl.PlayMode> sPlayModeRule = new HashMap<>();

// //???????????????????????????
//                //1.???????????????PLAY_MODEL_LIST????????????
//                //2.PLAY_MODEL_LIST_LOOP????????????
//                //3.PLAY_MODEL_RANDOM ????????????
//                //4.PLAY_MODEL_SINGLE_LOOP ??????????????????
//
//                // ????????????????????????mode?????????PlayMode?????????????????????
//                //PLAY_MODEL_SINGLE????????????
//                //PLAY_MODEL_SINGLE_LOOP ??????????????????
//                //PLAY_MODEL_LIST????????????
//                //PLAY_MODEL_LIST_LOOP????????????
//                //PLAY_MODEL_RANDOM ????????????
    static {
            sPlayModeRule.put(PLAY_MODEL_LIST,PLAY_MODEL_LIST_LOOP);
            sPlayModeRule.put(PLAY_MODEL_LIST_LOOP,PLAY_MODEL_RANDOM);
            sPlayModeRule.put(PLAY_MODEL_RANDOM,PLAY_MODEL_SINGLE_LOOP);
            sPlayModeRule.put(PLAY_MODEL_SINGLE_LOOP,PLAY_MODEL_LIST);

    }

    private View mPlayListBtn;
    private SobPopWindow mSobPopWindow;
    private ValueAnimator mOutBgAnimator;
    public final int BG_ANIMATION_DURATION = 300;
    private ValueAnimator mEnterBgAnmator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        initView();
        mPlayerPresenter = PlayerPresenter.getPlayerPresenter();
        mPlayerPresenter.registerViewCallback(this);
        initEvent();
        initBgAnimation();
    }

    private void initBgAnimation() {
        mEnterBgAnmator = ValueAnimator.ofFloat(1.0f,0.7f);
        mEnterBgAnmator.setDuration(BG_ANIMATION_DURATION);
        mEnterBgAnmator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value =(float) animation.getAnimatedValue();
                //????????????????????????????????????
                updateBgAlpha(value);
            }
        });
        //?????????
        mOutBgAnimator = ValueAnimator.ofFloat(0.7f, 1.0f);
        mOutBgAnimator.setDuration(BG_ANIMATION_DURATION);
        mOutBgAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value =(float) animation.getAnimatedValue();
                //????????????????????????????????????
                //????????????????????????????????????????????????
                updateBgAlpha(value);
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //????????????
        if (mPlayerPresenter != null) {
            mPlayerPresenter.ungisterViewCallback(this);
            mPlayerPresenter = null;
        }
    }


    private void initEvent() {
        mControlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //?????????????????????????????????????????????????????????
                //TODO:
                if (mPlayerPresenter.isPlay()) {
                    mPlayerPresenter.pause();
                } else {
                    //??????????????????????????????????????????????????????
                    mPlayerPresenter.play();
                }
            }
        });
        mDurationBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean isFromUser) {
                if (isFromUser) {
                    mCurrentProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsUserTouchProgressBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsUserTouchProgressBar = false;
                //?????????????????????????????????????????????
                mPlayerPresenter.seekTo(mCurrentProgress);
            }
        });

        mPlayPreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //?????????????????????
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.playpre();
                }
            }
        });
        mPlayNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //?????????????????????
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.playNext();
                }
            }
        });
        mTrackPageView.addOnPageChangeListener(this);

        mTrackPageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        mIsUserSlidePager=true;
                        break;
                }
                return false;
            }
        });

        mPlayModeSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchPlayMode();
            }
        });

        mPlayListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //??????????????????
                mSobPopWindow.showAtLocation(v, Gravity.BOTTOM,0,0);
                mEnterBgAnmator.start();
            }
        });
        //?????????????????????
        mSobPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                mOutBgAnimator.start();
            }
        });
        mSobPopWindow.setPlayListItemClickListener(new SobPopWindow.PlayListItemClickListener() {
            @Override
            public void onItemClick(int postion) {
                //????????????????????????item????????????
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.playByIndex(postion);
                }
            }
        });
        mSobPopWindow.setPlayListActionClickListener(new SobPopWindow.PlayListActionClickListener () {
            @Override
            public void onPlayModeClick() {
                //??????????????????
                switchPlayMode();
            }

            @Override
            public void onOderClick() {
                //????????????????????????
//                Toast.makeText(PlayActivity.this,"??????????????????",Toast.LENGTH_SHORT).show();
                if (mPlayerPresenter != null) {
                    mPlayerPresenter.reversePlayList();
                }
            }
        });
    }



    private void switchPlayMode() {
        //???????????????mode??????????????????mode
        XmPlayListControl.PlayMode playMode = sPlayModeRule.get(mCurrentMode);
        //??????????????????
        if (mPlayerPresenter != null) {
            mPlayerPresenter.swichPlayMode(playMode);
        }
    }

    public void updateBgAlpha(float alpha){
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.alpha = alpha;
        window.setAttributes(attributes);
    }
    //????????????????????????????????????????????????
    //1.???????????????PLAY_MODEL_LIST????????????
    //2.PLAY_MODEL_LIST_LOOP????????????
    //3.PLAY_MODEL_RANDOM ????????????
    //4.PLAY_MODEL_SINGLE_LOOP ??????????????????

    private void updatePLayModeBtnImg() {
        int resId=R.drawable.selector_play_mode_list_order;
        switch (mCurrentMode){
            case PLAY_MODEL_LIST:
                resId = R.drawable.selector_play_mode_list_order;
                break;
            case PLAY_MODEL_RANDOM:
                resId = R.drawable.selector_palyer_mode_random;
                break;
            case PLAY_MODEL_LIST_LOOP:
                resId= R.drawable.selector_palyer_mode_list_order_looper;
                break;
            case PLAY_MODEL_SINGLE_LOOP:
                resId = R.drawable.selector_palyer_mode_single_loop;
                break;
        }
        mPlayModeSwitchBtn.setImageResource(resId);
    }

    private void initView() {

        mControlBtn = this.findViewById(R.id.player_or_pause_btn);
        mTotalDuration = this.findViewById(R.id.track_duration);
        mCurrentPosition = this.findViewById(R.id.current_position);
        mDurationBar = this.findViewById(R.id.track_seek_bar);
        mPlayNextBtn = this.findViewById(R.id.player_next);
        mPlayPreBtn = this.findViewById(R.id.play_pre);
        mTrackTitleTv = this.findViewById(R.id.trck_title);
        if (!TextUtils.isEmpty(mTrackTitleText)) {
            mTrackTitleTv.setText(mTrackTitleText);
        }
        mTrackPageView = this.findViewById(R.id.track_pager_view);
        //???????????????
        mTrackPagerAdapter = new PlayerTrackPagerAdapter();
        //???????????????
        mTrackPageView.setAdapter(mTrackPagerAdapter);
        //???????????????????????????
        mPlayModeSwitchBtn = this.findViewById(R.id.player_mode_swicth_btn);
        //????????????
        mPlayListBtn = this.findViewById(R.id.player_list);
        mSobPopWindow = new SobPopWindow();
    }

    @Override
    public void onPlayStart() {
        //????????????,??????UI???????????????
        if (mControlBtn != null) {
            mControlBtn.setImageResource(R.drawable.selector_palyer_pause);
        }
    }

    @Override
    public void onPlayPause() {
        if (mControlBtn != null) {
            mControlBtn.setImageResource(R.drawable.selector_player_play);
        }
    }

    @Override
    public void onPlayStop() {
        if (mControlBtn != null) {
            mControlBtn.setImageResource(R.drawable.selector_player_play);
        }
    }

    @Override
    public void onPlayError() {

    }

    @Override
    public void nextOlay(Track track) {

    }

    @Override
    public void onPrePlay(Track track) {

    }

    @Override
    public void onListLoaded(List<Track> list) {
//        LogUtil.d(TAG,"list-->"+list);
        //??????????????????????????????
        if (mTrackPagerAdapter != null) {
            mTrackPagerAdapter.setData(list);
        }
        //????????????????????????????????????????????????
        if (mSobPopWindow != null) {
            mSobPopWindow.setListData(list);
        }

    }

    @Override
    public void onPlayModeChange(XmPlayListControl.PlayMode playMode) {
        //???????????????????????????UI
        mCurrentMode = playMode;
        //??????pop??????????????????
        mSobPopWindow.updatePlayMode(mCurrentMode);
        updatePLayModeBtnImg();

    }

    @Override
    public void onProgressChange(int currentProgress, int total) {
        mDurationBar.setMax(total);
        //????????????????????????????????????
        String totalDuration;
        String currentPosition;
        if (total > 1000 * 60 * 60) {
            totalDuration = mHourFormt.format(total);
            currentPosition = mHourFormt.format(currentProgress);
        } else {
            totalDuration = mMinFormt.format(total);
            currentPosition = mMinFormt.format(currentProgress);
        }
        if (mTotalDuration != null) {
            mTotalDuration.setText(totalDuration);
        }
        //??????????????????
        if (mCurrentPosition != null) {
            mCurrentPosition.setText(currentPosition);
        }
        //????????????
        //??????????????????
        if (!mIsUserTouchProgressBar) {
            mDurationBar.setProgress(currentProgress);
        }

    }

    @Override
    public void onAdLoading() {

    }

    @Override
    public void onAdFinished() {

    }

    @Override
    public void onTrackUpdate(Track track, int playIndex) {
        if (track==null) {
            LogUtil.d(TAG,"onTrackUpdate---> track==null");
            return;
        }
        this.mTrackTitleText = track.getTrackTitle();
        if (mTrackTitleTv != null) {
            //???????????????????????????
            mTrackTitleTv.setText(mTrackTitleText);
        }
        //????????????????????????????????????????????????????????????????????????

        //??????????????????????????????????????????????????????
        if (mTrackPageView != null) {
            mTrackPageView.setCurrentItem(playIndex, true);
        }
        //????????????????????????????????????
        if (mSobPopWindow != null) {
            mSobPopWindow.setCurrentPlayPosition(playIndex);
        }
    }

    @Override
    public void updateListOrder(boolean isReverse) {
            mSobPopWindow.updateOrderIcon(isReverse);
    }


    //PageView
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        LogUtil.d(TAG, "postion-->" + position);
        //????????????????????????,???????????????????????????
        if (mPlayerPresenter != null&&mIsUserSlidePager) {
            mPlayerPresenter.playByIndex(position);
        }
        mIsUserSlidePager =false;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
