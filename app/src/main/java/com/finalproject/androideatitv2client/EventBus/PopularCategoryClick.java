package com.finalproject.androideatitv2client.EventBus;

import com.finalproject.androideatitv2client.Model.CategoryModel;
import com.finalproject.androideatitv2client.Model.PopularCategoryModel;

public class PopularCategoryClick {
    private PopularCategoryModel popularCategoryModel;

    public PopularCategoryClick(PopularCategoryModel popularCategoryModel) {
        this.popularCategoryModel = popularCategoryModel;
    }

    public PopularCategoryModel getPopularCategoryModel() {
        return popularCategoryModel;
    }

    public void setPopularCategoryModel(PopularCategoryModel popularCategoryModel) {
        this.popularCategoryModel = popularCategoryModel;
    }

    // ************ TESTING SUCCESS STUFF **************
    private boolean success;
    public PopularCategoryClick(boolean success, PopularCategoryModel popularCategoryModel) {
        this.success = success;
        this.popularCategoryModel = popularCategoryModel;
    }
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
}
