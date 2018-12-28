package com.pyg.web;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pyg.pojogroup.Cart;
import com.pyg.service.CartService;
import com.pyg.utils.CookieUtil;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Reference(timeout = 6000)
    private CartService cartService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @RequestMapping("/findCartList")
    public List<Cart> findCartList() {
        //当前登录人账号
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("当前登录账号为:" + name);
        String cartListString = CookieUtil.getCookieValue(request, "cartList", "UTF-8");
        if (cartListString == null || cartListString.equals("")) {
            cartListString = "[]";
        }
        List<Cart> cartList_cookie = JSON.parseArray(cartListString, Cart.class);
        if (name.equals("anonymousUser")) {
            //如果未登录,返回从cookie中提取的购物车
            System.out.println("返回从cookie中提取的购物车");
            return cartList_cookie;
        } else {
            //如果已登录
            List<Cart> cartList_redis = cartService.findCarListFromRedis(name);
            if (cartList_cookie.size()>0){//如果cookie中的购物车存在商品
                //合并购物车
                cartList_redis = cartService.mergeCartList(cartList_cookie, cartList_redis);
                //清除本地cookie中的购物车
                CookieUtil.deleteCookie(request,response,"cartList");
                //将合并后的数据存入redis中
                cartService.saveCarListToRedis(name,cartList_redis);
            }


            return cartList_redis;
        }
    }

    @CrossOrigin(origins="http://localhost:9105",allowCredentials="true")
    @RequestMapping("/addGoodsToCartList")
    public Result addGoodsToCartList(Long itemId, Integer num) {
//        response.setHeader("Access-Control-Allow-Origin","http://localhost:9105");//可以访问的域(如果方法需要操作cookie,域必须明确)
//        response.setHeader("Access-Control-Allow-Credentials","true");//如果方法涉及到操作cookie,必加

        //当前登录人账号
        String name = SecurityContextHolder.getContext().getAuthentication().getName();

        try {
            //1. 从cookie中提取购物车
            List<Cart> cartList = findCartList();

            //2. 调用服务方法操作购物车
            cartList = cartService.addGoodsToCartList(cartList, itemId, num);
            if (name.equals("anonymousUser")) {
                //3. 将新的购物车存入cookie
                String cartListString = JSON.toJSONString(cartList);
                CookieUtil.setCookie(request, response, "cartList", cartListString, 3600 * 24, "UTF-8");
                System.out.println("向 cookie 存入数据");
            } else {
                //3. 将新的购物车存入redis
                cartService.saveCarListToRedis(name, cartList);
                System.out.println("向 redis 存入数据");
            }
            return new Result(true, "存入购物车成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(false, "存入失败");
        }
    }

}
