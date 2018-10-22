package nia.chapter2.echoclient;

import com.google.common.util.concurrent.Futures;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Listing 2.4 Main class for the client
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class EchoClient {
    private final String host;
    private final int port;
    private static final Logger log = Logger.getLogger(EchoClient.class.getName());

    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws Exception{
        loadNettyClasses();
        test_NConnections(Integer.parseInt(System.getProperty("connection.count")));
    }

    public void test_RepeatBootstrapAndConnect()
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
            for (int i = 0; i < 1000; i++) {
                log.info("connecting to port" + (port + (i % 10)));
                //bootstrap
                final long bootstrapStart = new Date().getTime();
                Bootstrap b = new Bootstrap()
                        .group(eventLoop)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 50);
                log.info("bootstrap: " + (new Date().getTime() - bootstrapStart));
                //connect
                final long connStart = new Date().getTime();
                ChannelFuture f= b.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast(
                                new EchoOnceClientHandler(connStart));
                    }
                }).connect(new InetSocketAddress(host, port + (i % 10)));
                //wait for close
                f.channel().closeFuture().sync();
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            eventLoop.shutdownGracefully().sync();
        }
    }


    public void test_NConnections(int N)
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
        //bootstrap
        final long bootstrapStart = new Date().getTime();
        Bootstrap b = new Bootstrap()
                .group(eventLoop)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 50);
        log.info("bootstrap: " + (new Date().getTime() - bootstrapStart));

        List<ChannelFuture> closeFutures = new ArrayList<>();
        try {
            for (int i = 0; i < N; i++) {
                log.info("connecting to port" + (port + (i % 10)));
                //connect
                final long connStart = new Date().getTime();
                ChannelFuture f= b.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast(
                                new EchoOnceClientHandler(connStart));
                    }
                }).connect(new InetSocketAddress(host, port + (i % 10)));
                closeFutures.add(f);
            }
            //wait for all channels to close
            for(ChannelFuture closeFuture: closeFutures){
                closeFuture.sync();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            eventLoop.shutdownGracefully().sync();
        }
    }

    /**
     * sometimes throwing SIGSEGV errors
     * @throws Exception
     */
    private void loadNettyClasses() throws Exception {
        log.info("Loading netty classes");
        List<ClassLoader> classLoadersList = new ArrayList<>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());
        classLoadersList.add(ClassLoader.getSystemClassLoader());

        Reflections r = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false /* don't exclude Object.class */), new ResourcesScanner())
                .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
                .filterInputsBy(new FilterBuilder()
                        .include(FilterBuilder.prefix("io.netty"))
//                        .include(FilterBuilder.prefix("java"))
//                        .include(FilterBuilder.prefix("sun.net"))
//                        .include(FilterBuilder.prefix("java.net"))
                        .include(FilterBuilder.prefix("java.nio"))));

        Set<Class<?>> classes = r.getSubTypesOf(Object.class);

        Class.forName("sun.nio.ch.SelChImpl");
        Class.forName("sun.nio.ch.SocketChannelImpl");
        Class.forName("sun.nio.ch.SocketDispatcher");
        Class.forName("sun.nio.ch.Net");
        Class.forName("java.net.StandardProtocolFamily");
        Class.forName("java.net.Socket");
        Class.forName("sun.nio.ch.SocketAdaptor");
        Class.forName("sun.nio.ch.ExtendedSocketOption");
        Class.forName("sun.nio.ch.ExtendedSocketOption$1");
        Class.forName("sun.net.ExtendedOptionsImpl");
        Class.forName("jdk.net.SocketFlow");
        Class.forName("sun.nio.ch.SocketOptionRegistry");
        Class.forName("sun.nio.ch.OptionKey");
        Class.forName("java.nio.channels.spi.AbstractSelector$1");
        Class.forName("java.nio.channels.spi.AbstractSelectionKey");
        Class.forName("sun.nio.ch.SelectionKeyImpl");
        Class.forName("nia.chapter2.echoclient.EchoClientHandler");

        for (Class clazz : classes) {
            try {
                Class.forName(clazz.getName());
            } catch (Throwable e) {

            }
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

