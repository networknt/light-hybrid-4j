package com.networknt.rpc.router;

import com.networknt.client.Http2Client;
import com.networknt.exception.ClientException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.net.URLEncoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by steve on 12/04/17.
 */
public class RpcRouterTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(RpcRouterTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;

    static Undertow server2 = null;
    @BeforeClass
    public static void setUp() {
        if (server2 == null) {
            logger.info("starting server2");
            HttpHandler handler = getJwksHandler();
            server2 = Undertow.builder()
                    .addHttpListener(7082, "localhost")
                    .setHandler(handler)
                    .build();
            server2.start();
        }

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server2 != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
            server2.stop();
            logger.info("The server2 is stopped.");
        }

    }

    static RoutingHandler getJwksHandler() {
        return Handlers.routing()
                .add(Methods.GET, "/oauth2/N2CMw0HGQXeLvC1wBfln2A/keys", exchange -> {
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.getResponseSender().send("{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"Tj_l_tIBTginOtQbL0Pv5w\",\"n\":\"0YRbWAb1FGDpPUUcrIpJC6BwlswlKMS-z2wMAobdo0BNxNa7hG_gIHVPkXu14Jfo1JhUhS4wES3DdY3a6olqPcRN1TCCUVHd-1TLd1BBS-yq9tdJ6HCewhe5fXonaRRKwutvoH7i_eR4m3fQ1GoVzVAA3IngpTr4ptnM3Ef3fj-5wZYmitzrRUyQtfARTl3qGaXP_g8pHFAP0zrNVvOnV-jcNMKm8YZNcgcs1SuLSFtUDXpf7Nr2_xOhiNM-biES6Dza1sMLrlxULFuctudO9lykB7yFh3LHMxtIZyIUHuy0RbjuOGC5PmDowLttZpPI_j4ynJHAaAWr8Ddz764WdQ\",\"e\":\"AQAB\"}]}");
                });

    }

    private static String auth = "Bearer eyJraWQiOiJUal9sX3RJQlRnaW5PdFFiTDBQdjV3IiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MjAxOTg3MzkyOSwianRpIjoiLWZaNzY5cWxFa05RRnFGQWNkLWlGQSIsImlhdCI6MTcwNDUxMzkyOSwibmJmIjoxNzA0NTEzODA5LCJ2ZXJzaW9uIjoiJzEuMCciLCJ1c2VyX2lkIjoic3RldmUiLCJ1c2VyX3R5cGUiOiJFTVBMT1lFRSIsImNsaWVudF9pZCI6ImY3ZDQyMzQ4LWM2NDctNGVmYi1hNTJkLTRjNTc4NzQyMWU3MiIsInJvbGVzIjoidXNlciIsInNjb3BlIjpbIndvcmxkLnIiLCJ3b3JsZC53Iiwic2VydmVyLmluZm8uciJdfQ.za1C5bh4QxI6McXeogmXtVYKS611VVPNn14PCcV5Q8NadVDIcgL7TCjT_KKbU35BiV0-xlu1BdyzMEZOr3E1xlhK-WB-eAHRF__IXydJXN-pWSo6wp-Jq4-rzZlW_e4kprlT_B0GHngzlJuru5Y-00Mh8bnhUuy1QTxF3JFHnz-62bJZmyeRx9iveNGvUTF5AJGPHhRoHMMCFJjSakiuy8El-wrTFN-Zi3e09n3xxPHj6qH_mDG_WyHMJ5r3bew9asMxU8QV_A0c7jkcwxJRg6UcVLSecmd-GxrCmjw3hiIJKInyqBBIa0uZkHmm4T1c9XiG1LbFHNhHwi0H_sykoQ";

    @Test
    public void testJsonRpcValidationError() throws Exception {
        Http2Client client = Http2Client.getInstance();
        String message = "{\"host\":\"www.networknt.com\",\"service\":\"account\",\"action\":\"delete\",\"version\":\"0.1.1\",\"data\":{\"accountNo\":\"1234567\"}}";

        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assert.assertTrue(body.contains("ERR11004"));
    }

    /**
     * Test empty post request body and expect 400 error from the server.
     * 
     * @throws Exception
     */
    @Test
    public void testJsonRpcEmptyBody() throws Exception {
        Http2Client client = Http2Client.getInstance();

        String message = "";
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assert.assertTrue(body.contains("ERR11201"));
    }


    // Ignore it as we cannot get the jwks and x509 certificate is not supported anymore.
    @Test
    public void testJsonRpcPostNoError() throws Exception {
        Http2Client client = Http2Client.getInstance();

        String message = "{\"host\":\"www.networknt.com\",\"service\":\"account\",\"action\":\"delete\",\"version\":\"0.1.1\",\"data\":{\"accountNo\":\"1234567\",\"accountType\":\"P\"}}";
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setPath("/api/json").setMethod(Methods.POST);
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(request, client.createClientCallback(reference, latch, message));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        int statusCode = reference.get().getResponseCode();
        System.out.println("statusCode = " + statusCode);
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        System.out.println("body = " + body);
        Assert.assertEquals("OK", body);
    }


    // Ignore it as we cannot get the jwks and x509 certificate is not supported anymore.
    @Test
    public void testJsonRpcGetNoError() throws Exception {
        Http2Client client = Http2Client.getInstance();
        String message = "/api/json?cmd=" + URLEncoder.encode("{\"host\":\"www.networknt.com\",\"service\":\"account\",\"action\":\"delete\",\"version\":\"0.1.1\",\"data\":{\"accountNo\":\"1234567\",\"accountType\":\"P\"}}", "UTF-8");
        System.out.println("message = " + message);

        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("body = " + body);
            Assert.assertEquals(200, statusCode);
            Assert.assertEquals("OK", body);
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
    }

    @Test
    public void testJsonRpcGetNoParamValue() throws Exception {
        Http2Client client = Http2Client.getInstance();
        String message = "/api/json?cmd=";
        System.out.println("message = " + message);

        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(message);
            request.getRequestHeaders().put(Headers.AUTHORIZATION, auth);
            connection.sendRequest(request, client.createClientCallback(reference, latch));
            latch.await();
            int statusCode = reference.get().getResponseCode();
            String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
            System.out.println("statusCode = " + statusCode + " body = " + body);
            Assert.assertEquals(400, statusCode);
            Assert.assertTrue(body.contains("ERR11202"));
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
    }
}
