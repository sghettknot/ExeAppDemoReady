package com.finalproject.androideatitv2client.EventBus;

import com.finalproject.androideatitv2client.Database.CartItem;

public class UpdateItemInCart {

    private CartItem cartItem;

    public UpdateItemInCart(CartItem cartItem) {
        this.cartItem = cartItem;
    }

    public CartItem getCartItem() {
        return cartItem;
    }

    public void setCartItem(CartItem cartItem) {
        this.cartItem = cartItem;
    }
}
