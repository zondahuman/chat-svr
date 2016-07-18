package com.abin.lee.im.chat.gate;


import com.abin.lee.im.chat.common.NamedThreadFactory;
import com.abin.lee.im.chat.gate.handler.GateChannelHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("com.abin.lee")
public class GateServer {
    private static Logger LOGGER = LogManager.getLogger(GateServer.class);

    private NamedThreadFactory bossNamedThreadFac = new NamedThreadFactory("NettyAcceptSelectorProcessor", false);
    private NamedThreadFactory workerNamedThreadFac = new NamedThreadFactory("NettyReadSelectorProcessor", true);
    private int availableCpu = Runtime.getRuntime().availableProcessors();
    private Bootstrap bootstrap;

    public void connect(final int bindPort,String host)throws Exception{
        bootstrap = new Bootstrap();
        //conf server nio threadpool
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(availableCpu, bossNamedThreadFac);
        bossGroup.setIoRatio(100);
        try{
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
            bootstrap.group(bossGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_KEEPALIVE, true)//  keepalive connect for ever
                    .option(ChannelOption.TCP_NODELAY, false)// nagle algorithm
//                    .option(ChannelOption.SO_SNDBUF, 10 * 1024)// 1m
//                    .option(ChannelOption.SO_RCVBUF, 10 * 1024)// 1m
//                    .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 10 * 1024) // 调大写出buffer为512kb
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new GateChannelHandler());
                        }

                        ;
                    });

            //binding port，sync wait success
            ChannelFuture channelFuture = bootstrap.connect(host, bindPort).sync();

            channelFuture.addListener(new GenericFutureListener<Future<Object>>() {
                @Override
                public void operationComplete(Future<Object> future) throws Exception {
                    if (future.isSuccess()) {
                        LOGGER.info("IM GateWayServer Start! binding port on:" + bindPort);
                    }
                }
            });
            //waiting listening port to wait shutdown
            channelFuture.channel().closeFuture().sync();
        }finally{
            //exit Release resources
            bossGroup.shutdownGracefully();
        }

    }

    public static void main(String[] args)throws Exception {
        int port = 8085;
        if(args!=null && args.length > 0){
            port = Integer.valueOf(args[0]);
        }
        new GateServer().connect(port, "127.0.0.1");
    }
}
