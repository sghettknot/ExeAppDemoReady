package com.finalproject.androideatitv2client.ui.cart;

import android.app.SearchManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproject.androideatitv2client.Adapter.MyCartAdapter;
import com.finalproject.androideatitv2client.Common.Common;
import com.finalproject.androideatitv2client.Common.MySwipeHelper;
import com.finalproject.androideatitv2client.Database.CartDataSource;
import com.finalproject.androideatitv2client.Database.CartDatabase;
import com.finalproject.androideatitv2client.Database.CartItem;
import com.finalproject.androideatitv2client.Database.LocalCartDataSource;
import com.finalproject.androideatitv2client.EventBus.CounterCartEvent;
import com.finalproject.androideatitv2client.EventBus.FoodItemClick;
import com.finalproject.androideatitv2client.EventBus.HideFABCart;
import com.finalproject.androideatitv2client.EventBus.UpdateItemInCart;
import com.finalproject.androideatitv2client.Model.FoodModel;
import com.finalproject.androideatitv2client.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

public class CartFragment extends Fragment {

    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;
    private List<CartItem> searchResultList;
    private List<CartItem> cartItems;
    private List<Float> cartRatings;
    private boolean isSearch;


    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    //@BindView(R.id.txt_total_price)
    //TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    //@BindView(R.id.group_place_holder)
    //CardView group_place_holder;

    private MyCartAdapter adapter;

    private Unbinder unbinder;

    private  CartViewModel cartViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        cartViewModel =
                new ViewModelProvider(this).get(CartViewModel.class);

        searchResultList = new ArrayList<>();
        isSearch = false;

