/**
 * 
 */
package org.apache.mina.transport.nio2;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.service.executor.IoHandlerExecutor;
import org.apache.mina.service.idlechecker.IndexedIdleChecker;
import org.apache.mina.transport.tcp.AbstractTcpClient;
import org.apache.mina.transport.tcp.TcpSessionConfig;

/**
 * @author jeffmaury
 *
 */
public class Nio2TcpClient extends AbstractTcpClient {

    private IndexedIdleChecker idleChecker;

    public Nio2TcpClient() {
        this(null);
    }
    
    public Nio2TcpClient(IoHandlerExecutor handlerExecutor) {
        super(handlerExecutor);
        idleChecker = new IndexedIdleChecker();
    }
    
    /* (non-Javadoc)
     * @see org.apache.mina.api.IoClient#connect(java.net.SocketAddress)
     */
    public IoFuture<IoSession> connect(SocketAddress remoteAddress) throws IOException {
        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
        
        final Nio2TcpSession session = new Nio2TcpSession(this, idleChecker, channel);
        getManagedSessions().put(session.getId(), session);
        TcpSessionConfig config = getSessionConfig();

        session.getConfig().setIdleTimeInMillis(IdleStatus.READ_IDLE, config.getIdleTimeInMillis(IdleStatus.READ_IDLE));
        session.getConfig().setIdleTimeInMillis(IdleStatus.WRITE_IDLE,
                config.getIdleTimeInMillis(IdleStatus.WRITE_IDLE));

        // apply the default service socket configuration
        Boolean keepAlive = config.isKeepAlive();

        if (keepAlive != null) {
            session.getConfig().setKeepAlive(keepAlive);
        }

        Boolean oobInline = config.isOobInline();

        if (oobInline != null) {
            session.getConfig().setOobInline(oobInline);
        }

        Boolean reuseAddress = config.isReuseAddress();

        if (reuseAddress != null) {
            session.getConfig().setReuseAddress(reuseAddress);
        }

        Boolean tcpNoDelay = config.isTcpNoDelay();

        if (tcpNoDelay != null) {
            session.getConfig().setTcpNoDelay(tcpNoDelay);
        }

        Integer receiveBufferSize = config.getReceiveBufferSize();

        if (receiveBufferSize != null) {
            session.getConfig().setReceiveBufferSize(receiveBufferSize);
        }

        Integer sendBufferSize = config.getSendBufferSize();

        if (sendBufferSize != null) {
            session.getConfig().setSendBufferSize(sendBufferSize);
        }

        Integer trafficClass = config.getTrafficClass();

        if (trafficClass != null) {
            session.getConfig().setTrafficClass(trafficClass);
        }

        Integer soLinger = config.getSoLinger();

        if (soLinger != null) {
            session.getConfig().setSoLinger(soLinger);
        }

        // Set the secured flag if the service is to be used over SSL/TLS
        if (config.isSecured()) {
            session.initSecure(config.getSslContext());
        }
        
        Nio2TcpSession.ConnectFuture connectFuture = new Nio2TcpSession.ConnectFuture();
        channel.connect(remoteAddress, session, (CompletionHandler<Void, Nio2TcpSession>)connectFuture);
        return connectFuture;
    }

    public void disconnect() {
        // Close all the existing sessions
        for (IoSession session : getManagedSessions().values()) {
            session.close(true);
        }

        fireServiceInactivated();

        // will stop the idle processor if we are the last service
        idleChecker.destroy();
    }

}
