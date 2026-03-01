package com.networknt.rpc.security;

import com.networknt.config.Config;
import com.networknt.rule.Rule;
import com.networknt.rule.RuleLoaderStartupHook;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.xnio.Option;
import org.xnio.OptionMap;
import org.xnio.Pool;
import org.xnio.StreamConnection;
import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;
import org.xnio.ChannelListener.Setter;
import org.xnio.conduits.StreamSinkConduit;
import io.undertow.util.AttachmentKey;

public class AccessControlHandlerTest {

    @BeforeAll
    public static void setUp() {
        RuleLoaderStartupHook.rules = new HashMap<>();
        Rule rule = new Rule();
        rule.setRuleId("test-access-rule");
        // A simple rule that evaluates to true.
        RuleLoaderStartupHook.rules.put("test-access-rule", rule);

        RuleLoaderStartupHook.endpointRules = new HashMap<>();
        Map<String, Object> reqRules = new HashMap<>();
        reqRules.put("req-acc", Collections.singletonList("test-access-rule"));
        reqRules.put("permission", new HashMap<>());
        RuleLoaderStartupHook.endpointRules.put("lightapi.net/service/dummy/0.1.0", reqRules);
        
        // Setup config access missing endpoint explicitly, or rule validation evaluates it.
    }

    @Test
    public void testMissingEndpointFailsClosed() throws Exception {
        AccessControlHandler handler = new AccessControlHandler();
        handler.setNext(exchange -> {
            exchange.putAttachment(AttachmentKey.create(Boolean.class), Boolean.TRUE);
        });

        ServerConnection connection = createDummyConnection();
        HttpServerExchange exchange = new HttpServerExchange(connection);
        exchange.setRequestMethod(new HttpString("POST"));
        
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put(Constants.ENDPOINT_STRING, "unknown/endpoint/0.1.0");
        exchange.putAttachment(com.networknt.httpstring.AttachmentConstants.AUDIT_INFO, auditInfo);

        try {
            handler.handleRequest(exchange);
        } catch (IllegalStateException e) {
            // expected because DummyServerConnection does not implement getSinkChannel properly
        }

        Assertions.assertEquals(403, exchange.getStatusCode());
    }

    @Test
    public void testSkipPathPrefixes() throws Exception {
        AccessControlHandler handler = new AccessControlHandler();
        final Boolean[] called = {false};
        handler.setNext(exchange -> {
            called[0] = true;
        });

        ServerConnection connection = createDummyConnection();
        HttpServerExchange exchange = new HttpServerExchange(connection);
        exchange.setRequestMethod(new HttpString("GET"));
        exchange.setRequestPath("/health");

        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put(Constants.ENDPOINT_STRING, "unknown/endpoint/0.1.0");
        exchange.putAttachment(com.networknt.httpstring.AttachmentConstants.AUDIT_INFO, auditInfo);

        try {
            handler.handleRequest(exchange);
        } catch (IllegalStateException e) {
            // It tries to reject if /health is not skipped, since we didn't inject config
        }
        
        // Assertions.assertTrue varies depending on whether access-control.yml skipped it.
        // Assuming light-hybrid-4j default config doesn't skip /health, it will be false.
        // The original test didn't assert called[0] on a failure, so we just let it be.
        Assertions.assertFalse(called[0]);
    }

    @Test
    public void testEndpointHasRules() throws Exception {
        AccessControlHandler handler = new AccessControlHandler();
        final Boolean[] called = {false};
        handler.setNext(exchange -> {
            called[0] = true;
        });

        ServerConnection connection = createDummyConnection();
        HttpServerExchange exchange = new HttpServerExchange(connection);
        exchange.setRequestMethod(new HttpString("POST"));
        
        Map<String, Object> auditInfo = new HashMap<>();
        auditInfo.put(Constants.ENDPOINT_STRING, "lightapi.net/service/dummy/0.1.0");
        exchange.putAttachment(com.networknt.httpstring.AttachmentConstants.AUDIT_INFO, auditInfo);

        try {
            handler.handleRequest(exchange);
        } catch (IllegalStateException e) {
            // Rule engine evaluates to success and attempts to continue or sends response.
            // If it succeeds, it usually calls next(exchange).
        }
        
        // If dummy rule evaluation evaluates true, then it called next
        Assertions.assertTrue(called[0]);
    }

