package com.program.himalaya;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lcodecore.tkrefreshlayout.RefreshListenerAdapter;
import com.lcodecore.tkrefreshlayout.TwinklingRefreshLayout;
import com.program.himalaya.adapters.AlbumListAdapter;
import com.program.himalaya.adapters.SearchRecommendAdatpter;
import com.program.himalaya.base.BaseActivity;
import com.program.himalaya.interfaces.ISeacherCallback;
import com.program.himalaya.presenters.AlbumDetailPresenter;
import com.program.himalaya.presenters.SearchPresenter;
import com.program.himalaya.utils.LogUtil;
import com.program.himalaya.views.FlowTextLayout;
import com.program.himalaya.views.UILoader;
import com.ximalaya.ting.android.opensdk.model.album.Album;
import com.ximalaya.ting.android.opensdk.model.word.HotWord;
import com.ximalaya.ting.android.opensdk.model.word.QueryResult;

import net.lucode.hackware.magicindicator.buildins.UIUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchActivity extends BaseActivity implements ISeacherCallback, AlbumListAdapter.OnAlbumItemClickListener {

    private static final String TAG = "SearchActivity";
    private View mBackBtn;
    private EditText mInputBox;
    private View mSearchBtn;
    private FrameLayout mReaultContainer;
    private SearchPresenter mSearchPresenter;
    private UILoader mUILoader;
    private RecyclerView mResultListView;
    private AlbumListAdapter mAlbumListAdapter;
    private FlowTextLayout mFlowTextLayout;
    private InputMethodManager mImm;
    private View mDelbtn;
    public static final int TIME_SHOW_IMM = 500;
    private RecyclerView mSearchRecomendList;
    private SearchRecommendAdatpter mRecommendAdapter;
    private TwinklingRefreshLayout mRefreshLayout;
    private boolean mNeedSuggestWords = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initView();
        initEvent();
        initPresenter();
    }

    private void initPresenter() {
        //??????UI????????????
        mSearchPresenter = SearchPresenter.getSearchPresenter();
        mSearchPresenter.registerViewCallback(this);
        //????????????
        mSearchPresenter.getHotWord();
        mImm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSearchPresenter != null) {
            //??????UI???????????????
            mSearchPresenter.ungisterViewCallback(this);
            mSearchPresenter = null;
        }
    }

    private void initEvent() {
        mAlbumListAdapter.setAlbumItemClickListener(this);
        mRefreshLayout.setOnRefreshListener(new RefreshListenerAdapter() {
            @Override
            public void onLoadMore(TwinklingRefreshLayout refreshLayout) {
                LogUtil.d(TAG, "load more...");
                //??????????????????
                if (mSearchPresenter != null) {
                    mSearchPresenter.loadMore();
                }

            }
        });
        if (mRecommendAdapter != null) {
            mRecommendAdapter.setItemClickListener(new SearchRecommendAdatpter.ItemClickListener() {
                @Override
                public void onItemClick(String keyword) {
                    LogUtil.d(TAG, "mRecommendAdapter keyword -->" + keyword);
                    //??????
                    //?????????????????????
                    //???????????????????????????
                    mNeedSuggestWords = false;
                    switch2Search(keyword);
                }
            });
        }
        mDelbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInputBox.setText("");
            }
        });
        mFlowTextLayout.setClickListener(new FlowTextLayout.ItemClickListener() {
            @Override
            public void onItemClick(String text) {
                //???????????????????????????
                mNeedSuggestWords = false;
                switch2Search(text);
            }
        });
        mUILoader.setOnRetryClickListener(new UILoader.OnRetryClickListener() {
            @Override
            public void onRetryClick() {
                if (mSearchPresenter != null) {
                    mSearchPresenter.reSearch();
                    mUILoader.updateStatus(UILoader.UIStatus.LOADING);
                }
            }
        });
        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //???????????????
                String keyword = mInputBox.getText().toString().trim();
                if (TextUtils.isEmpty(keyword)) {
                    Toast.makeText(SearchActivity.this,"???????????????????????????",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mSearchPresenter != null) {
                    mSearchPresenter.deSearch(keyword);
                    mUILoader.updateStatus(UILoader.UIStatus.LOADING);
                }
            }
        });
        mInputBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (TextUtils.isEmpty(s)) {
                    mSearchPresenter.getHotWord();
                    mDelbtn.setVisibility(View.GONE);
                } else {
                    mDelbtn.setVisibility(View.VISIBLE);
                    if (mNeedSuggestWords) {
                        //??????????????????
                        getSuggestWord(s.toString());
                    }else {
                        mNeedSuggestWords=true;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void switch2Search(String text) {
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(SearchActivity.this,"???????????????????????????",Toast.LENGTH_SHORT).show();
            return;
        }
        //1.????????????????????????
        mInputBox.setText(text);
        //??????????????????
        mInputBox.setSelection(text.length());
        //2.????????????
        if (mSearchPresenter != null) {
            mSearchPresenter.deSearch(text);
        }
        //????????????
        if (mUILoader != null) {
            mUILoader.updateStatus(UILoader.UIStatus.LOADING);
        }
    }

    /**
     * ????????????????????????
     *
     * @param keyword
     */
    private void getSuggestWord(String keyword) {
        LogUtil.d(TAG, "getSuggestWord -->" + keyword);
        if (mSearchPresenter != null) {
            mSearchPresenter.getRecommendWord(keyword);
        }

    }

    private void initView() {
        mBackBtn = this.findViewById(R.id.search_back);
        mInputBox = this.findViewById(R.id.search_input);
        mDelbtn = this.findViewById(R.id.search_input_delete);
        mDelbtn.setVisibility(View.GONE);
        mInputBox.postDelayed(new Runnable() {
            @Override
            public void run() {
                mInputBox.requestFocus();
                mImm.showSoftInput(mInputBox, InputMethodManager.SHOW_IMPLICIT);
            }
        }, TIME_SHOW_IMM);
        mSearchBtn = this.findViewById(R.id.search_btn);
        mReaultContainer = this.findViewById(R.id.search_container);
        if (mUILoader == null) {
            mUILoader = new UILoader(this) {
                @Override
                protected View getSuccessView(ViewGroup container) {
                    return createSuccessView();
                }
                @Override
                protected View getEmptyView() {
                    //??????????????????
                    View emptyView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_empty_view,this,false);
                    TextView tipsView = emptyView.findViewById(R.id.empty_view_tips_tv);
                    tipsView.setText(R.string.search_no_sub_content_tips_text);
                    return emptyView;
                }
            };
            if (mUILoader.getParent() instanceof ViewGroup) {
                ((ViewGroup) mUILoader.getParent()).removeView(mUILoader);
            }
            mReaultContainer.addView(mUILoader);
        }
    }

    /**
     * ?????????????????????view
     *
     * @return
     */
    private View createSuccessView() {
        View resultView = LayoutInflater.from(this).inflate(R.layout.search_result_layout, null);
        //????????????
        mRefreshLayout = resultView.findViewById(R.id.search_result_refresh_layout);
        mRefreshLayout.setEnableRefresh(false); //??????????????????
        //????????????
        mFlowTextLayout = resultView.findViewById(R.id.recommend_hot_word_view);

        mResultListView = resultView.findViewById(R.id.result_list_view);
        //?????????????????????
        LinearLayoutManager resultLayoutManager = new LinearLayoutManager(this);
        mResultListView.setLayoutManager(resultLayoutManager);
        //???????????????
        mAlbumListAdapter = new AlbumListAdapter();
        mResultListView.setAdapter(mAlbumListAdapter);
//????????????
        mResultListView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.top = UIUtil.dip2px(view.getContext(), 5);    //px-->dp
                outRect.bottom = UIUtil.dip2px(view.getContext(), 5);
                outRect.left = UIUtil.dip2px(view.getContext(), 5);
                outRect.right = UIUtil.dip2px(view.getContext(), 5);
            }
        });

        //????????????
        mSearchRecomendList = resultView.findViewById(R.id.search_recommend_list);
        //?????????????????????
        LinearLayoutManager recommendLayoutManger = new LinearLayoutManager(this);
        mSearchRecomendList.setLayoutManager(recommendLayoutManger);
        mSearchRecomendList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.top = UIUtil.dip2px(view.getContext(), 2);    //px-->dp
                outRect.bottom = UIUtil.dip2px(view.getContext(), 2);
                outRect.left = UIUtil.dip2px(view.getContext(), 5);
                outRect.right = UIUtil.dip2px(view.getContext(), 5);
            }
        });
        //???????????????
        mRecommendAdapter = new SearchRecommendAdatpter();
        mSearchRecomendList.setAdapter(mRecommendAdapter);
        return resultView;
    }

    @Override
    public void onSearchResultLoaded(List<Album> result) {
        handlerSearchResult(result);
        //????????????
        mImm.hideSoftInputFromWindow(mInputBox.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void handlerSearchResult(List<Album> result) {
        hideSuccessView();
        mRefreshLayout.setVisibility(View.VISIBLE);
        if (result != null) {
            if (result.size() == 0) {
                //????????????
                if (mUILoader != null) {
                    mUILoader.updateStatus(UILoader.UIStatus.EMPTY);
                }
            } else {
                //??????????????????????????????
                mAlbumListAdapter.setData(result);
                mUILoader.updateStatus(UILoader.UIStatus.SUCCESS);
            }
        }
    }

    @Override
    public void onHotWordLoaded(List<HotWord> hotWordList) {
        hideSuccessView();
        mFlowTextLayout.setVisibility(View.VISIBLE);
        if (mUILoader != null) {
            mUILoader.updateStatus(UILoader.UIStatus.SUCCESS);
        }
        LogUtil.d(TAG, "hotWordList size -->" + hotWordList.size());
        LogUtil.d(TAG, "hotWordList--->" + hotWordList);
        List<String> hotWords = new ArrayList<>();
        hotWords.clear();
        for (HotWord hotWord : hotWordList) {
            String searchWord = hotWord.getSearchword();
            LogUtil.d(TAG, "hotWordList hotWord.getSearchword()--->" + hotWord.getSearchword());
            hotWords.add(searchWord);
        }
        LogUtil.d(TAG, "hotWords=" + hotWords);
        Collections.sort(hotWords);
        mFlowTextLayout.setTextContents(hotWords);
    }

    @Override
    public void onLoadMoreResult(List<Album> result, boolean isOkay) {
        //???????????????????????????
        if (mRefreshLayout != null) {
            mRefreshLayout.finishLoadmore();
        }
        if (isOkay) {
            handlerSearchResult(result);
        } else {
            Toast.makeText(SearchActivity.this, "??????????????????", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRecommendWordLoaded(List<QueryResult> keyWordList) {
        //?????????????????????
        if (mRecommendAdapter != null) {
            mRecommendAdapter.setData(keyWordList);
        }
        LogUtil.d(TAG, "keyWordList size ==>" + keyWordList.size());

        //??????UI????????????????????????
        if (mUILoader != null) {
            mUILoader.updateStatus(UILoader.UIStatus.SUCCESS);
        }
        //?????????????????????
        hideSuccessView();
        mSearchRecomendList.setVisibility(View.VISIBLE);
    }

    @Override
    public void onError(int errorCode, String errorMsg) {
        if (mUILoader != null) {
            mUILoader.updateStatus(UILoader.UIStatus.NETWORK_ERROR);
        }
    }

    private void hideSuccessView() {
        mSearchRecomendList.setVisibility(View.GONE);
        mRefreshLayout.setVisibility(View.GONE);
        mFlowTextLayout.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(int position, Album album) {
        //?????????????????????

        AlbumDetailPresenter.getInstance().setTargetAlbum(album);
        //item?????????,?????????????????????
        Intent intent = new Intent(this, DetailActivity.class);
        startActivity(intent);
    }
}