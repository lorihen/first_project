package com.pyg.web;

import com.alibaba.dubbo.config.annotation.Service;
import com.pyg.pojo.TbPayLog;
import com.pyg.service.OrderService;
import com.pyg.service.WeixinPayService;
import com.pyg.utils.IdWorker;
import entity.Result;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.dubbo.config.annotation.Reference;

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
    private OrderService orderService;

    @RequestMapping("/createNative")
    public Map createNative() {
        //1.获取当前登录用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //2.到redis中查询支付日志
        TbPayLog payLog = orderService.searchPayLogFromRedis(username);
        //判断日志是否存在
        if (payLog != null) {
            //存在
            return weixinPayService.createNative(payLog.getOutTradeNo(), payLog.getTotalFee() + "");
        } else {
            return new HashMap();
        }


    }

    @RequestMapping("/queryPayStatus")
    public Result queryPayStatus(String out_trade_no) {
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
                orderService.updateOrderStatus(out_trade_no, map.get("transaction_id"));
                break;
            }
            try {
                Thread.sleep(3000);//间隔三秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //为了不让循环无休止进行,写了个中断程序,定时为大约五分钟
            x++;
            if (x >= 100) {
                result = new Result(false, "二维码超时");
                break;
            }
        }
        return result;
    }
}
