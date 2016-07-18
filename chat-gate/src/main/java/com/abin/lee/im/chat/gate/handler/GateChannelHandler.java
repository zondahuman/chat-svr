package com.abin.lee.im.chat.gate.handler;

import com.abin.lee.im.chat.model.proto.MsgBodyProto;
import com.abin.lee.im.chat.model.proto.MsgHeaderProto;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.logging.Logger;

public class GateChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(GateChannelHandler.class.getName());

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int param = 10;
        ByteBuf out = PooledByteBufAllocator.DEFAULT.buffer();
//        ByteBuf out = PooledByteBufAllocator.DEFAULT.directBuffer();
        MsgHeaderProto.MsgHeader.Builder msgHeaderBuilder = createMsgHeader(param);

        MsgBodyProto.MsgBody msgBody = createMsgBody(param);
        byte[] msgBodyBytes = msgBody.toByteArray();

        msgHeaderBuilder.setBodyLength(msgBodyBytes.length);
        MsgHeaderProto.MsgHeader msgHeader = msgHeaderBuilder.build();
        byte[] msgHeaderBytes = msgHeader.toByteArray();
        out.writeInt(msgHeaderBytes.length + 4 + msgHeader.getBodyLength());
        out.writeInt(msgHeaderBytes.length);
        out.writeBytes(msgHeaderBytes);
        out.writeBytes(msgBodyBytes);
        ctx.writeAndFlush(out.retain());
        out.release();
    }

    private MsgHeaderProto.MsgHeader.Builder createMsgHeader(int param) {
        MsgHeaderProto.MsgHeader.Builder msgHeaderBuilder = MsgHeaderProto.MsgHeader.newBuilder();
        msgHeaderBuilder.addAddress("beijing");
        msgHeaderBuilder.setBodyLength(0);
        msgHeaderBuilder.setMessageType(MsgHeaderProto.MsgHeader.MessageType.ANDROID);
        msgHeaderBuilder.setPageNum(1);
        msgHeaderBuilder.setPageSize(10);
        msgHeaderBuilder.setPassWord("abinpwd");
        msgHeaderBuilder.setQuery("param");
        msgHeaderBuilder.setUserId(param);
        msgHeaderBuilder.setUserName("abin");
        return msgHeaderBuilder;
    }

    private MsgBodyProto.MsgBody createMsgBody(int param) {
        MsgBodyProto.MsgBody.Builder msgHeaderBuilder = MsgBodyProto.MsgBody.newBuilder();
        msgHeaderBuilder.addAddress("shanghai");
        msgHeaderBuilder.setBodyLength(0);
        msgHeaderBuilder.setMessageType(MsgBodyProto.MsgBody.MessageType.IOS);
        msgHeaderBuilder.setPassWord("leepwd");
        msgHeaderBuilder.setUserId(param);
        msgHeaderBuilder.setUserName("lee");
        return msgHeaderBuilder.build();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        //super.channelReadComplete(ctx);
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        System.out.println("receive server response:[" + msg + "]");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.warning("unexpected exception from downstream:" + cause.getMessage());
        ctx.close();
    }

}
