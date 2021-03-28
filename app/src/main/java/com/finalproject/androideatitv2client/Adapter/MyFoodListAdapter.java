package com.finalproject.androideatitv2client.Adapter;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.finalproject.androideatitv2client.Callback.IRecyclerClickListener;
import com.finalproject.androideatitv2client.Common.Common;
import com.finalproject.androideatitv2client.Database.CartDataSource;
import com.finalproject.androideatitv2client.Database.CartDatabase;
import com.finalproject.androideatitv2client.Database.CartItem;
import com.finalproject.androideatitv2client.Database.LocalCartDataSource;
import com.finalproject.androideatitv2client.EventBus.CounterCartEvent;
import com.finalproject.androideatitv2client.EventBus.FoodItemClick;
import com.finalproject.androideatitv2client.Model.FoodModel;
import com.finalproject.androideatitv2client.R;
import com.finalproject.androideatitv2client.ui.comments.CommentFragment;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Scheduler;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MyFoodListAdapter extends RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder> {

    private Context context;
    private List<FoodModel> foodModelList;
    private CompositeDisposable compositeDisposable;
    private CartDataSource cartDataSource;

    public MyFoodListAdapter(Context context, List<FoodModel> foodModelList) {
        this.context = context;
        this.foodModelList = foodModelList;
        this.compositeDisposable = new CompositeDisposable();
        this.cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(context).cartDAO());
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
        .inflate(R.layout.layout_food_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(foodModelList.get(position).getImage()).into(holder.img_food_image);
        // COMMENT OUT
        //holder.txt_food_price.setText(new StringBuilder("$")
        //.append(foodModelList.get(position).getPrice()));
        holder.txt_food_name.setText(new StringBuilder("")
        .append(foodModelList.get(position).getName()));

        // Event
        holder.setListener((view, pos) -> {
            Common.selectedFood = foodModelList.get(pos);
            Common.selectedFood.setKey(String.valueOf(pos));
            EventBus.getDefault().postSticky(new FoodItemClick(true, foodModelList.get(pos)));
        });

        holder.img_cart.setOnClickListener(view -> {
            CartItem cartItem = new CartItem();
            cartItem.setUid(Common.currentUser.getUid());
            cartItem.setUserPhone(Common.currentUser.getPhone());

            cartItem.setFoodId(foodModelList.get(position).getId());
            cartItem.setFoodName(foodModelList.get(position).getName());
            cartItem.setFoodImage(foodModelList.get(position).getImage());
            cartItem.setFoodQuantity(1);
            cartItem.setFoodExtraPrice(0.0);
            cartItem.setFoodAddon("Default");
            cartItem.setFoodSize("Default");

            cartDataSource.getItemWithAllOptionInCart(Common.currentUser.getUid(),
                    cartItem.getFoodId(),
                    cartItem.getFoodSize(),
                    cartItem.getFoodAddon())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<CartItem>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(CartItem cartItemFromDB) {
                            if (cartItemFromDB.equals(cartItem)) {
                                // already in database, just update
                                cartItemFromDB.setFoodExtraPrice(cartItem.getFoodExtraPrice());
                                cartItemFromDB.setFoodAddon(cartItem.getFoodAddon());
                                cartItemFromDB.setFoodSize(cartItem.getFoodSize());
                                cartItemFromDB.setFoodQuantity(cartItem.getFoodQuantity());

                                cartDataSource.updateCartItems(cartItemFromDB)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new SingleObserver<Integer>() {
                                            @Override
                                            public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                                            }

                                            @Override
                                            public void onSuccess(@io.reactivex.annotations.NonNull Integer integer) {
                                                Toast.makeText(context, "Update Favourite success", Toast.LENGTH_SHORT).show();
                                                EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                            }

                                            @Override
                                            public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                                Toast.makeText(context, "[UPDATE FAVOURITE]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                // item not available in cart before, insert new
                                compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(() -> {
                                            Toast.makeText(context,"Add to Favourite success", Toast.LENGTH_SHORT).show();
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                        }, throwable -> {
                                            Toast.makeText(context,"[FAVOURITE ERROR]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                        }));
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (e.getMessage().contains("empty")) {
                                // Default, if cart is empty, this code will be fired
                                compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(() -> {
                                            Toast.makeText(context,"Add to Favourite success", Toast.LENGTH_SHORT).show();
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                        }, throwable -> {
                                            Toast.makeText(context,"[CART ERROR]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                        }));
                            } else
                                Toast.makeText(context, "[GET FAVOURITE]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    @Override
    public int getItemCount() {
        return foodModelList.size();
    }


    public FoodModel getItem(int position) {
        return foodModelList.get(position);
    }


    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private Unbinder unbinder;
        @BindView(R.id.txt_food_name)
        TextView txt_food_name;

        // COMMENT OUT
        //@BindView(R.id.txt_food_price)
        //TextView txt_food_price;
        @BindView(R.id.img_food_image)
        ImageView img_food_image;

        @BindView(R.id.img_quick_cart)
        ImageView img_cart;

        IRecyclerClickListener listener;

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onItemClickListener(view, getAdapterPosition());
        }
    }
}