        View root = inflater.inflate(R.layout.fragment_cart, container, false);
        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItems().observe(getViewLifecycleOwner(),
                new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                if (cartItems == null || cartItems.isEmpty()) {
                    recycler_cart.setVisibility(View.GONE);
                    //group_place_holder.setVisibility(View.GONE);
                    txt_empty_cart.setVisibility(View.VISIBLE);
                } else {
                    recycler_cart.setVisibility(View.VISIBLE);
                    //group_place_holder.setVisibility(View.VISIBLE);
                    txt_empty_cart.setVisibility(View.GONE);

                    if (isSearch) {
                        adapter = new MyCartAdapter(getContext(), searchResultList);
                    } else {
                        adapter = new MyCartAdapter(getContext(), cartItems);
                    }
                    recycler_cart.setAdapter(adapter);
                    populateCartItems(cartItems);
                    getRatings();
                }
            }
        });
        unbinder = ButterKnife.bind(this, root);
        initViews();
        return root;
    }

    private void initViews() {

        setHasOptionsMenu(true);

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        EventBus.getDefault().postSticky(new HideFABCart(true));

        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_cart, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            cartDataSource.deleteCartItem(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(Integer integer) {
                                            adapter.notifyItemRemoved(pos);
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                            Toast.makeText(getContext(), "Delete item success!", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }));
            }
        };
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.action_settings).setVisible(false); // Hide home menu already inflate
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.cart_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.action_search_cart);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menuItem.getActionView();
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
            isSearch = false;
            EditText ed = (EditText) searchView.findViewById(R.id.search_src_text);
            // Clear text
            ed.setText("");
            // Collapse the action view
            searchView.setQuery("", false);
            searchView.setIconified(true);
            // Collapse the search widget
            //menuItem.collapseActionView();
            searchView.clearFocus();
            // Restore result to original
            cartViewModel.getMutableLiveDataCartItems();
        });
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_cart) {
            cartDataSource.cleanCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) { }

                        @Override
                        public void onSuccess(Integer integer) {
                            Toast.makeText(getContext(), "Clear favourite success", Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            return true;
        }

        if (item.getItemId() == R.id.action_sort_recent_cart) { sortRecent(); }
        if (item.getItemId() == R.id.action_sort_rate_cart) { sortRate(); }
        if (item.getItemId() == R.id.action_sort_alpha_asc_cart) { sortAlphAsc(); }
        if (item.getItemId() == R.id.action_sort_alpha_des_cart) { sortAlphDes(); }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().postSticky(new HideFABCart(false));
        cartViewModel.onStop();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateItemInCartEvent(UpdateItemInCart event) {
        if (event.getCartItem() != null) {
            // First, save state of Recycler View
            recyclerViewState = recycler_cart.getLayoutManager().onSaveInstanceState();
            cartDataSource.updateCartItems(event.getCartItem())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            //calculateTotalPrice();
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState); // fix error refresh recycler view after update
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(),"[UPDATE FAVOURITE]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void calculateTotalPrice() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double aLong) {
                        //txt_total_price.setText(new StringBuilder("Total: ")
                        //.append(Common.formatPrice(price)));
                        //Toast.makeText(getContext(), "HEY HEY HEY",Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getContext(), "[SUM CART]"+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void populateCartItems(List<CartItem> items) {
        cartItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            cartItems.add(items.get(i));
        }
    }


    private void startSearch(String s) {
        isSearch = true;
        searchResultList = new ArrayList<>();
        for (int i=0; i < adapter.getItemCount(); i++) {
            CartItem cartItem = adapter.getItemAtPosition(i);
            if (cartItem.getFoodName().toLowerCase().contains(s))
                searchResultList.add(cartItem);
        }
        cartViewModel.getMutableLiveDataCartItems().setValue(searchResultList);
    }


    public void sortRecent() {
        List<CartItem> listCartItem = new ArrayList<>();
        for (int i = 0; i < cartItems.size(); i++) {
            listCartItem.add(cartItems.get(i));
        }
        Collections.reverse(listCartItem);
        adapter = new MyCartAdapter(getContext(), listCartItem);
        recycler_cart.setAdapter(adapter);
    }


    public void sortRate() {
        if (cartRatings.size() != cartItems.size()) {
            return;
        }
        List<CartItem> sortedCartItems = new ArrayList<>();
        List<CartItem> copyCartItems = new ArrayList<>(cartItems);
        List<Float> copyCartRatings = new ArrayList<>(cartRatings);
        int cartSize = cartItems.size();
        for (int i = 0; i < cartSize; i++) {
            float max = 0;
            int index = 0;
            int remainedSize = copyCartItems.size();
            float rate = 0;
            for (int j = 0; j < remainedSize; j++) {
                rate = copyCartRatings.get(j);
                if (max < rate) {
                    max = rate;
                    index = j;
                }
            }
            sortedCartItems.add(copyCartItems.get(index));
            copyCartItems.remove(index);
            copyCartRatings.remove(index);
        }
        adapter = new MyCartAdapter(getContext(), sortedCartItems);
        recycler_cart.setAdapter(adapter);
    }


    public void sortAlphAsc() {
        List<Character> cartChars = new ArrayList<>();
        for (int i = 0; i < cartItems.size(); i++) {
            cartChars.add(Character.toLowerCase(cartItems.get(i).getFoodName().charAt(0)));
        }

        List<CartItem> sortedCartItems = new ArrayList<>();
        List<CartItem> copyCartItems = new ArrayList<>(cartItems);
        List<Character> copyCartChars = new ArrayList<>(cartChars);
        int cartSize = cartItems.size();

        for (int i = 0; i < cartSize; i++) {
            char min = 'z';
            int index = 0;
            char character;
            int remainedSize = copyCartItems.size();
            for (int j = 0; j < remainedSize; j++) {
                character = copyCartChars.get(j);
                if (min > character) {
                    min = character;
                    index = j;
                }
            }
            sortedCartItems.add(copyCartItems.get(index));
            copyCartItems.remove(index);
            copyCartChars.remove(index);
        }
        adapter = new MyCartAdapter(getContext(), sortedCartItems);
        recycler_cart.setAdapter(adapter);
    }


    public void sortAlphDes() {
        List<Character> cartChars = new ArrayList<>();
        for (int i = 0; i < cartItems.size(); i++) {
            cartChars.add(Character.toLowerCase(cartItems.get(i).getFoodName().charAt(0)));
        }

        List<CartItem> sortedCartItems = new ArrayList<>();
        List<CartItem> copyCartItems = new ArrayList<>(cartItems);
        List<Character> copyCartChars = new ArrayList<>(cartChars);
        int cartSize = cartItems.size();

        for (int i = 0; i < cartSize; i++) {
            char max = 'a';
            int index = 0;
            char character;
            int remainedSize = copyCartItems.size();
            for (int j = 0; j < remainedSize; j++) {
                character = copyCartChars.get(j);
                if (max < character) {
                    max = character;
                    index = j;
                }
            }
            sortedCartItems.add(copyCartItems.get(index));
            copyCartItems.remove(index);
            copyCartChars.remove(index);
        }
        adapter = new MyCartAdapter(getContext(), sortedCartItems);
        recycler_cart.setAdapter(adapter);
    }


    public void getRatings() {
        cartRatings = new ArrayList<>();
        for (int i = 0; i < cartItems.size(); i++) {

            CartItem cartItem = cartItems.get(i);
            String foodId = cartItem.getFoodId();
            String category = "";
            int number = 0;

            if (foodId.contains("food")) {
                category = "menu_01";
                number = Integer.parseInt(foodId.substring(5));
            }
            if (foodId.contains("bar")) {
                category = "menu_02";
                number = Integer.parseInt(foodId.substring(4));
            }
            if (foodId.contains("sports")) {
                category = "menu_03";
                number = Integer.parseInt(foodId.substring(7));
            }

            FirebaseDatabase
                    .getInstance()
                    .getReference("Category")
                    .child(category)
                    .child("foods")
                    .child((number - 1) + "")
                    .child("ratingValue")
                    .addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                float rate = snapshot.getValue(Float.class);
                                cartRatings.add(rate);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                        }
                    });
        }
    }
}
