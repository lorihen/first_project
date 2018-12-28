package com.pyg.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.pyg.mapper.TbOrderItemMapper;
import com.pyg.mapper.TbPayLogMapper;
import com.pyg.pojo.TbOrderItem;
import com.pyg.pojo.TbPayLog;
import com.pyg.pojogroup.Cart;
import com.pyg.utils.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pyg.mapper.TbOrderMapper;
import com.pyg.pojo.TbOrder;
import com.pyg.pojo.TbOrderExample;
import com.pyg.pojo.TbOrderExample.Criteria;
import com.pyg.service.OrderService;

import entity.PageResult;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private TbOrderMapper orderMapper;

    @Autowired
    private TbOrderItemMapper orderItemMapper;

    @Autowired
    private TbPayLogMapper payLogMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbOrder> findAll() {
        return orderMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;

    /**
     * 增加
     */
    @Override
    public void add(TbOrder order) {
        //1.从redis中提取购物车列表
        List<Cart> cartList = (List<Cart>) redisTemplate.
                boundHashOps("cartList").get(order.getUserId());

        List orderIdList = new ArrayList(); //订单ID列表
        double total_money = 0;//总金额 0 元

        //2.循环购物车列表添加到订单
        for (Cart cart : cartList) {
            TbOrder tbOrder = new TbOrder();    //创建新订单对象
            long orderId = idWorker.nextId();    //订单ID
            tbOrder.setOrderId(orderId);
            tbOrder.setUserId(order.getUserId());    //登录用户ID
            tbOrder.setPaymentType(order.getPaymentType());//支付方式
            tbOrder.setSellerId(order.getSellerId());    //商家ID
            tbOrder.setSellerId(cart.getSellerId());    //商家ID
            System.out.println("order参数中的sellerID为:"+order.getSellerId());
            System.out.println("cart参数中的sellerID为:"+cart.getSellerId());
            tbOrder.setStatus("1");//支付状态:未付款
            tbOrder.setCreateTime(new Date());//订单创建日期
            tbOrder.setUpdateTime(new Date());//订单更新日期
            tbOrder.setReceiverAreaName(order.getReceiverAreaName());//订单收货地址
            tbOrder.setReceiverMobile(order.getReceiverMobile());//手机号
            tbOrder.setReceiver(order.getReceiver());//收货人
            tbOrder.setSourceType(order.getSourceType());//订单来源
            System.out.println("没有添加sku时的订单对象:" + tbOrder);

            //循环购物车明细
            double money = 0;
            for (TbOrderItem orderItem : cart.getOrderItemList()) {
                orderItem.setOrderId(orderId);//订单ID
                orderItem.setId(idWorker.nextId());//sku商品ID
                orderItem.setSellerId(cart.getSellerId());
                money += orderItem.getTotalFee().doubleValue();//金额累加
                orderItemMapper.insert(orderItem);
            }
            tbOrder.setPayment(new BigDecimal(money));//订单金额

            orderIdList.add(orderId+"");//订单ID添加到订单列表
            total_money+=money;//累加到总金额
            orderMapper.insert(tbOrder);
        }
        //添加支付日志
        if ("1".equals(order.getPaymentType())){//如果是微信支付
            TbPayLog payLog = new TbPayLog();
            payLog.setOutTradeNo(idWorker.nextId()+"");//支付订单号
            payLog.setUserId(order.getUserId());//用户ID
            payLog.setCreateTime(new Date());//订单创建时间
            payLog.setOrderList(orderIdList.toString().replace("[","").replace("]",""));
            payLog.setPayType("1");//支付类型,微信支付
            payLog.setTotalFee((long) (total_money*100));//总金额
            payLog.setTradeState("0");//支付状态,未支付

            //支付记录添加到数据库
            payLogMapper.insert(payLog);
            //存入redis缓存中
            redisTemplate.boundHashOps("payLog").put(order.getUserId(), payLog);
        }


        //清除redis中的购物车缓存
        redisTemplate.boundHashOps("cartList").delete(order.getUserId());
    }


    /**
     * 修改
     */
    @Override
    public void update(TbOrder order) {
        orderMapper.updateByPrimaryKey(order);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbOrder findOne(Long id) {
        return orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            orderMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbOrder order, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbOrderExample example = new TbOrderExample();
        Criteria criteria = example.createCriteria();

        if (order != null) {
            if (order.getPaymentType() != null && order.getPaymentType().length() > 0) {
                criteria.andPaymentTypeLike("%" + order.getPaymentType() + "%");
            }
            if (order.getPostFee() != null && order.getPostFee().length() > 0) {
                criteria.andPostFeeLike("%" + order.getPostFee() + "%");
            }
            if (order.getStatus() != null && order.getStatus().length() > 0) {
                criteria.andStatusLike("%" + order.getStatus() + "%");
            }
            if (order.getShippingName() != null && order.getShippingName().length() > 0) {
                criteria.andShippingNameLike("%" + order.getShippingName() + "%");
            }
            if (order.getShippingCode() != null && order.getShippingCode().length() > 0) {
                criteria.andShippingCodeLike("%" + order.getShippingCode() + "%");
            }
            if (order.getUserId() != null && order.getUserId().length() > 0) {
                criteria.andUserIdLike("%" + order.getUserId() + "%");
            }
            if (order.getBuyerMessage() != null && order.getBuyerMessage().length() > 0) {
                criteria.andBuyerMessageLike("%" + order.getBuyerMessage() + "%");
            }
            if (order.getBuyerNick() != null && order.getBuyerNick().length() > 0) {
                criteria.andBuyerNickLike("%" + order.getBuyerNick() + "%");
            }
            if (order.getBuyerRate() != null && order.getBuyerRate().length() > 0) {
                criteria.andBuyerRateLike("%" + order.getBuyerRate() + "%");
            }
            if (order.getReceiverAreaName() != null && order.getReceiverAreaName().length() > 0) {
                criteria.andReceiverAreaNameLike("%" + order.getReceiverAreaName() + "%");
            }
            if (order.getReceiverMobile() != null && order.getReceiverMobile().length() > 0) {
                criteria.andReceiverMobileLike("%" + order.getReceiverMobile() + "%");
            }
            if (order.getReceiverZipCode() != null && order.getReceiverZipCode().length() > 0) {
                criteria.andReceiverZipCodeLike("%" + order.getReceiverZipCode() + "%");
            }
            if (order.getReceiver() != null && order.getReceiver().length() > 0) {
                criteria.andReceiverLike("%" + order.getReceiver() + "%");
            }
            if (order.getInvoiceType() != null && order.getInvoiceType().length() > 0) {
                criteria.andInvoiceTypeLike("%" + order.getInvoiceType() + "%");
            }
            if (order.getSourceType() != null && order.getSourceType().length() > 0) {
                criteria.andSourceTypeLike("%" + order.getSourceType() + "%");
            }
            if (order.getSellerId() != null && order.getSellerId().length() > 0) {
                criteria.andSellerIdLike("%" + order.getSellerId() + "%");
            }

        }

        Page<TbOrder> page = (Page<TbOrder>) orderMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public TbPayLog searchPayLogFromRedis(String userId) {
        return (TbPayLog) redisTemplate.boundHashOps("payLog").get(userId);
    }



    @Override
    public void updateOrderStatus(String out_trade_no, String transaction_id) {
        //1.修改支付日志的订单状态
        TbPayLog payLog = payLogMapper.selectByPrimaryKey(out_trade_no);
        payLog.setPayTime(new Date());//支付时间
        payLog.setTradeState("2");//已支付
        payLog.setTransactionId(transaction_id);//交易号
        payLogMapper.updateByPrimaryKey(payLog);//更新

        //2.修改订单状态
        String orderList = payLog.getOrderList();//获取订单号列表
        String[] orderIds = orderList.split(",");
        for (String orderId : orderIds) {
            TbOrder order = orderMapper.selectByPrimaryKey(Long.parseLong(orderId));//获取订单
            if (order!=null){
                order.setStatus("2");//已付款
                orderMapper.updateByPrimaryKey(order);//更新
            }
        }
        //清除redis缓存
        redisTemplate.boundHashOps("payLog").delete(payLog.getUserId());
    }


}
