package net.chickensmp.auth;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.List;

/** AES/CFB8 stream encryption, identical to vanilla's connection encryption. */
final class CipherCodec {
    static Cipher cipher(int mode, SecretKey key) throws Exception {
        Cipher c = Cipher.getInstance("AES/CFB8/NoPadding");
        c.init(mode, key, new IvParameterSpec(key.getEncoded()));
        return c;
    }

    static final class Decoder extends MessageToMessageDecoder<ByteBuf> {
        private final Cipher cipher;
        Decoder(Cipher cipher) { this.cipher = cipher; }
        @Override protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            int n = in.readableBytes();
            byte[] src = new byte[n];
            in.readBytes(src);
            ByteBuf buf = ctx.alloc().heapBuffer(n);
            buf.writeBytes(cipher.update(src));
            out.add(buf);
        }
    }

    static final class Encoder extends MessageToByteEncoder<ByteBuf> {
        private final Cipher cipher;
        Encoder(Cipher cipher) { this.cipher = cipher; }
        @Override protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
            int n = in.readableBytes();
            byte[] src = new byte[n];
            in.readBytes(src);
            out.writeBytes(cipher.update(src));
        }
    }
}
