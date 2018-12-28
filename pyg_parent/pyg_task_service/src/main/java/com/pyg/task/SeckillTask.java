package com.pyg.task;


import com.pyg.mapper.TbSeckillGoodsMapper;
import com.pyg.pojo.TbSeckillGoods;
import com.pyg.pojo.TbSeckillGoodsExample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class SeckillTask {

    @Autowired
    private TbSeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    @Scheduled(cron ="0/10 * * * * ?")
    public void refreshSeckillGoods(){
        System.out.println("执行了 秒杀商品增量更新 任务调度"+new Date());

        //查询缓存中的秒杀商品ID集合
        List goodsList = new ArrayList(redisTemplate.boundHashOps("seckillGoods").keys());
        System.out.println(goodsList);

        //去数据库中查找
        TbSeckillGoodsExample example = new TbSeckillGoodsExample();
        TbSeckillGoodsExample.Criteria criteria = example.createCriteria();
        criteria.andStatusEqualTo("1");//审核通过
        criteria.andStockCountGreaterThan(0);//剩余库存大于0
        criteria.andStartTimeLessThanOrEqualTo(new Date());//开始时间小于等于当前时间
        criteria.andEndTimeGreaterThan(new Date());	//结束时间大于当前时

        if (goodsList.size()>0){
            criteria.andIdNotIn(goodsList);//排除掉缓存中已经存在的商品ID集合
        }

        List<TbSeckillGoods> seckillGoodsList = seckillGoodsMapper.selectByExample(example);
        System.out.println("从数据库中查找秒杀商品");

        //存入缓存中
        for (TbSeckillGoods seckillGoods : seckillGoodsList) {
            redisTemplate.boundHashOps("seckillGoods").put(seckillGoods.getId(),seckillGoods);
            System.out.println("增量更新秒杀商品ID:"+seckillGoods.getId());
        }
    }


    @Scheduled(cron = "1/3 * * * * ?")
    public void removeExpireSeckillGoods(){
        //查询出缓存中的数据,扫描每条,判断时间,,如果当前时间超过了截止时间,移除此记录
        List<TbSeckillGoods> seckillGoods = redisTemplate.boundHashOps("seckillGoods").values();
        System.out.println("执行了清除秒杀商品的任务");
        for (TbSeckillGoods seckillGood : seckillGoods) {
            if (seckillGood.getEndTime().getTime()<new Date().getTime()){
                //数据先同步到数据库
                seckillGoodsMapper.updateByPrimaryKey(seckillGood);
                //再清除缓存
                redisTemplate.boundHashOps("seckillGoods").delete(seckillGood.getId());
                System.out.println("清除了过期秒杀商品");
            }
        }
    }
}
