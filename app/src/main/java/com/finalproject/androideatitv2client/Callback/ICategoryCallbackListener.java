package com.finalproject.androideatitv2client.Callback;

import com.finalproject.androideatitv2client.Model.CategoryModel;

import java.util.List;

public interface ICategoryCallbackListener {
    void onCategoryLoadSuccess(List<CategoryModel> categoryModelList);
    void onCategoryLoadFailed(String message);

}
