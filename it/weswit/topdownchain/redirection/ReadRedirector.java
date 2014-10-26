package it.weswit.topdownchain.redirection;


import it.weswit.topdownchain.Redirector;
import it.weswit.topdownchain.mock.AsynchronouSocketChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class ReadRedirector extends Redirector.With1<IOException> {
    private final AsynchronouSocketChannel socket;
    private final int max;
    private volatile byte[] result;

    public ReadRedirector(AsynchronouSocketChannel socket, int max) {
        this.socket = socket;
        this.max = max;
    }
    
    public final void launch() {
        final ByteBuffer buf = ByteBuffer.allocate(max);
        socket.read(buf, null, new CompletionHandler<Integer, Void>() {
            public void completed(Integer done, Void attachment) {
                buf.flip();
                result = new byte[buf.remaining()];
                buf.get(result);
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
    
    public byte[] getResult() {
        return result;
    }
}