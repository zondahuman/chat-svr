package com.abin.lee.im.chat.router.handler;


import com.abin.lee.im.chat.model.proto.MsgBodyProto;
import com.abin.lee.im.chat.model.proto.MsgHeaderProto;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.MessageFormat;

public class RouterServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LogManager.getLogger(RouterServerHandler.class);
    private static final AttributeKey<ByteBuf> cumulationKey = AttributeKey.valueOf("cumulation");

    private final ByteToMessageDecoder.Cumulator MERGE_CUMULATOR = new ByteToMessageDecoder.Cumulator() {
        @Override
        public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
            ByteBuf buffer;
            if (cumulation.writerIndex() > cumulation.maxCapacity() - in.readableBytes() || cumulation.refCnt() > 1) {
                buffer = expandCumulation(alloc, cumulation, in.readableBytes());
            } else {
                buffer = cumulation;
            }
            buffer.writeBytes(in);
            in.release();
            return buffer;
        }
    };

    private ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf cumulation, int readable) {
        ByteBuf oldCumulation = cumulation;
        cumulation = alloc.buffer(oldCumulation.readableBytes() + readable);
        cumulation.writeBytes(oldCumulation);
        oldCumulation.release();
        return cumulation;
    }

    public boolean isFirstChannelRead(ByteBuf cumulationBuf){
          return cumulationBuf == null;
    }

    private ByteBuf getCumulationBufOfFirstRead(ByteBuf in){
        boolean notLessThan4 = false;
        ByteBuf cumulationBuf = null;
        try {
            if (in.readableBytes() >= 4) {
                in.markReaderIndex();
                int totalLen = in.readInt();
                in.resetReaderIndex();
                ByteBuf container = PooledByteBufAllocator.DEFAULT.heapBuffer(totalLen + 4);
                container.writeBytes(in);
                cumulationBuf = container;
                notLessThan4 = true;
            } else {
                cumulationBuf = in;
            }
        } finally {
//            if (notLessThan4) {
//                in.release();
//            }
        }
        return cumulationBuf;
    }

    private MsgHeaderProto.MsgHeader parseMsgHeader(ByteBuf cumulationBuf) throws InvalidProtocolBufferException {
        int headerLen = cumulationBuf.readInt();
        byte[] headerBytes = new byte[headerLen];
        cumulationBuf.readBytes(headerBytes);
        MsgHeaderProto.MsgHeader msgHeader = MsgHeaderProto.MsgHeader.parseFrom(headerBytes);
        LOGGER.info("msgHeader=" + msgHeader);
        System.out.println("msgHeader=" + msgHeader);
        System.out.println("cumulationBuf.isReadable()=" + cumulationBuf.isReadable());
        return msgHeader;
    }

    private MsgBodyProto.MsgBody parseMsgBody(ByteBuf cumulationBuf, MsgHeaderProto.MsgHeader msgHeader) throws InvalidProtocolBufferException {
        int msgBodyLength = msgHeader.getBodyLength();
        byte[] msgBodyBytes = new byte[msgBodyLength];
        cumulationBuf.readBytes(msgBodyBytes);
        MsgBodyProto.MsgBody msgBody = MsgBodyProto.MsgBody.parseFrom(msgBodyBytes);
        LOGGER.info("msgBody=" + msgBody);
        System.out.println("msgBody=" + msgBody);
        System.out.println("cumulationBuf.isReadable()=" + cumulationBuf.isReadable());
        return msgBody;
    }

    private void dispatcherRequestMsg(ByteBuf cumulationBuf, Attribute<ByteBuf> cumulationAttr, ChannelHandlerContext ctx){
        try {
            int msgBytesLen = 4;
            while (cumulationBuf.readableBytes() >= msgBytesLen) {
                System.out.println("cumulationBuf.isReadable()=" + cumulationBuf.isReadable());
                cumulationBuf.markReaderIndex();
                int totalLen = cumulationBuf.readInt();
                int cumulationValueLen = cumulationBuf.readableBytes();
                if (cumulationValueLen < totalLen) {
                    cumulationBuf.resetReaderIndex();
                    LOGGER.warn("building is not completed!!");
                    break;
                }
                System.out.println("cumulationBuf.isReadable()=" + cumulationBuf.isReadable());
                // resolve msgHeader
                MsgHeaderProto.MsgHeader msgHeader = this.parseMsgHeader(cumulationBuf);
                // resolve msgBody
                MsgBodyProto.MsgBody msgBody = this.parseMsgBody(cumulationBuf, msgHeader);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void cleanCumulationResource(ByteBuf cumulationValue, Attribute<ByteBuf> cumulationAttr,
                                         ChannelHandlerContext ctx) {
        // 如果没有可读数据释放buffer
        if (cumulationValue != null && !cumulationValue.isReadable()) {
            LOGGER.info(MessageFormat.format("Buffer({0}) Release!", cumulationValue.toString()));
            cumulationValue.release();
            cumulationValue = null;
            cumulationAttr.set(null);
        }
        if (cumulationValue != null && cumulationValue.readableBytes() > 1 * 1024 * 1024) {
            LOGGER.error("Message too big!!");
            ctx.close();
        }

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (msg instanceof ByteBuf) {
            Attribute<ByteBuf> cumulationAttr = ctx.channel().attr(cumulationKey);
            ByteBuf cumulationBuf = cumulationAttr.get();
            ByteBuf in = (ByteBuf) msg;
            try {
                if (isFirstChannelRead(cumulationBuf)) {
                    cumulationBuf = getCumulationBufOfFirstRead(in);
                } else {
                    cumulationBuf = MERGE_CUMULATOR.cumulate(ctx.alloc(), cumulationBuf, in);
                }

                // save merge result
                cumulationAttr.set(cumulationBuf);
                //resolve msgHeader and msgBody
                dispatcherRequestMsg(cumulationBuf, cumulationAttr, ctx);
            }  finally {
                cleanCumulationResource(cumulationBuf, cumulationAttr, ctx);
            }
        }


    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
