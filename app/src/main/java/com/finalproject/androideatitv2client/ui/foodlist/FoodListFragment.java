package com.finalproject.androideatitv2client.ui.foodlist;

import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproject.androideatitv2client.Adapter.MyCartAdapter;
import com.finalproject.androideatitv2client.Adapter.MyFoodListAdapter;
import com.finalproject.androideatitv2client.Common.Common;
import com.finalproject.androideatitv2client.Database.CartItem;
import com.finalproject.androideatitv2client.EventBus.CounterCartEvent;
import com.finalproject.androideatitv2client.Model.CategoryModel;
import com.finalproject.androideatitv2client.Model.FoodModel;
import com.finalproject.androideatitv2client.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FoodListFragment extends Fragment {

    private FoodListViewModel foodListViewModel;

    Unbinder unbinder;
    @BindView(R.id.recycler_food_list)
    RecyclerView recycler_food_list;

    LayoutAnimationController layoutAnimationController;
    MyFoodListAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        foodListViewModel =
                new ViewModelProvider(this).get(FoodListViewModel.class);
        View root = inflater.inflate(R.layout.fragment_food_list, container, false);
        unbinder = ButterKnife.bind(this, root);
        initViews();
        foodListViewModel.getMutableLiveDataFoodList().observe(getViewLifecycleOwner(), foodModels -> {
            adapter = new MyFoodListAdapter(getContext(), foodModels);
            recycler_food_list.setAdapter(adapter);
            recycler_food_list.setLayoutAnimation(layoutAnimationController);
        });
        return root;
    }

    private void initViews() {

        ((AppCompatActivity)getActivity())
                .getSupportActionBar()
                .setTitle(Common.categorySelected.getName());

        setHasOptionsMenu(true);

        recycler_food_list.setHasFixedSize(true);
        recycler_food_list.setLayoutManager(new LinearLayoutManager(getContext()));

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);
    }


    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false); // Hide home menu already inflate
        menu.findItem(R.id.action_suggest).setVisible(false);
        super.onPrepareOptionsMenu(menu);
    }


    // Search Button
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.sort, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search_list);

        SearchManager searchManager = (SearchManager)getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView)menuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        // Event
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                startSearch(s);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        // Clear text when click to clear button on search view
        ImageView closeButton = (ImageView)searchView.findViewById(R.id.search_close_btn);
        closeButton.setOnClickListener(view -> {
            EditText ed = (EditText)searchView.findViewById(R.id.search_src_text);
            // Clear text
            ed.setText("");
            // Collapse the action view
            searchView.setQuery("", false);
            // Collapse the search widget
            menuItem.collapseActionView();
            searchView.clearFocus();
            // Restore result to original
            foodListViewModel.getMutableLiveDataFoodList();
        });
    }

    private void startSearch(String s) {
        List<FoodModel> resultList = new ArrayList<>();
        for (int i=0; i < Common.categorySelected.getFoods().size(); i++) {
            FoodModel foodModel = Common.categorySelected.getFoods().get(i);
            if (foodModel.getName().toLowerCase().contains(s))
                resultList.add(foodModel);
        }
        foodListViewModel.getMutableLiveDataFoodList().setValue(resultList);
    }



    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sort_recent_list) { sortRecent(); }
        if (item.getItemId() == R.id.action_sort_rate_list) { sortRate(); }
        if (item.getItemId() == R.id.action_sort_alpha_asc_list) { sortAlphAsc(); }
        if (item.getItemId() == R.id.action_sort_alpha_des_list) { sortAlphDes(); }
        return super.onOptionsItemSelected(item);
    }


    public void sortRecent() {
        List<FoodModel> foodModels = new ArrayList<>();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            foodModels.add(adapter.getItem(i));
        }
        Collections.reverse(foodModels);
        adapter = new MyFoodListAdapter(getContext(), foodModels);
        recycler_food_list.setAdapter(adapter);
    }


    public void sortRate() {
        List<FoodModel> sortedFoodModels = new ArrayList<>();
        List<FoodModel> copyFoodModels = new ArrayList<>();
        List<Double> foodRatings = new ArrayList<>();
        FoodModel foodModel = new FoodModel();
        int foodSize = adapter.getItemCount();

        for (int i = 0; i < foodSize; i++) {
            foodModel = adapter.getItem(i);
            copyFoodModels.add(foodModel);
            if (foodModel.getRatingValue() == null) {
                foodRatings.add(0.0);
            } else {
                foodRatings.add(foodModel.getRatingValue());
            }
        }

        for (int i = 0; i < foodSize; i++) {
            double max = 0;
            int index = 0;
            int remainedSize = copyFoodModels.size();
            double rate = 0;
            for (int j = 0; j < remainedSize; j++) {
                rate = foodRatings.get(j);
                if (max < rate) {
                    max = rate;
                    index = j;
                }
            }
            sortedFoodModels.add(copyFoodModels.get(index));
            copyFoodModels.remove(index);
            foodRatings.remove(index);
        }
        adapter = new MyFoodListAdapter(getContext(), sortedFoodModels);
        recycler_food_list.setAdapter(adapter);
    }


    public void sortAlphAsc() {
        List<Character> foodChars = new ArrayList<>();
        List<FoodModel> sortedFoodModels = new ArrayList<>();
        List<FoodModel> foodModels = new ArrayList<>();

        int foodSize = adapter.getItemCount();
        FoodModel foodModel;

        for (int i = 0; i < foodSize; i++) {
            foodModel = adapter.getItem(i);
            foodModels.add(foodModel);
            foodChars.add(Character.toLowerCase(foodModel.getName().charAt(0)));
        }

        for (int i = 0; i < foodSize; i++) {
            char min = 'z';
            int index = 0;
            char character;
            int remainedSize = foodModels.size();
            for (int j = 0; j < remainedSize; j++) {
                character = foodChars.get(j);
                if (min > character) {
                    min = character;
                    index = j;
                }
            }
            sortedFoodModels.add(foodModels.get(index));
            foodModels.remove(index);
            foodChars.remove(index);
        }
        adapter = new MyFoodListAdapter(getContext(), sortedFoodModels);
        recycler_food_list.setAdapter(adapter);
    }


    public void sortAlphDes() {
        List<Character> foodChars = new ArrayList<>();
        List<FoodModel> sortedFoodModels = new ArrayList<>();
        List<FoodModel> foodModels = new ArrayList<>();

        int foodSize = adapter.getItemCount();
        FoodModel foodModel;

        for (int i = 0; i < foodSize; i++) {
            foodModel = adapter.getItem(i);
            foodModels.add(foodModel);
            foodChars.add(Character.toLowerCase(foodModel.getName().charAt(0)));
        }

        for (int i = 0; i < foodSize; i++) {
            char max = 'a';
            int index = 0;
            char character;
            int remainedSize = foodModels.size();
            for (int j = 0; j < remainedSize; j++) {
                character = foodChars.get(j);
                if (max < character) {
                    max = character;
                    index = j;
                }
            }
            sortedFoodModels.add(foodModels.get(index));
            foodModels.remove(index);
            foodChars.remove(index);
        }
        adapter = new MyFoodListAdapter(getContext(), sortedFoodModels);
        recycler_food_list.setAdapter(adapter);
    }
}