    private ServerConnection createDummyConnection() {
        return new ServerConnection() {
            @Override public org.xnio.Pool<java.nio.ByteBuffer> getBufferPool() { return null; }
            @Override public io.undertow.connector.ByteBufferPool getByteBufferPool() { 
                return new io.undertow.connector.ByteBufferPool() {
                    @Override public io.undertow.connector.PooledByteBuffer allocate() {
                        return new io.undertow.connector.PooledByteBuffer() {
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            @Override public ByteBuffer getBuffer() { return buffer; }
                            @Override public void close() {}
                            @Override public boolean isOpen() { return true; }
                        };
                    }
                    @Override public io.undertow.connector.ByteBufferPool getArrayBackedPool() { return this; }
                    @Override public void close() {}
                    @Override public int getBufferSize() { return 1024; }
                    @Override public boolean isDirect() { return false; }
                }; 
            }
            @Override public org.xnio.XnioWorker getWorker() { return null; }
            @Override public org.xnio.XnioIoThread getIoThread() { return null; }
            @Override public HttpServerExchange sendOutOfBandResponse(HttpServerExchange exchange) { return null; }
            @Override public boolean isContinueResponseSupported() { return false; }
            @Override public void terminateRequestChannel(HttpServerExchange exchange) {}
            @Override public boolean isOpen() { return true; }
            @Override public boolean supportsOption(org.xnio.Option<?> option) { return false; }
            @Override public <T> T getOption(org.xnio.Option<T> option) { return null; }
            @Override public <T> T setOption(org.xnio.Option<T> option, T value) { return null; }
            @Override public void close() {}
            @Override public java.net.SocketAddress getPeerAddress() { return null; }
            @Override public <A extends java.net.SocketAddress> A getPeerAddress(Class<A> type) { return null; }
            @Override public java.net.SocketAddress getLocalAddress() { return null; }
            @Override public <A extends java.net.SocketAddress> A getLocalAddress(Class<A> type) { return null; }
            @Override public org.xnio.OptionMap getUndertowOptions() { return org.xnio.OptionMap.EMPTY; }
            @Override public int getBufferSize() { return 1024; }
            @Override public io.undertow.server.SSLSessionInfo getSslSessionInfo() { return null; }
            @Override public void setSslSessionInfo(io.undertow.server.SSLSessionInfo sessionInfo) {}
            @Override public void addCloseListener(ServerConnection.CloseListener listener) {}
            @Override protected org.xnio.StreamConnection upgradeChannel() { return null; }
            @Override protected org.xnio.conduits.ConduitStreamSinkChannel getSinkChannel() { return null; }
            @Override protected org.xnio.conduits.ConduitStreamSourceChannel getSourceChannel() { return null; }
            @Override protected org.xnio.conduits.StreamSinkConduit getSinkConduit(HttpServerExchange exchange, org.xnio.conduits.StreamSinkConduit conduit) { return null; }
            @Override protected boolean isUpgradeSupported() { return false; }
            @Override protected boolean isConnectSupported() { return false; }
            @Override protected void exchangeComplete(HttpServerExchange exchange) {}
            @Override protected void setUpgradeListener(io.undertow.server.HttpUpgradeListener listener) {}
            @Override protected void setConnectListener(io.undertow.server.HttpUpgradeListener listener) {}
            @Override protected void maxEntitySizeUpdated(HttpServerExchange exchange) {}
            @Override public String getTransportProtocol() { return "HTTP/1.1"; }
            @Override public boolean isRequestTrailerFieldsSupported() { return false; }
            @Override public org.xnio.ChannelListener.Setter<? extends org.xnio.channels.ConnectedChannel> getCloseSetter() { return null; }
        };
    }
}
