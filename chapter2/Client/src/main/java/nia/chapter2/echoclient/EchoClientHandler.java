package nia.chapter2.echoclient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listing 2.3 ChannelHandler for the client
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
@Sharable
public class EchoClientHandler
    extends SimpleChannelInboundHandler<ByteBuf> {
    private static ExecutorService executorService = Executors.newSingleThreadExecutor();
    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        System.out.println("channelActive: " + (new Date().getTime() - EchoClient.start));
        EchoClient.start = new Date().getTime();
        ctx.channel().writeAndFlush(Unpooled.copiedBuffer("Netty rocks!",
                CharsetUtil.UTF_8));
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try{
                        Thread.sleep(1000);
                        EchoClient.start = new Date().getTime();
                        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!",
                                CharsetUtil.UTF_8));

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
        });

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        System.out.println("received: " + (new Date().getTime()- EchoClient.start));
        System.out.println(
                "Client received: " + in.toString(CharsetUtil.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
        Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
