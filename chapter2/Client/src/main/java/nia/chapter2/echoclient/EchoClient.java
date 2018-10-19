package nia.chapter2.echoclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Listing 2.4 Main class for the client
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class EchoClient {
    private final String host;
    private final int port;
    public static volatile long start;

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start()
        throws Exception {
        final AtomicLong serial = new AtomicLong();
        EventLoopGroup eventLoop = new NioEventLoopGroup(0, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("CSEventLoop-" + serial.getAndIncrement());
                thread.setPriority(Thread.MAX_PRIORITY);
                return thread;
            }
        });
        try {
            start = new Date().getTime();
            Bootstrap b = new Bootstrap()
                .group(eventLoop)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress(host, port))
                    .option(ChannelOption.TCP_NODELAY,  true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 50)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                        throws Exception {
                        ch.pipeline().addLast(
                             new EchoClientHandler());
                    }
                });
            System.out.println("bootstrap: " + (new Date().getTime() - EchoClient.start));
            EchoClient.start = new Date().getTime();
            ChannelFuture f = b.connect();
            Thread.sleep(1000000); //don't exit the process
            f.channel().closeFuture().sync();
        } finally {
            eventLoop.shutdownGracefully().sync();
        }
    }

    public static void main(String[] args)
            throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: " + EchoClient.class.getSimpleName() +
                    " <host> <port>"
            );
            return;
        }

        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        new EchoClient(host, port).start();
    }
}

