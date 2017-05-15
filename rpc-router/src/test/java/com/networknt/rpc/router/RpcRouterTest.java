package com.networknt.rpc.router;

import com.networknt.colfer.Account;
import com.networknt.rpc.security.JwtVerifyHandler;
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
    private static String auth = "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTgwODkxMzM2NCwianRpIjoicjZpNGstcEF2ekU4VTd4LTFya3JIQSIsImlhdCI6MTQ5MzU1MzM2NCwibmJmIjoxNDkzNTUzMjQ0LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3b3JsZC5yIiwid29ybGQudyIsInNlcnZlci5pbmZvLnIiXX0.VZCeU_M9xJKquSpGu0DgkX5aThUvqlChEcQOIG4aFlLkgfq76hf498GCdqLlAlk7RvkwnQUrwNa2kH8T-gNapgpWYnnwJ0cpWGE4LQ0urqFHetoJeiVyv6XVVp9khO4dsbcJLvVDzEr2Sgzwu3Bi7pkEg6BNwBQIEZRIwNxvQWIt9hnrdrvkId70C0mC9GkZC35_bEOWMkamw0TFUAimeStyZo3NJDwmH9EQmSN1523dF4Q2hFxhtfzOv-DQccIe8U2iG3tT3LJCSYjRJK0idt3NFq57WT0MA7vPSOFplTqCK_WfH5u-so_xKnltRKoKadXkBjHojznXO6nNhF38eQ";

    static Undertow server = null;

    @BeforeClass
    public static void setUp() throws Exception {
        RpcRouter rpcRouter = new RpcRouter();
        JwtVerifyHandler jwtVerifyHandler = new JwtVerifyHandler();
        jwtVerifyHandler.setNext(rpcRouter.getHandler());
        // Make sure that services are loaded in service map. It should be called by
        // Server module in a normal instance.
        RpcStartupHookProvider provider = new RpcStartupHookProvider();
        provider.onStartup();
        if(server == null) {
            logger.info("starting server");
            server = Undertow.builder()
                    .addHttpListener(8080, "localhost")
                    .setHandler(jwtVerifyHandler)
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
                request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
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

    /**
     * Test empty post request body and expect 400 error from the server.
     * 
     * @throws Exception
     */
    @Test
    public void testJsonRpcEmptyBody() throws Exception {
        UndertowClient client = UndertowClient.getInstance();

        String message = "";
        final CountDownLatch latch = new CountDownLatch(1);
        final List<String> responses = new CopyOnWriteArrayList<>();
        final ClientConnection connection = client.connect(new URI("http://localhost:8080"), worker, pool, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        try {
            connection.getIoThread().execute(() -> {
                final ClientRequest request = new ClientRequest().setMethod(Methods.POST).setPath("/api/json");
                request.getRequestHeaders().put(Headers.HOST, "localhost");
                request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
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
            Assert.assertTrue(responseBody.contains("ERR11201"));
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
                request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
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
