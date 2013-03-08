/**
 * 
 */
package org.apache.mina.transport.nio2;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.api.IoService;
import org.apache.mina.api.IoSession;
import org.apache.mina.api.IoSessionConfig;
import org.apache.mina.api.IoSession.SessionState;
import org.apache.mina.service.idlechecker.IdleChecker;
import org.apache.mina.session.AbstractIoSession;
import org.apache.mina.session.WriteRequest;
import org.apache.mina.transport.tcp.TcpSessionConfig;
import org.apache.mina.util.AbstractIoFuture;

/**
 * @author jeffmaury
 *
 */
public class Nio2TcpSession extends AbstractIoSession {

    static class ConnectFuture extends AbstractIoFuture<IoSession> implements CompletionHandler<Void, Nio2TcpSession> {
        @Override
        protected boolean cancelOwner(boolean mayInterruptIfRunning) {
            return false;
        }
    
        @Override
        public void completed(Void result, Nio2TcpSession attachment) {
            setResult(attachment);
            attachment.setConnected();
            attachment.scheduleRead();
            
        }
    
        @Override
        public void failed(Throwable exc, Nio2TcpSession attachment) {
            setException(exc);
        }
    
    }

    private AsynchronousSocketChannel channel;
    
    private Nio2SessionConfig configuration;
    
    private ByteBuffer readBuffer;

    private boolean readSuspended;

    private boolean writeSuspended;
    
    private AtomicBoolean readRunning = new AtomicBoolean(false);
    
    private AtomicBoolean writeRunning = new AtomicBoolean(false);
    
    private int counter = 0;
    
    private final int sendBufferSize;
    
    Nio2TcpSession(final IoService service, final IdleChecker idleChecker, final AsynchronousSocketChannel channel) {
        super(service, idleChecker);
        this.channel = channel;
        this.configuration = new Nio2SessionConfig(channel);
        this.readBuffer = ByteBuffer.allocateDirect(configuration.getReceiveBufferSize());
        this.sendBufferSize = configuration.getSendBufferSize();
    }
    
    /**
     * Set this session status as connected. To be called by the processor selecting/polling this session.
     */
    void setConnected() {
        if (!isCreated()) {
            throw new RuntimeException("Trying to open a non created session");
        }

        state = SessionState.CONNECTED;

        processSessionOpen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
     public SocketAddress getRemoteAddress() {
        try {
            return channel.getRemoteAddress();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.mina.api.IoSession#getLocalAddress()
     */
    public SocketAddress getLocalAddress() {
        try {
            return channel.getLocalAddress();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.mina.api.IoSession#suspendRead()
     */
    public void suspendRead() {
        readSuspended = true;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.api.IoSession#suspendWrite()
     */
    public void suspendWrite() {
        writeSuspended = true;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.api.IoSession#resumeRead()
     */
    public void resumeRead() {
        readSuspended = false;
        scheduleRead();
    }

    /* (non-Javadoc)
     * @see org.apache.mina.api.IoSession#resumeWrite()
     */
    public void resumeWrite() {
        writeSuspended = false;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.api.IoSession#isReadSuspended()
     */
    public boolean isReadSuspended() {
        return readSuspended;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.api.IoSession#isWriteSuspended()
     */
    public boolean isWriteSuspended() {
        return writeSuspended;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.api.IoSession#getConfig()
     */
    public TcpSessionConfig getConfig() {
        return configuration;
    }

    class WriteCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

        @Override
        public void completed(Integer result, ByteBuffer buffer) {
            //System.out.println("Write confirmed " + result + " bytes " + (System.currentTimeMillis() - start) + " remaining=" + buffer.remaining());
            if (buffer.remaining() > 0) {
                channel.write(buffer, buffer, this);
            } else {
                //System.out.println(System.nanoTime() + " end write");
                WriteRequest request = getWriteQueue().poll();
                if (request != null) {
                    //.println(System.nanoTime() + " starting write");
                    channel.write((ByteBuffer)request.getMessage(), (ByteBuffer)request.getMessage(), this);                        
                } else {
                    writeRunning.set(false);
                }
            }
        }

        @Override
        public void failed(Throwable exc, ByteBuffer buffer) {
            writeRunning.set(false);
            processException(exc);
        }
    }
    
    private final WriteCompletionHandler writeCompletionHandler = new WriteCompletionHandler();
    
    /* (non-Javadoc)
     * @see org.apache.mina.session.AbstractIoSession#writeDirect(java.lang.Object)
     */
    @Override
    protected int writeDirect(Object message) {
        final ByteBuffer buffer = (ByteBuffer)message;
        
        if (writeRunning.compareAndSet(false, true)) {
              int length = buffer.remaining();
              //System.out.println("Writing " + buffer.remaining() + " bytes counter=" + counter++);
              //System.out.println(System.nanoTime() + " starting write");
              channel.write(buffer, buffer, writeCompletionHandler);
              return length;
            
        } else {
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.mina.session.AbstractIoSession#convertToDirectBuffer(org.apache.mina.session.WriteRequest, boolean)
     */
    @Override
    protected ByteBuffer convertToDirectBuffer(WriteRequest writeRequest, boolean createNew) {
        ByteBuffer message = (ByteBuffer) writeRequest.getMessage();

        if (!message.isDirect()) {
            ByteBuffer directBuffer = ByteBuffer.allocateDirect(message.remaining());
            directBuffer.put(message);
            directBuffer.flip();
            writeRequest.setMessage(directBuffer);
            return directBuffer;
        }
        return message;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.session.AbstractIoSession#flushWriteQueue()
     */
    @Override
    public void flushWriteQueue() {
        // TODO Auto-generated method stub
        //System.out.println("FlushWriteQueue called");
        if (writeRunning.compareAndSet(false, true)) {
            WriteRequest request = getWriteQueue().poll();
            if (request != null) {
                channel.write((ByteBuffer)request.getMessage(), (ByteBuffer)request.getMessage(), writeCompletionHandler);
            } else {
                writeRunning.set(false);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.mina.session.AbstractIoSession#channelClose()
     */
    @Override
    protected void channelClose() {
        try {
            channel.close();
        } catch (IOException e) {
            processException(e);
        }
    }
    
    class ReadCompletionHandler implements CompletionHandler<Integer, Nio2TcpSession> {
        @Override
        public void completed(Integer result, Nio2TcpSession attachment) {
            //System.out.println("Recieved " + result + " bytes");
            //System.out.println(System.nanoTime() + " end read");
            readRunning.set(false);
            readBuffer.flip();
            processMessageReceived(readBuffer);
            if (attachment.isConnected() && !readSuspended) {
                attachment.scheduleRead();
            }
            if (writeRunning.compareAndSet(false, true)) {
                WriteRequest request = getWriteQueue().poll();
                if (request != null) {
                    //System.out.println(System.nanoTime() + " starting write");
                    channel.write((ByteBuffer)request.getMessage(), (ByteBuffer)request.getMessage(), writeCompletionHandler);
                } else {
                    writeRunning.set(false);
                }
            }
        }

        @Override
        public void failed(Throwable exc, Nio2TcpSession attachment) {
            readRunning.set(false);
            processException(exc);
            if (attachment.isConnected()) {
                attachment.scheduleRead();
            }
        }
    }
    
    private final CompletionHandler<Integer, Nio2TcpSession> readCompletionHandler = new ReadCompletionHandler();
    
    protected void scheduleRead() {
        if (readRunning.compareAndSet(false, true)) {
            //System.out.println(System.nanoTime() + " starting read");
            readBuffer.rewind();
            channel.read(readBuffer, this, readCompletionHandler);
        }
    }

}
