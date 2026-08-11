package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class GarminHttpClientIntegrationTest {
    private ServerSocket server;
    private ExecutorService serverExecutor;
    private String baseUrl;

    @Before
    public void setUp() throws Exception {
        server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
        serverExecutor = Executors.newCachedThreadPool();
        serverExecutor.execute(this::acceptRequests);
        baseUrl = "http://127.0.0.1:" + server.getLocalPort();
    }

    @After
    public void tearDown() {
        try {
            if (server != null) server.close();
        } catch (IOException ignored) { }
        if (serverExecutor != null) serverExecutor.shutdownNow();
    }

    @Test
    public void redirectCookiesAreReplayedAndClearable() throws Exception {
        GarminHttpClient client = new GarminHttpClient();

        GarminHttpClient.Response redirected = get(client, "/set-redirect", true);
        assertEquals(200, redirected.code);
        assertTrue(redirected.body.contains("redirect=beta"));
        assertTrue(get(client, "/echo", false).body.contains("redirect=beta"));

        client.clearCookies();
        assertEquals("", get(client, "/echo", false).body);
    }

    @Test
    public void clientsHaveIsolatedCookiesAndDoNotReplaceGlobalHandler() throws Exception {
        CookieHandler original = CookieHandler.getDefault();
        GarminHttpClient first = new GarminHttpClient();
        GarminHttpClient second = new GarminHttpClient();
        assertSame(original, CookieHandler.getDefault());

        ExecutorService clients = Executors.newFixedThreadPool(2);
        try {
            Future<String> firstCookie = clients.submit(() -> {
                assertEquals(204, get(first, "/set-alpha", false).code);
                return get(first, "/echo", false).body;
            });
            Future<String> secondCookie = clients.submit(() -> {
                assertEquals(204, get(second, "/set-bravo", false).code);
                return get(second, "/echo", false).body;
            });
            assertTrue(firstCookie.get().contains("session=alpha"));
            assertTrue(secondCookie.get().contains("session=bravo"));
        } finally {
            clients.shutdownNow();
        }
        assertSame(original, CookieHandler.getDefault());
    }

    private GarminHttpClient.Response get(GarminHttpClient client, String path,
                                          boolean followRedirects) throws Exception {
        return client.execute("GET", baseUrl + path, null, null,
                Collections.emptyMap(), followRedirects);
    }

    private void acceptRequests() {
        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                serverExecutor.execute(() -> handle(socket));
            } catch (IOException exception) {
                if (!server.isClosed()) throw new AssertionError(exception);
            }
        }
    }

    private static void handle(Socket socket) {
        try (Socket request = socket;
             BufferedReader input = new BufferedReader(new InputStreamReader(
                     request.getInputStream(), StandardCharsets.ISO_8859_1))) {
            String requestLine = input.readLine();
            String path = requestLine == null ? "" : requestLine.split(" ")[1];
            String cookie = "";
            String line;
            while ((line = input.readLine()) != null && !line.isEmpty()) {
                if (line.regionMatches(true, 0, "Cookie:", 0, 7)) {
                    cookie = line.substring(7).trim();
                }
            }
            if ("/set-alpha".equals(path)) {
                respond(request.getOutputStream(), 204, "No Content", "",
                        "Set-Cookie: session=alpha; Path=/\r\n");
            } else if ("/set-bravo".equals(path)) {
                respond(request.getOutputStream(), 204, "No Content", "",
                        "Set-Cookie: session=bravo; Path=/\r\n");
            } else if ("/set-redirect".equals(path)) {
                respond(request.getOutputStream(), 302, "Found", "redirect",
                        "Set-Cookie: redirect=beta; Path=/\r\nLocation: /echo\r\n");
            } else {
                respond(request.getOutputStream(), 200, "OK", cookie, "");
            }
        } catch (IOException ignored) { }
    }

    private static void respond(OutputStream output, int status, String reason, String body,
                                String extraHeaders) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 " + status + " " + reason + "\r\n"
                + extraHeaders
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.ISO_8859_1));
        output.write(bytes);
        output.flush();
    }
}
