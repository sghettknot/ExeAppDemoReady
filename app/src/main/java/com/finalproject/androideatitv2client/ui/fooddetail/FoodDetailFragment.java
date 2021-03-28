package com.finalproject.androideatitv2client.ui.fooddetail;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.andremion.counterfab.CounterFab;
import com.bumptech.glide.Glide;
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton;
import com.finalproject.androideatitv2client.Common.Common;
import com.finalproject.androideatitv2client.Database.CartDataSource;
import com.finalproject.androideatitv2client.Database.CartDatabase;
import com.finalproject.androideatitv2client.Database.CartItem;
import com.finalproject.androideatitv2client.Database.LocalCartDataSource;
import com.finalproject.androideatitv2client.EventBus.CounterCartEvent;
import com.finalproject.androideatitv2client.Model.CommentModel;
import com.finalproject.androideatitv2client.Model.FoodModel;
import com.finalproject.androideatitv2client.R;
import com.finalproject.androideatitv2client.ui.comments.CommentFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.w3c.dom.Comment;
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FoodDetailFragment extends Fragment {

    private FoodDetailViewModel foodDetailViewModel;

    // 15: Update Cart
    private CartDataSource cartDataSource;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Unbinder unbinder;
    private android.app.AlertDialog waitingDialog;

    @BindView(R.id.img_food)
    ImageView img_food;
    @BindView(R.id.btnCart)
    CounterFab btnCart;
    @BindView(R.id.btn_rating)
    FloatingActionButton btn_rating;
    @BindView(R.id.food_name)
    TextView food_name;
    @BindView(R.id.food_description)
    TextView food_description;
    // COMMENT OUT
    //@BindView(R.id.food_price)
    //TextView food_price;
    //@BindView(R.id.number_button)
    //ElegantNumberButton numberButton;
    @BindView(R.id.ratingBar)
    RatingBar ratingBar;
    @BindView(R.id.btnShowComment)
    Button btnShowComment;

    @OnClick(R.id.btn_rating)
    void onRatingButtonClick() {
         showDialogRating();
    }

    @OnClick(R.id.btnShowComment)
    void onShowCommentButtonClick() {
        CommentFragment commentFragment = new CommentFragment().getInstance();
        commentFragment.show(getActivity().getSupportFragmentManager(), "CommentFragment");
    }

    // 15: Improve Cart System
    @OnClick(R.id.btnCart)
    void onCartItemAdd() {
        CartItem cartItem = new CartItem();
        cartItem.setUid(Common.currentUser.getUid());
        cartItem.setUserPhone(Common.currentUser.getPhone());

        cartItem.setFoodId(Common.selectedFood.getId());
        cartItem.setFoodName(Common.selectedFood.getName());
        cartItem.setFoodImage(Common.selectedFood.getImage());

        /* START COMMENT
        cartItem.setFoodPrice(Double.valueOf(String.valueOf(Common.selectedFood.getPrice())));
        cartItem.setFoodQuantity(1);
        cartItem.setFoodExtraPrice(Common.calculatedExtraPrice(Common.selectedFood.getUserSelectedSize(), Common.selectedFood.getUserSelectedAddon()));
        if (Common.selectedFood.getUserSelectedAddon() != null)
            cartItem.setFoodAddon(new Gson().toJson(Common.selectedFood.getUserSelectedAddon()));
        else
            cartItem.setFoodAddon("Default");
        if (Common.selectedFood.getUserSelectedSize() != null)
            cartItem.setFoodSize(new Gson().toJson(Common.selectedFood.getUserSelectedSize()));
        else
            cartItem.setFoodSize("Default");
        END OF COMMENT */
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
                                            Toast.makeText(getContext(), "Update Favourite success", Toast.LENGTH_SHORT).show();
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                        }

                                        @Override
                                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                                            Toast.makeText(getContext(), "[UPDATE CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // item not available in cart before, insert new
                            compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(() -> {
                                        Toast.makeText(getContext(),"Add to Favourite success", Toast.LENGTH_SHORT).show();
                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                    }, throwable -> {
                                        Toast.makeText(getContext(),"[CART ERROR]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(getContext(),"Add to Favourite success", Toast.LENGTH_SHORT).show();
                                        EventBus.getDefault().postSticky(new CounterCartEvent(true));
                                    }, throwable -> {
                                        Toast.makeText(getContext(),"[CART ERROR]"+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                    }));
                        } else
                            Toast.makeText(getContext(), "[GET CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDialogRating() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Rating Food");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_rating, null);

        RatingBar ratingBar = (RatingBar)itemView.findViewById(R.id.rating_bar);
        EditText edt_comment = (EditText)itemView.findViewById(R.id.edt_comment);

        builder.setView(itemView);
        builder.setNegativeButton("CANCEL", (dialog, i) -> {
        });
        builder.setPositiveButton("OK", (dialog, i) -> {
            CommentModel commentModel = new CommentModel();
            commentModel.setName(Common.currentUser.getName());
            commentModel.setUid(Common.currentUser.getUid());
            commentModel.setComment(edt_comment.getText().toString());
            commentModel.setRatingValue(ratingBar.getRating());
            Map<String, Object> serverTimeStamp = new HashMap<>();
            serverTimeStamp.put("timeStamp", ServerValue.TIMESTAMP);
            commentModel.setCommentTimeStamp(serverTimeStamp);

            foodDetailViewModel.setCommentModel(commentModel);

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        foodDetailViewModel =
                new ViewModelProvider(this).get(FoodDetailViewModel.class);
        View root = inflater.inflate(R.layout.fragment_food_detail, container, false);
        unbinder = ButterKnife.bind(this, root);
        initViews();
        foodDetailViewModel.getMutableLiveDataFood().observe(this, foodModel -> {
            displayInfo(foodModel);
        });
        foodDetailViewModel.getMutableLiveDataComment().observe(this, commentModel -> {
            submitRatingToFirebase(commentModel);
        });
        return root;
    }

    private void initViews() {
        // 15
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        waitingDialog = new SpotsDialog.Builder().setCancelable(false).setContext(getContext()).build();
    }

    private void submitRatingToFirebase(CommentModel commentModel) {
        waitingDialog.show();
        // Submit to Comments Ref
        FirebaseDatabase.getInstance()
                .getReference(Common.COMMENT_REF)
                .child(Common.selectedFood.getId())
                .push()
                .setValue(commentModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // After submit to CommentRef, update value aveger in Food
                        addRatingToFood(commentModel.getRatingValue());
                    }
                    waitingDialog.dismiss();
                });
    }

    private void addRatingToFood(float ratingValue) {
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())    // Select category
                .child("foods") // Select array list "foods" of this category
                .child(Common.selectedFood.getKey()) // Because food item is array list so key is index of array list
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            FoodModel foodModel = dataSnapshot.getValue(FoodModel.class);
                            foodModel.setKey(Common.selectedFood.getKey());

                            // Apply rating
                            if (foodModel.getRatingValue() == null)
                                foodModel.setRatingValue(0d);
                            if (foodModel.getRatingCount() == null)
                                foodModel.setRatingCount(0l);   // l = L lower case
                            double sumRating = foodModel.getRatingValue() + ratingValue;
                            long ratingCount = foodModel.getRatingCount() + 1;

                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("ratingValue", sumRating);
                            updateData.put("ratingCount", ratingCount);

                            // update data in variable
                            foodModel.setRatingValue(sumRating);
                            foodModel.setRatingCount(ratingCount);

                            dataSnapshot.getRef()
                                    .updateChildren(updateData)
                                    .addOnCompleteListener(task -> {
                                        waitingDialog.dismiss();
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getContext(), "Thank you!", Toast.LENGTH_SHORT).show();
                                            Common.selectedFood = foodModel;
                                            foodDetailViewModel.setFoodModel(foodModel); // Call Refresh
                                        }
                                    });
                        } else
                            waitingDialog.dismiss();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        waitingDialog.dismiss();
                        Toast.makeText(getContext(), ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayInfo(FoodModel foodModel) {
        Glide.with(getContext()).load(foodModel.getImage()).into(img_food);
        food_name.setText(new StringBuilder(foodModel.getName()));
        food_description.setText(new StringBuilder(foodModel.getDescription()));
        // COMMENT OUT
        //food_price.setText(new StringBuilder(foodModel.getPrice().toString()));

        // Refresh Rating Bar after submitting a rating
        if (foodModel.getRatingValue() != null)
            ratingBar.setRating(foodModel.getRatingValue().floatValue() / foodModel.getRatingCount());

        ((AppCompatActivity)getActivity())
                .getSupportActionBar()
                .setTitle(Common.selectedFood.getName());

    }

    @Override
    public void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }
}