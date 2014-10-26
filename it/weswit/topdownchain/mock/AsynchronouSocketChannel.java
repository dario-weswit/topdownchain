package it.weswit.topdownchain.mock;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;


public class AsynchronouSocketChannel {
    
    public final <A> void read(final ByteBuffer src, final A attachment,
            final CompletionHandler<Integer,? super A> handler)
    {
        new Thread() {
            public void run() {
                try {
                    byte[] buf = new byte[src.remaining()];
                    int len = System.in.read(buf);
                    src.put(buf, 0, len);
                    handler.completed(len, attachment);
                } catch (IOException e) {
                    handler.failed(e, attachment);
                }
            }
        }.start();
    }

    public final <A> void write(final ByteBuffer dst, final A attachment,
                                final CompletionHandler<Integer,? super A> handler)
    {
        new Thread() {
            public void run() {
                try {
                    byte[] bytes = new byte[dst.remaining()];
                    dst.get(bytes);
                    System.out.println(new String(bytes, 0, bytes.length, "ASCII7"));
                    handler.completed(bytes.length, attachment);
                } catch (UnsupportedEncodingException e) {
                    System.out.println("???");
                    handler.failed(e, attachment);
                }
            }
        }.start();
    }
            
}