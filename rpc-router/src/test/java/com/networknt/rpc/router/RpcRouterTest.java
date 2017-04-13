package com.networknt.rpc.router;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.UndertowClient;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.net.URI;

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
    public void testJsonRpc() throws Exception {
        UndertowClient client = UndertowClient.getInstance();
        String message = "{\"host\":\"www.networknt.com\",\"service\":\"account\",\"action\":\"delete\",\"version\":\"0.1.1\",\"accountNo\":\"1234567\"}";
        final ClientConnection connection = client.connect(new URI("h2c-prior://localhost:8080"), worker, pool, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();

    }

    @Test
    public void testColferRpc() {

    }
}
