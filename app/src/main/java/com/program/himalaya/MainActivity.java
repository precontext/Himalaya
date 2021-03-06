package com.program.himalaya;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.program.himalaya.adapters.IndicatorAdapter;
import com.program.himalaya.adapters.MainContenAdapter;
import com.program.himalaya.data.XimalayaDBHelper;
import com.program.himalaya.interfaces.IPlayerCallback;
import com.program.himalaya.presenters.PlayerPresenter;
import com.program.himalaya.presenters.RecommendPresenter;
import com.program.himalaya.utils.LogUtil;
import com.program.himalaya.views.RoundRectImageView;
import com.squareup.picasso.Picasso;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.track.Track;
import com.ximalaya.ting.android.opensdk.player.service.XmPlayListControl;

import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;

import java.util.List;

public class MainActivity extends FragmentActivity implements IPlayerCallback {
    private static final String TAG="MainActivity";
    private MagicIndicator magicIndicator;
    private ViewPager mcontentPager;
    private IndicatorAdapter mIndicatorAdapter;
    private RoundRectImageView mRoqudRectImageView;
    private TextView mHeaderTitle;
    private TextView mSubTitle;
    private ImageView mPlayControl;
    private PlayerPresenter mPlayerPresenter;
    private View mPlayControlItem;
    private View mSearchBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
      //
        initPresenter();
    }

    private void initPresenter() {
        mPlayerPresenter = PlayerPresenter.getPlayerPresenter();
        mPlayerPresenter.registerViewCallback(this);
    }

    private void initEvent() {
        mIndicatorAdapter.setOnIdicatorTapClickListener(new IndicatorAdapter.OnIdicatorTapClickListener() {
            @Override
            public void onTabClick(int index) {
                LogUtil.d(TAG,"click index -->"+index);
                if (mcontentPager!=null){
                    mcontentPager.setCurrentItem(index,false);
                }
            }
        });
        mPlayControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayerPresenter != null) {
                    boolean hasPlayList = mPlayerPresenter.hasPlayList();
                    if (!hasPlayList){
                            //??????????????????????????????????????????????????????????????????????????????
                        playFirstRecommend();
                    }else {
                        if (mPlayerPresenter.isPlay()) {
                            mPlayerPresenter.pause();
                        }else {
                            mPlayerPresenter.play();
                        }
                    }
                }

            }
        });
        mPlayControlItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hasPlayList=mPlayerPresenter.hasPlayList();
                if (!hasPlayList) {
                    playFirstRecommend();
                }
                //?????????????????????
                startActivity(new Intent(MainActivity.this,PlayActivity.class));
            }
        });

        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,SearchActivity.class);
                startActivity(intent);
            }
        });

    }

    /**
     * ?????????????????????
     */
    private void playFirstRecommend() {
        List<Album> currentRecommend = RecommendPresenter.getInstance().getCurrentRecommend();
        if (currentRecommend != null&&currentRecommend.size()>0) {
            Album album = currentRecommend.get(0);
            long albumId = album.getId();
            mPlayerPresenter.playByAlbumId(albumId);
        }
    }

    private void initView() {
         magicIndicator =this.findViewById(R.id.magic_indicator);
         magicIndicator.setBackgroundColor(this.getResources().getColor(R.color.main_color));
         //??????indicator????????????
        mIndicatorAdapter = new IndicatorAdapter(this);
        CommonNavigator commonNavigator = new CommonNavigator(this);
        commonNavigator.setAdjustMode(true);    //????????????????????????????????????
        commonNavigator.setAdapter(mIndicatorAdapter);
        //????????????????????????
        //ViewPager
        mcontentPager =this.findViewById(R.id.content_pager);

         //???????????????
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        MainContenAdapter mainContenAdapter=new MainContenAdapter(supportFragmentManager);
        mcontentPager.setAdapter(mainContenAdapter);
         //???ViewPagerHelper???IndicatorAdapter??????
        magicIndicator.setNavigator(commonNavigator);
        ViewPagerHelper.bind(magicIndicator,mcontentPager);

        //?????????????????????
        mRoqudRectImageView = this.findViewById(R.id.main_track_cover);
        mHeaderTitle = this.findViewById(R.id.main_head_title);
        mHeaderTitle.setSelected(true);                     //??????     ?????????
        mSubTitle = this.findViewById(R.id.main_sub_title);
        mPlayControl = this.findViewById(R.id.mian_play_control);
        mPlayControlItem = this.findViewById(R.id.main_play_control_item);

        //????????????
        mSearchBtn = this.findViewById(R.id.search_btn);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPlayerPresenter != null) {
            mPlayerPresenter.ungisterViewCallback(this);
        }
    }

    @Override
    public void onPlayStart() {
        undatePlayControl(true);
    }

    @Override
    public void onPlayPause() {
        undatePlayControl(false);
    }

    private void undatePlayControl(boolean isPlay){
        if (mPlayControl != null) {
            mPlayControl.setImageResource(isPlay?R.drawable.selector_palyer_pause :R.drawable.selector_player_play);
        }
    }

    @Override
    public void onPlayStop() {
        undatePlayControl(false);
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

    }

    @Override
    public void onPlayModeChange(XmPlayListControl.PlayMode playMode) {

    }

    @Override
    public void onProgressChange(int currentProgress, int total) {

    }

    @Override
    public void onAdLoading() {

    }

    @Override
    public void onAdFinished() {

    }

    @Override
    public void onTrackUpdate(Track track, int playIndex) {
        if (track != null) {
            String trackTitle = track.getTrackTitle();
            String nickname = track.getAnnouncer().getNickname();
            String coverUrlMiddle = track.getCoverUrlMiddle();
            if (mHeaderTitle != null) {
                mHeaderTitle.setText(trackTitle);
            }
            if (mSubTitle != null) {
                mSubTitle.setText(nickname);
            }
            if (coverUrlMiddle != null) {
                Picasso.with(this).load(coverUrlMiddle).into(mRoqudRectImageView);
            }
            LogUtil.d(TAG,"trackTitle="+trackTitle);
            LogUtil.d(TAG,"nickname="+nickname);
            LogUtil.d(TAG,"coverUrlMiddle="+coverUrlMiddle);
        }
    }

    @Override
    public void updateListOrder(boolean isReverse) {

    }
}