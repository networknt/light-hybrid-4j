package com.networknt.rpc.router;

import com.networknt.colfer.Account;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.*;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StringReadChannelListener;
import io.undertow.util.StringWriteChannelListener;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Created by steve on 12/04/17.
 */
public class RpcRouterTest {
    static final Logger logger = LoggerFactory.getLogger(RpcRouterTest.class);
    private static final ByteBufferPool pool = new DefaultByteBufferPool(true, 8192 * 3, 1000, 10, 100);
    private static XnioWorker worker;
    private static OptionMap DEFAULT_OPTIONS;

    static Undertow server = null;

    @BeforeClass
    public static void setUp() throws Exception {
        RpcRouter rpcRouter = new RpcRouter();
        // Make sure that services are loaded in service map. It should be called by
        // Server module in a normal instance.
        RpcStartupHookProvider provider = new RpcStartupHookProvider();
        provider.onStartup();
        if(server == null) {
            logger.info("starting server");
            server = Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setHandler(rpcRouter.getHandler())
                    .build();
            server.start();
        }
        final OptionMap.Builder builder = OptionMap.builder()
                       .set(Options.WORKER_IO_THREADS, 8)
                       .set(Options.TCP_NODELAY, true)
                       .set(Options.KEEP_ALIVE, true)
                       .set(Options.WORKER_NAME, "Client");
        DEFAULT_OPTIONS = builder.getMap();

        // Create xnio worker
        final Xnio xnio = Xnio.getInstance();
        final XnioWorker xnioWorker = xnio.createWorker(null, DEFAULT_OPTIONS);
        worker = xnioWorker;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if(server != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server.stop();
            logger.info("The server is stopped.");
        }
        worker.shutdown();
    }

    @Test
    public void testJsonRpcValidationError() throws Exception {
        UndertowClient client = UndertowClient.getInstance();

        String message = "{\"host\":\"www.networknt.com\",\"service\":\"account\",\"action\":\"delete\",\"version\":\"0.1.1\",\"accountNo\":\"1234567\"}";
        final CountDownLatch latch = new CountDownLatch(1);
        final List<String> responses = new CopyOnWriteArrayList<>();
        final ClientConnection connection = client.connect(new URI("http://localhost:8080"), worker, pool, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(() -> {
                final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/api/json");
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(ClientExchange result) {
                        new StringWriteChannelListener(message).setup(result.getRequestChannel());
                        result.setResponseListener(new ClientCallback<ClientExchange>() {
                            @Override
                            public void completed(ClientExchange result) {
                                new StringReadChannelListener(pool) {

                                    @Override
                                    protected void stringDone(String string) {
                                        System.out.println("response = " + string);
                                        responses.add(string);
                                        latch.countDown();
                                    }

                                    @Override
                                    protected void error(IOException e) {
                                        e.printStackTrace();
                                        latch.countDown();
                                    }
                                }.setup(result.getResponseChannel());
                            }

                            @Override
                            public void failed(IOException e) {
                                e.printStackTrace();
                                latch.countDown();
                            }
                        });
                    }

                    @Override
                    public void failed(IOException e) {
                        e.printStackTrace();
                        latch.countDown();
                    }
                });
            });

            latch.await();
            final String responseBody = responses.iterator().next();
            System.out.println("body = " + responseBody);
            Assert.assertTrue(responseBody.contains("ERR11004"));
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testJsonRpcNoError() throws Exception {
        UndertowClient client = UndertowClient.getInstance();

        String message = "{\"host\":\"www.networknt.com\",\"service\":\"account\",\"action\":\"delete\",\"version\":\"0.1.1\",\"accountNo\":\"1234567\",\"accountType\":\"P\"}";
        final CountDownLatch latch = new CountDownLatch(1);
        final List<String> responses = new CopyOnWriteArrayList<>();
        final ClientConnection connection = client.connect(new URI("http://localhost:8080"), worker, pool, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(() -> {
                final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/api/json");
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(ClientExchange result) {
                        new StringWriteChannelListener(message).setup(result.getRequestChannel());
                        result.setResponseListener(new ClientCallback<ClientExchange>() {
                            @Override
                            public void completed(ClientExchange result) {
                                new StringReadChannelListener(pool) {

                                    @Override
                                    protected void stringDone(String string) {
                                        System.out.println("response = " + string);
                                        responses.add(string);
                                        latch.countDown();
                                    }

                                    @Override
                                    protected void error(IOException e) {
                                        e.printStackTrace();
                                        latch.countDown();
                                    }
                                }.setup(result.getResponseChannel());
                            }

                            @Override
                            public void failed(IOException e) {
                                e.printStackTrace();
                                latch.countDown();
                            }
                        });
                    }

                    @Override
                    public void failed(IOException e) {
                        e.printStackTrace();
                        latch.countDown();
                    }
                });
            });

            latch.await();
            final String responseBody = responses.iterator().next();
            Assert.assertEquals("OK", responseBody);
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testColferRpc() throws Exception {
        /*
        UndertowClient client = UndertowClient.getInstance();
        Account account = new Account();
        account.host = "www.networknt.com";
        account.service = "account";
        account.action = "credit";
        account.version = "1.0.0";
        account.accountNo = "1234567";
        account.credit = 12.00f;

        byte[] buf = new byte[Math.min(Account.colferSizeMax, 2048)];
        int i = account.marshal(buf, 0);

        final CountDownLatch latch = new CountDownLatch(1);
        final List<String> responses = new CopyOnWriteArrayList<>();
        final ClientConnection connection = client.connect(new URI("http://localhost:8080"), worker, pool, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(() -> {
                final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/api/json");
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                connection.sendRequest(request, new ClientCallback<ClientExchange>() {
                    @Override
                    public void completed(ClientExchange result) {
                        new StringWriteChannelListener(message).setup(result.getRequestChannel());
                        result.setResponseListener(new ClientCallback<ClientExchange>() {
                            @Override
                            public void completed(ClientExchange result) {
                                new StringReadChannelListener(pool) {

                                    @Override
                                    protected void stringDone(String string) {
                                        System.out.println("response = " + string);
                                        responses.add(string);
                                        latch.countDown();
                                    }

                                    @Override
                                    protected void error(IOException e) {
                                        e.printStackTrace();
                                        latch.countDown();
                                    }
                                }.setup(result.getResponseChannel());
                            }

                            @Override
                            public void failed(IOException e) {
                                e.printStackTrace();
                                latch.countDown();
                            }
                        });
                    }

                    @Override
                    public void failed(IOException e) {
                        e.printStackTrace();
                        latch.countDown();
                    }
                });
            });

            latch.await();
            final String responseBody = responses.iterator().next();
            Assert.assertEquals("OK", responseBody);
        } finally {
            IoUtils.safeClose(connection);
        }
        */
    }

}
