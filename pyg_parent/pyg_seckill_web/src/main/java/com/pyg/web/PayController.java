package com.pyg.web;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pyg.pojo.TbPayLog;
import com.pyg.pojo.TbSeckillOrder;
import com.pyg.service.SeckillOrderService;
import com.pyg.service.WeixinPayService;
import entity.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制层
 *
 * @author Administrator
 */
@RestController
@RequestMapping("/pay")
public class PayController {

    @Reference(timeout = 6000)
    private WeixinPayService weixinPayService;

    @Reference
    private SeckillOrderService seckillOrderService;

    /**
     * 生成二维码
     *
     * @return
     */
    @RequestMapping("/createNative")
    public Map createNative() {
        //1.获取当前登录用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //2.到redis中查询支付日志
        TbSeckillOrder seckillOrder = seckillOrderService.searchOrderFromRedisByUserId(username);
        //判断秒杀订单是否存在
        if (seckillOrder != null) {
            //存在
            long fen = (long) (seckillOrder.getMoney().doubleValue() * 100);   //金额(分)
            return weixinPayService.createNative(seckillOrder.getId() + "", fen + "");
        } else {
            return new HashMap();
        }


    }

    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no) {
        //获取登录用户ID
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Result result = null;
        int x = 0;
        while (true) {
            //调用查询接口
            Map<String, String> map = weixinPayService.queryPayStatus(out_trade_no);
            if (map == null) {//出错
                result = new Result(false, "支付方式发生错误");
                break;
            }
            if (map.get("trade_state").equals("SUCCESS")) {//支付成功
                result = new Result(true, "支付成功");
                //保存订单
                seckillOrderService.saveOrderFromRedisToDb(userId, Long.valueOf(out_trade_no), map.get("transaction_id"));
                break;
            }
            try {
                Thread.sleep(3000);//间隔三秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            x++;
            if (x > 20) {
                result = new Result(false, "二维码超时");
                //1.调用微信的关闭订单接口（学员实现）
                Map<String, String> payresult = weixinPayService.closePay(out_trade_no);
                if (!"SUCCESS".equals(payresult.get("result_code"))) {//如果返回结果是正常关闭
                    //确认是否是已支付状态
                    if ("ORDERPAID".equals(payresult.get("err_code"))) {
                        result = new Result(true, "支付成功");
                        //保存订单到数据库
                        seckillOrderService.saveOrderFromRedisToDb(userId,
                                Long.valueOf(out_trade_no), map.get("transaction_id"));
                    }
                }
                if (result.isSuccess() == false) {
                    System.out.println("超时，取消订单");
                    //2.调用删除
                    seckillOrderService.deleteOrderFromRedis(userId,
                            Long.valueOf(out_trade_no));
                }
                break;
            }
        }
        return result;
    }
}
