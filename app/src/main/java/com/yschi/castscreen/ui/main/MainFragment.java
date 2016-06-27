package com.yschi.castscreen.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.yschi.castscreen.R;
import com.yschi.castscreen.service.discovery.events.EventDiscoveryClient;
import com.yschi.castscreen.ui.activity.AbstractPopableFragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by kevinleperf on 27/06/2016.
 */

public class MainFragment extends AbstractPopableFragment {

    private ArrayAdapter<String> mDiscoverAdapter;
    private MainActivity mMainActivity;

    @Bind(R.id.discover_listview)
    ListView mDiscoverListView;

    @Bind(R.id.discover_waiting)
    ViewGroup mDiscoverWaiting;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_waiting_receiver, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);

        mDiscoverAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1);

        mDiscoverListView.setAdapter(mDiscoverAdapter);

        mDiscoverAdapter.addAll(mMainActivity.keySet());

        if (mDiscoverAdapter.getCount() > 0) {
            mDiscoverWaiting.setVisibility(View.GONE);
        } else {
            mDiscoverWaiting.setVisibility(View.VISIBLE);
        }

        mDiscoverListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String name = mDiscoverAdapter.getItem(i);

                mMainActivity.setReceiverName(name);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MainActivity) {
            mMainActivity = (MainActivity) context;
        }
    }

    @Override
    public void onDetach() {
        mMainActivity = null;
        super.onDetach();
    }

    @Override
    protected boolean canEdit() {
        return false;
    }

    @Override
    protected String getTitle() {
        return getString(R.string.app_name);
    }

    @Override
    protected boolean hasList() {
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventDiscoveryClient event) {
        if (event != null) {
            mMainActivity.put(event.getName(), event.getIp());
            mDiscoverAdapter.clear();
            mDiscoverAdapter.addAll(mMainActivity.keySet());

            mDiscoverWaiting.setVisibility(View.GONE);
        }
    }
}
