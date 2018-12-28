package com.pyg.service;

import com.pyg.pojogroup.Cart;

import java.util.List;

public interface CartService {

    /**
     * 添加商品到购物车
     * @param cartList
     * @param itemId
     * @param num
     * @return
     */
    public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num);

    /**
     * 从缓存中查找购物车
     * @param username
     * @return
     */
    public List<Cart> findCarListFromRedis(String username);

    /**
     * 将购物车保存到缓存中
     * @param username
     * @param cartList
     */
    public void saveCarListToRedis(String username, List<Cart> cartList);

    /**
     * 合并redis和cookie的购物车
     * @param cartList1
     * @param cartList2
     * @return
     */
    public List<Cart> mergeCartList(List<Cart> cartList1,List<Cart> cartList2);
}
