package com.abin.lee.im.chat.router;


import com.abin.lee.im.chat.common.NamedThreadFactory;
import com.abin.lee.im.chat.router.handler.RouterServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

//@Configuration
//@ComponentScan("com.abin.lee")
public class RouterServer {
    private static Logger LOGGER = LogManager.getLogger(RouterServer.class);

    private NamedThreadFactory bossNamedThreadFac = new NamedThreadFactory("NettyAcceptSelectorProcessor", false);
    private NamedThreadFactory workerNamedThreadFac = new NamedThreadFactory("NettyReadSelectorProcessor", true);
    private int availableCpu = Runtime.getRuntime().availableProcessors();
    private ServerBootstrap serverBootstrap;

    public void bind(final int bindPort, String host)throws Exception{
        LOGGER.info("bindPort=" + bindPort + ", host=" + host);
        //conf server nio threadpool
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(availableCpu, bossNamedThreadFac);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(availableCpu,workerNamedThreadFac);
        workerGroup.setIoRatio(100);
        try{
//            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_KEEPALIVE, true)// keepalive connect for ever
                    .option(ChannelOption.TCP_NODELAY, false)// nagle algorithm
                    .option(ChannelOption.SO_SNDBUF, 10)// 1m
                    .option(ChannelOption.SO_RCVBUF, 10)// 1m
                    .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 256 * 1024) // 调大写出buffer为512kb
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new RouterServerHandler());
                        }

                    });

            //binding port，sync wait success
            ChannelFuture channelFuture = serverBootstrap.bind(host, bindPort).sync();

            channelFuture.addListener(new GenericFutureListener<Future<Object>>() {
                @Override
                public void operationComplete(Future<Object> future) throws Exception {
                    if (future.isSuccess()) {
                        LOGGER.info("IM RouterServer Start! binding port on:" + bindPort);
                    }
                }
            });
            //waiting listening port to wait shutdown
            channelFuture.channel().closeFuture().sync();

        }finally{
            //exit Release resources
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception{
        int port = 8085;
        if(args!=null && args.length > 0){
            port = Integer.valueOf(args[0]);
        }
        new RouterServer().bind(port, "127.0.0.1");


    }
}
