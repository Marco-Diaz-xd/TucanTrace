package tucantrace.ui;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Servidor HTTP local mínimo que:
 * <ul>
 *   <li>Sirve la página del visor UML en {@code /}.</li>
 *   <li>Expone un stream de eventos en {@code /events} (Server-Sent Events).</li>
 * </ul>
 *
 * <p>Usa solo {@code com.sun.net.httpserver} (incluido en el JDK), sin dependencias.</p>
 */
public class LiveServer implements AutoCloseable {

    private final HttpServer server;
    private final String umlPage;
    private final String terminalPage;
    private final List<BlockingQueue<String>> clientes = new CopyOnWriteArrayList<>();
    private final int puerto;
    private volatile Runnable runHandler;

    public LiveServer(int puerto, String umlPage, String terminalPage) throws IOException {
        this.puerto = puerto;
        this.umlPage = umlPage;
        this.terminalPage = terminalPage;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        // Hilos daemon: no impiden que la JVM termine (los handlers SSE quedan
        // bloqueados esperando eventos y si fueran no-daemon colgarían el cierre).
        this.server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }));
        this.server.createContext("/", this::handleIndex);
        this.server.createContext("/terminal", this::handleTerminal);
        this.server.createContext("/events", this::handleEvents);
        this.server.createContext("/run", this::handleRun);
    }

    /** Registra la acción que se ejecuta al pedir /run (re-ejecutar el programa). */
    public void setRunHandler(Runnable handler) {
        this.runHandler = handler;
    }

    public void start() {
        server.start();
    }

    public int getPuerto() {
        return puerto;
    }

    public String getUrl() {
        return "http://127.0.0.1:" + puerto + "/";
    }

    public String getUrlTerminal() {
        return "http://127.0.0.1:" + puerto + "/terminal";
    }

    /** Nº de navegadores conectados al stream de eventos. */
    public int getClientesConectados() {
        return clientes.size();
    }

    /** Publica un evento JSON a todos los navegadores conectados. */
    public void publicar(String json) {
        for (BlockingQueue<String> q : clientes) {
            q.offer(json);
        }
    }

    // ------------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------------
    private void handleIndex(HttpExchange ex) throws IOException {
        sendHtml(ex, umlPage);
    }

    private void handleTerminal(HttpExchange ex) throws IOException {
        sendHtml(ex, terminalPage);
    }

    private void sendHtml(HttpExchange ex, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        h.add("Content-Type", "text/html; charset=utf-8");
        h.add("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private void handleRun(HttpExchange ex) throws IOException {
        Runnable h = runHandler;
        if (h != null) {
            new Thread(h, "tt-run").start();
        }
        byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        ex.getResponseHeaders().add("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private void handleEvents(HttpExchange ex) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.add("Content-Type", "text/event-stream; charset=utf-8");
        h.add("Cache-Control", "no-cache");
        h.add("Connection", "keep-alive");
        h.add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, 0);

        BlockingQueue<String> cola = new LinkedBlockingQueue<>();
        clientes.add(cola);

        try (OutputStream os = ex.getResponseBody()) {
            // Comentario inicial para abrir el stream
            os.write(": conectado a TucanTrace\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();

            while (true) {
                String msg = cola.take();
                os.write(("data: " + msg + "\n\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Navegador desconectado: normal
        } finally {
            clientes.remove(cola);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}