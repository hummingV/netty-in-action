package nia.chapter2.echoclient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.util.Date;
import java.util.logging.Logger;

public class EchoOnceClientHandler
        extends SimpleChannelInboundHandler<ByteBuf> {
    private static final Logger log = Logger.getLogger(EchoOnceClientHandler.class.getName());
    private final long connStartTime;
    private volatile long sendTime;

    public EchoOnceClientHandler(long connStartTime) {
        this.connStartTime = connStartTime;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        log.info("channelActive: " + (new Date().getTime() - connStartTime));
        sendTime = new Date().getTime();
        ctx.channel().writeAndFlush(Unpooled.copiedBuffer("Netty rocks!",
                CharsetUtil.UTF_8));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        log.info("received: " + (new Date().getTime() - sendTime));
        log.fine("Client received: " + in.toString(CharsetUtil.UTF_8));
        ChannelFuture future = ctx.channel().close();
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                log.fine("channel closed");
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}