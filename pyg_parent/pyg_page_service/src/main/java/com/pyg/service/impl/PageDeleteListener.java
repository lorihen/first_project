package com.pyg.service.impl;

import com.pyg.service.ItemPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

@Component
public class PageDeleteListener implements MessageListener {
    @Autowired
    private ItemPageService itemPageService;

    @Override
    public void onMessage(Message message) {
        ObjectMessage objectMessage = (ObjectMessage) message;
        try {
            Long[] goodsId = (Long[])objectMessage.getObject();
            System.out.println("PageDeleteListener 收到监听消息:"+goodsId);
            boolean b = itemPageService.deleteItemHtml(goodsId);
            System.out.println("网页删除结果:" + b);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
