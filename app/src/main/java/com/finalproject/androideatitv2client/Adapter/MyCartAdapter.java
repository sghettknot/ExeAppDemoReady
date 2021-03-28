package com.finalproject.androideatitv2client.Adapter;

import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.finalproject.androideatitv2client.Common.Common;
import com.finalproject.androideatitv2client.Database.CartItem;
import com.finalproject.androideatitv2client.EventBus.FoodItemClick;
import com.finalproject.androideatitv2client.EventBus.UpdateItemInCart;
import com.finalproject.androideatitv2client.HomeActivity;
import com.finalproject.androideatitv2client.Model.FoodModel;
import com.finalproject.androideatitv2client.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyCartAdapter extends RecyclerView.Adapter<MyCartAdapter.MyViewHolder> {

    Context context;
    List<CartItem> cartItemList;

    public MyCartAdapter(Context context, List<CartItem> cartItemList) {
        this.context = context;
        this.cartItemList = cartItemList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_cart_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(cartItemList.get(position).getFoodImage())
                .into(holder.img_cart);
        holder.txt_food_name.setText(new StringBuilder(cartItemList.get(position).getFoodName()));
        holder.img_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickItem(position);
            }
        });
        holder.txt_food_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickItem(position);
            }
        });

        /* COMMENT
        holder.txt_food_price.setText(new StringBuilder("")
        .append(cartItemList.get(position).getFoodPrice() + cartItemList.get(position).getFoodExtraPrice()));

        holder.numberButton.setNumber(String.valueOf(cartItemList.get(position).getFoodQuantity()));

        // Event
        holder.numberButton.setOnValueChangeListener(new ElegantNumberButton.OnValueChangeListener() {
            @Override
            public void onValueChange(ElegantNumberButton view, int oldValue, int newValue) {
                // When user click this button, we will update database
                cartItemList.get(position).setFoodQuantity(newValue);
                EventBus.getDefault().postSticky(new UpdateItemInCart(cartItemList.get(position)));
            }
        });
         END OF COMMENT*/
    }


    public void clickItem(int position) {
        //going to set the selectedFood in order to jump to the detail fragment
        // first of all, we should find the menu address of the place
        CartItem cartItem = cartItemList.get(position);
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
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Common.selectedFood = snapshot.getValue(FoodModel.class);
                            Common.selectedFood.setKey(snapshot.getKey());
                            EventBus.getDefault().postSticky(new FoodItemClick(true, Common.selectedFood));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public CartItem getItemAtPosition(int pos) {
        return cartItemList.get(pos);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private Unbinder unbinder;
        @BindView(R.id.img_cart)
        ImageView img_cart;
        /* COMMENT
        @BindView(R.id.txt_food_price)
        TextView txt_food_price;
        END OF COMMENT */
        @BindView(R.id.txt_food_name)
        TextView txt_food_name;
        /* COMMENT
        @BindView(R.id.number_button)
        ElegantNumberButton numberButton;
        END OF COMMENT
         */
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }
}
