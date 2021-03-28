package com.finalproject.androideatitv2client.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asksira.loopingviewpager.LoopingViewPager;
import com.finalproject.androideatitv2client.Adapter.MyBestDealsAdapter;
import com.finalproject.androideatitv2client.Adapter.MyPopularCategoriesAdapter;
import com.finalproject.androideatitv2client.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;

    Unbinder unbinder;

    @BindView(R.id.recycler_popular)
    RecyclerView recycle_popular;
    @BindView(R.id.viewpager)
    LoopingViewPager viewPager;

    LayoutAnimationController layoutAnimationController;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        unbinder = ButterKnife.bind(this, root);
        init();
        homeViewModel.getPopularList().observe(this, popularCategoryModels -> {

            // create adapter
            MyPopularCategoriesAdapter adapter = new MyPopularCategoriesAdapter(getContext(), popularCategoryModels);
            recycle_popular.setAdapter(adapter);
            recycle_popular.setLayoutAnimation(layoutAnimationController);
        });

        homeViewModel.getBestDealList().observe(this, bestDealModels -> {
            MyBestDealsAdapter adapter = new MyBestDealsAdapter(getContext(), bestDealModels, true);
            viewPager.setAdapter(adapter);
        });

        return root;
    }

    private void init() {
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(),R.anim.layout_item_from_left);
        recycle_popular.setHasFixedSize(true);
        recycle_popular.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));

    }

    @Override
    public void onResume() {
        super.onResume();
        viewPager.resumeAutoScroll();
    }

    @Override
    public void onPause() {
        viewPager.pauseAutoScroll();
        super.onPause();
    }
}