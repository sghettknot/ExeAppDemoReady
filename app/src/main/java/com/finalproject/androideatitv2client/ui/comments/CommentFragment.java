package com.finalproject.androideatitv2client.ui.comments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproject.androideatitv2client.Adapter.MyCommentAdapter;
import com.finalproject.androideatitv2client.Callback.ICommentCallbackListener;
import com.finalproject.androideatitv2client.Common.Common;
import com.finalproject.androideatitv2client.Model.CommentModel;
import com.finalproject.androideatitv2client.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class CommentFragment extends BottomSheetDialogFragment implements ICommentCallbackListener {

    private CommentViewModel commentViewModel;

    private Unbinder unbinder;

    @BindView(R.id.recycler_comment)
    RecyclerView recycle_comment;

    AlertDialog dialog;
    ICommentCallbackListener listener;

    public CommentFragment() { listener = this; }

    private static CommentFragment instance;

    public static CommentFragment getInstance() {
        if (instance == null)
            instance = new CommentFragment();
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View itemView = LayoutInflater.from(getContext())
                .inflate(R.layout.bottom_sheet_comment_fragment, container, false);
        unbinder = ButterKnife.bind(this, itemView);
        initViews();
        loadCommentsFromFirebase();
        commentViewModel.getMutableLiveDataFoodList().observe(this, commentModels -> {
            MyCommentAdapter adapter = new MyCommentAdapter(getContext(), commentModels, Common.selectedFood);
            recycle_comment.setAdapter(adapter);
        });
        return itemView;
    }

    private void loadCommentsFromFirebase() {
        dialog.show();
        List<CommentModel> commentModels = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference(Common.COMMENT_REF)
                .child(Common.selectedFood.getId())
                .orderByChild("serverTimeStamp")
                .limitToLast(100)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot commentSnapShot:dataSnapshot.getChildren()) {
                            CommentModel commentModel = commentSnapShot.getValue(CommentModel.class);
                            commentModels.add(commentModel);
                        }
                        listener.onCommentLoadSuccess(commentModels);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onCommentLoadFailed(databaseError.getMessage());
                    }
                });
    }

    private void initViews() {
        // commentViewModel = ViewModelProviers.of(this).get(CommentViewModel.class); DEPRECIATED
        commentViewModel = new ViewModelProvider(this).get(CommentViewModel.class);
        dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();

        recycle_comment.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, true);
        recycle_comment.setLayoutManager(layoutManager);
        recycle_comment.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));
    }

    @Override
    public void onCommentLoadSuccess(List<CommentModel> commentModels) {
        dialog.dismiss();
        commentViewModel.setCommentList(commentModels);
    }

    @Override
    public void onCommentLoadFailed(String message) {
        dialog.dismiss();
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
