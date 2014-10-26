package it.weswit.topdownchain.redirection;


import it.weswit.topdownchain.Redirector;
import it.weswit.topdownchain.mock.AsynchronouSocketChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class WriteRedirector extends Redirector.With1<IOException> {
    private final AsynchronouSocketChannel socket;
    private final byte[] bytes;
    private volatile int done = 0;

    public WriteRedirector(AsynchronouSocketChannel socket, byte[] bytes) {
        this.socket = socket;
        this.bytes = bytes;
    }
    
    public final void launch() {
        final ByteBuffer buf = ByteBuffer.allocate(bytes.length);
        buf.put(bytes, 0, bytes.length);
        buf.flip();
        socket.write(buf, null, new CompletionHandler<Integer, Void>() {
            public void completed(Integer done, Void attachment) {
                WriteRedirector.this.done = done;
                onCompleted();
            }
            public void failed(Throwable t, Void attachment) {
                if (t instanceof IOException) {
                    onException1((IOException) t);
                } else {
                    onUnexpectedException(t);
                }
            }
        });
    }
    
    public int getDone() {
        return done;
    }
}