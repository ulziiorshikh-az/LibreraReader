package com.foobnix.ui2.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.FileMetaComparators;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.view.MyPopupMenu;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.PopupHelper;
import com.foobnix.pdf.search.activity.msg.MessageSyncFinish;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

public class CloudsFragment2 extends UIFragment<FileMeta> {
    public static final Pair<Integer, Integer> PAIR = new Pair<Integer, Integer>(R.string.clouds, R.drawable.glyphicons_2_cloud);
    FileMetaAdapter metaAdapter;
    ImageView onListGrid;
    View panelRecent;

    @Override
    public Pair<Integer, Integer> getNameAndIconRes() {
        return PAIR;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_clouds, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        panelRecent = view.findViewById(R.id.panelRecent);

        onListGrid = (ImageView) view.findViewById(R.id.onListGrid);
        onListGrid.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                popupMenu(onListGrid);
            }
        });

        metaAdapter = new FileMetaAdapter();
        metaAdapter.tempValue = FileMetaAdapter.TEMP_VALUE_FOLDER_PATH;
        bindAdapter(metaAdapter);
        bindAuthorsSeriesAdapter(metaAdapter);

        onGridList();
        populate();

        progressBar = view.findViewById(R.id.progressBar1);
        progressBar.setVisibility(View.GONE);
        TintUtil.setDrawableTint(progressBar.getIndeterminateDrawable().getCurrent(), Color.WHITE);

        view.findViewById(R.id.onRefresh).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                getActivity().startService(new Intent(getActivity(), BooksService.class).setAction(BooksService.ACTION_SYNC_DROPBOX));

            }
        });

        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);

        final View dropboxProvider = view.findViewById(R.id.dropbox);
        dropboxProvider.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                Clouds.get().loginToDropbox(getActivity(), new Runnable() {

                    @Override
                    public void run() {
                        dropboxProvider.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), R.string.success, Toast.LENGTH_SHORT).show();
                        getActivity().startService(new Intent(getActivity(), BooksService.class).setAction(BooksService.ACTION_SYNC_DROPBOX));
                    }
                });
            }
        });
        dropboxProvider.setVisibility(Clouds.get().isDropbox() ? View.GONE : View.VISIBLE);

        return view;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void notifyUpdateFragment(MessageSyncFinish event) {
        progressBar.setVisibility(View.GONE);
        populate();
    }

    @Override
    public void onTintChanged() {
        TintUtil.setBackgroundFillColor(panelRecent, TintUtil.color);
    }

    public boolean onBackAction() {
        return false;
    }

    @Override
    public List<FileMeta> prepareDataInBackground() {
        File root = new File(AppState.get().syncPath);
        File[] listFiles = root.listFiles();
        if (listFiles == null) {
            return new ArrayList<FileMeta>();
        }

        List<FileMeta> res = new ArrayList<FileMeta>();
        for (File file : listFiles) {
            if (file.isDirectory()) {
                continue;
            }
            FileMeta meta = FileMetaCore.createMetaIfNeed(file.getPath(), true);
            res.add(meta);
        }
        Collections.sort(res, FileMetaComparators.BY_DATE);
        Collections.reverse(res);

        return res;
    }

    @Override
    public void populateDataInUI(List<FileMeta> items) {
        if (metaAdapter != null) {
            metaAdapter.getItemsList().clear();
            metaAdapter.getItemsList().addAll(items);
            metaAdapter.notifyDataSetChanged();
        }
    }

    public void onGridList() {
        LOG.d("onGridList");
        onGridList(AppState.get().cloudMode, onListGrid, metaAdapter, null);
    }

    private void popupMenu(final ImageView onGridList) {
        MyPopupMenu p = new MyPopupMenu(getActivity(), onGridList);
        PopupHelper.addPROIcon(p, getActivity());

        List<Integer> names = Arrays.asList(R.string.list, R.string.compact, R.string.grid, R.string.cover);
        final List<Integer> icons = Arrays.asList(R.drawable.glyphicons_114_justify, R.drawable.glyphicons_114_justify_compact, R.drawable.glyphicons_156_show_big_thumbnails, R.drawable.glyphicons_157_show_thumbnails);
        final List<Integer> actions = Arrays.asList(AppState.MODE_LIST, AppState.MODE_LIST_COMPACT, AppState.MODE_GRID, AppState.MODE_COVERS);

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            p.getMenu().add(names.get(i)).setIcon(icons.get(i)).setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    AppState.get().cloudMode = actions.get(index);
                    onGridList.setImageResource(icons.get(index));
                    onGridList();
                    return false;
                }
            });
        }

        p.show();
    }

    @Override
    public void notifyFragment() {
        populate();
    }

    @Override
    public void resetFragment() {
        onGridList();
        populate();
    }

}