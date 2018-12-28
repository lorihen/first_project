package com.pyg.service.impl;

import com.pyg.service.ItemPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Component
public class PageListener implements MessageListener{
    @Autowired
    private ItemPageService itemPageService;

    @Override
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
        try {
            String text = textMessage.getText();
            System.out.println("PageListener 接收到监听消息: "+text);
            itemPageService.genItemHtml(Long.parseLong(text));
            System.out.println("成功生成静态页面");
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
