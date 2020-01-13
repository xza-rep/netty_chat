package com.itheima.netty_chat;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 拾柒
 * @create 2020/1/8
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    //用来保存所有的客户端连接
    private  static ChannelGroup clients=new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private SimpleDateFormat sdf=new SimpleDateFormat("yyyy-mm-dd hh:MM");
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame msg) throws Exception {
        //当接收到数据后会自动调用
        String text = msg.text();
        System.out.println("接收到消息数据为："+text);

        for (Channel client : clients) {
            //将消息发送到所有的客户端
            client.writeAndFlush(new TextWebSocketFrame(sdf.format(new Date()) + ":" + text));
        }
    }

    //当有新的客户端连接服务器之后，会自动调用这个方法
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //将新的通道加入到clients
        clients.add(ctx.channel());
    }
}
