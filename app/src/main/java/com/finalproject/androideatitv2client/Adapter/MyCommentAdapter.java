package com.finalproject.androideatitv2client.Adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.finalproject.androideatitv2client.Email.SendEmail;
import com.finalproject.androideatitv2client.Model.CommentModel;
import com.finalproject.androideatitv2client.Model.FoodModel;
import com.finalproject.androideatitv2client.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyCommentAdapter extends RecyclerView.Adapter<MyCommentAdapter.MyViewHolder> {

    Context context;
    List<CommentModel> commentModelList;
    FoodModel selectedFood;

    public MyCommentAdapter(Context context, List<CommentModel> commentModelList, FoodModel selectedFood) {
        this.context = context;
        this.commentModelList = commentModelList;
        this.selectedFood = selectedFood;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_comment_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Long timeStamp = Long.valueOf(commentModelList.get(position).getCommentTimeStamp().get("timeStamp").toString());
        holder.txt_comment_date.setText(DateUtils.getRelativeTimeSpanString(timeStamp));
        holder.txt_comment.setText(commentModelList.get(position).getComment());
        holder.txt_comment_name.setText(commentModelList.get(position).getName());
        holder.ratingBar.setRating(commentModelList.get(position).getRatingValue());
        holder.reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                report(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private Unbinder unbinder;

        @BindView(R.id.txt_comment_date)
        TextView txt_comment_date;
        @BindView(R.id.txt_comment)
        TextView txt_comment;
        @BindView(R.id.txt_comment_name)
        TextView txt_comment_name;
        @BindView(R.id.rating_bar)
        RatingBar ratingBar;
        @BindView(R.id.report_btn)
        ImageView reportButton;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }


    public void report(int position) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(context);
        builder.setTitle("Report this comment");

        View itemView = LayoutInflater.from(context).inflate(R.layout.layout_report, null);
        EditText edt_report = (EditText) itemView.findViewById(R.id.edt_report);

        builder.setView(itemView);
        builder.setNegativeButton("CANCEL", (dialog, i) -> { });
        builder.setPositiveButton("SEND", (dialog, i) -> {
            String report = edt_report.getText().toString().trim();
            sendEmail(report, position);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public void sendEmail(String report, int position) {
        try {
            CommentModel commentModel = commentModelList.get(position);
            String emailBody = "Report: " + report + "\n\n"
                    + "Comment No. " + position + " : " + "\n"
                    + "Name: " + commentModel.getName() + "\n"
                    + "Comment: " + commentModel.getComment() + "\n"
                    + "Comment id: " + commentModel.getUid()
                    + "\n\n"
                    + "User name: " + commentModel.getName()
                    + "\n\n"
                    + "Item Name : " + selectedFood.getName() + "\n"
                    + "Description: " + selectedFood.getDescription() + "\n"
                    + "id: " + selectedFood.getId();

            SendEmail sendEmail = new SendEmail(context,"watcharavuth2000@gmail.com",
                    "Report on comment " + position + " of " + selectedFood.getName(), emailBody);
            sendEmail.execute();

        } catch (Exception e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
