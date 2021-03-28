package com.finalproject.androideatitv2client.Callback;

import com.finalproject.androideatitv2client.Model.CommentModel;

import java.util.List;

public interface ICommentCallbackListener {
    void onCommentLoadSuccess(List<CommentModel> commentModels);
    void onCommentLoadFailed(String message);
}
