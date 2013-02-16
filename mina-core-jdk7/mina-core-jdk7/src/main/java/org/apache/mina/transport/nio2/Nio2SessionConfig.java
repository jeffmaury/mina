/**
 * 
 */
package org.apache.mina.transport.nio2;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousSocketChannel;

import javax.net.ssl.SSLContext;

import org.apache.mina.api.ConfigurationException;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.transport.tcp.TcpSessionConfig;

/**
 * @author jeffmaury
 *
 */
public class Nio2SessionConfig implements TcpSessionConfig {

    private AsynchronousSocketChannel channel;
    private SSLContext sslContext;
    
    private long idleTimeRead = -1;

    private long idleTimeWrite = -1;


    Nio2SessionConfig(final AsynchronousSocketChannel channel) {
        this.channel = channel;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getIdleTimeInMillis(IdleStatus status) {
        switch (status) {
        case READ_IDLE:
            return idleTimeRead;
        case WRITE_IDLE:
            return idleTimeWrite;
        default:
            throw new RuntimeException("unexpected excetion, unknown idle status : " + status);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIdleTimeInMillis(IdleStatus status, long ildeTimeInMilli) {
        switch (status) {
        case READ_IDLE:
            this.idleTimeRead = ildeTimeInMilli;
            break;
        case WRITE_IDLE:
            this.idleTimeWrite = ildeTimeInMilli;
            break;
        default:
            throw new RuntimeException("unexpected excetion, unknown idle status : " + status);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isTcpNoDelay() {
        try {
            return channel.getOption(StandardSocketOptions.TCP_NODELAY);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTcpNoDelay(boolean tcpNoDelay) {
        try {
            channel.setOption(StandardSocketOptions.TCP_NODELAY, tcpNoDelay);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isReuseAddress() {
        try {
            return channel.getOption(StandardSocketOptions.SO_REUSEADDR);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReuseAddress(boolean reuseAddress) {
        try {
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, reuseAddress);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getReceiveBufferSize() {
        try {
            return channel.getOption(StandardSocketOptions.SO_RCVBUF);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReceiveBufferSize(int receiveBufferSize) {
        try {
            channel.setOption(StandardSocketOptions.SO_RCVBUF, receiveBufferSize);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getSendBufferSize() {
        try {
            return channel.getOption(StandardSocketOptions.SO_SNDBUF);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSendBufferSize(int sendBufferSize) {
        try {
            channel.setOption(StandardSocketOptions.SO_SNDBUF, sendBufferSize);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getTrafficClass() {
        //TODO: check trafic class for NIO2
        throw new RuntimeException("trafic class not supported in NIO2");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTrafficClass(int trafficClass) {
        // TODO: check trafic class for NIO2
        throw new RuntimeException("trafic class not supported in NIO2");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isKeepAlive() {
        try {
            return channel.getOption(StandardSocketOptions.SO_KEEPALIVE);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKeepAlive(boolean keepAlive) {
        try {
            channel.setOption(StandardSocketOptions.SO_KEEPALIVE, keepAlive);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isOobInline() {
        //TODO: check OOB in line class for NIO2
        throw new RuntimeException("OOB in line not supported in NIO2");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOobInline(boolean oobInline) {
        //TODO: check OOB in line class for NIO2
        throw new RuntimeException("OOB in line not supported in NIO2");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getSoLinger() {
        try {
            return channel.getOption(StandardSocketOptions.SO_LINGER);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSoLinger(int soLinger) {
        try {
            channel.setOption(StandardSocketOptions.SO_LINGER, soLinger);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecured() {
        return getSslContext() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;

    }
}
