package tucantrace.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servidor web embebido ligero basado en {@link com.sun.net.httpserver.HttpServer}.
 * <p>
 * Proporciona:
 * <ul>
 *   <li>Visualizador web responsivo con SVG interactivo y estilos CSS para resaltar en vivo.</li>
 *   <li>Endpoints REST para obtener el SVG actual y simular o enviar eventos de depuración.</li>
 *   <li>Server-Sent Events (SSE) en <code>/events</code> para streaming en tiempo real a los navegadores.</li>
 * </ul>
 * </p>
 */
public class TraceWebServer implements AutoCloseable {

    private final int port;
    private HttpServer server;
    private volatile String currentSvg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><text x=\"20\" y=\"30\">No SVG loaded</text></svg>";
    private final List<OutputStream> sseClients = new CopyOnWriteArrayList<>();
    private final List<TraceEventListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Interfaz para recibir notificaciones de eventos transmitidos.
     */
    @FunctionalInterface
    public interface TraceEventListener {
        void onEvent(String eventType, String dataJson);
    }

    public TraceWebServer(int port) {
        this.port = port;
    }

    public TraceWebServer() {
        this(8080);
    }

    /**
     * Inicia el servidor HTTP y SSE.
     */
    public synchronized void start() throws IOException {
        if (running.get()) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Rutas
        server.createContext("/", new MainHtmlHandler());
        server.createContext("/diagram.svg", new SvgHandler());
        server.createContext("/events", new SseHandler());
        server.createContext("/api/highlight", new HighlightApiHandler());
        server.createContext("/api/clear", new ClearApiHandler());
        server.createContext("/api/status", new StatusApiHandler());

        server.start();
        running.set(true);
        System.out.println("🌐 TucanTrace Web Server iniciado en http://localhost:" + port + "/");
    }

    /**
     * Actualiza el contenido SVG servido.
     *
     * @param svg código SVG completo
     */
    public void updateSvg(String svg) {
        this.currentSvg = svg;
        broadcastEvent("svg-update", "{\"status\":\"updated\"}");
    }

    /**
     * Obtiene el SVG actual.
     */
    public String getSvg() {
        return currentSvg;
    }

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void addListener(TraceEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TraceEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Emite un evento de resaltado de método (MethodEntry o MethodExit) a todos los clientes web conectados.
     */
    public void highlightMethod(String className, String methodName, boolean isEntry) {
        String json = String.format("{\"type\":\"%s\",\"className\":\"%s\",\"methodName\":\"%s\",\"timestamp\":%d}",
                isEntry ? "MethodEntry" : "MethodExit",
                escapeJson(className),
                escapeJson(methodName),
                System.currentTimeMillis());
        broadcastEvent("trace-event", json);
    }

    /**
     * Emite un evento de modificación de atributo (FieldModification) a todos los clientes web conectados.
     */
    public void highlightField(String className, String fieldName, String value) {
        String json = String.format("{\"type\":\"FieldModification\",\"className\":\"%s\",\"fieldName\":\"%s\",\"value\":\"%s\",\"timestamp\":%d}",
                escapeJson(className),
                escapeJson(fieldName),
                escapeJson(value != null ? value : ""),
                System.currentTimeMillis());
        broadcastEvent("trace-event", json);
    }

    /**
     * Emite un evento de preparación de clase (ClassPrepare) a todos los clientes web conectados.
     */
    public void highlightClass(String className) {
        String json = String.format("{\"type\":\"ClassPrepare\",\"className\":\"%s\",\"timestamp\":%d}",
                escapeJson(className),
                System.currentTimeMillis());
        broadcastEvent("trace-event", json);
    }

    /**
     * Transmite un evento arbitrario con formato JSON a través del canal SSE.
     */
    public void broadcastEvent(String eventType, String jsonData) {
        for (TraceEventListener l : listeners) {
            try {
                l.onEvent(eventType, jsonData);
            } catch (Exception ignored) {}
        }

        byte[] payload = ("event: " + eventType + "\ndata: " + jsonData + "\n\n").getBytes(StandardCharsets.UTF_8);
        List<OutputStream> toRemove = new CopyOnWriteArrayList<>();

        for (OutputStream os : sseClients) {
            try {
                os.write(payload);
                os.flush();
            } catch (IOException e) {
                toRemove.add(os);
            }
        }

        sseClients.removeAll(toRemove);
    }

    @Override
    public synchronized void close() {
        if (!running.get()) {
            return;
        }
        running.set(false);
        for (OutputStream os : sseClients) {
            try {
                os.close();
            } catch (IOException ignored) {}
        }
        sseClients.clear();
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 TucanTrace Web Server detenido.");
        }
    }

    // =========================================================================
    // Handlers HTTP
    // =========================================================================

    private class MainHtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String html = getViewerHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class SvgHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            byte[] bytes = currentSvg.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "image/svg+xml; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class SseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            // Chunked transfer encoding
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            sseClients.add(os);

            // Mensaje inicial de bienvenida
            String initial = "event: connected\ndata: {\"status\":\"ready\",\"clients\":" + sseClients.size() + "}\n\n";
            try {
                os.write(initial.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException e) {
                sseClients.remove(os);
            }
        }
    }

    private class HighlightApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String type = "MethodEntry";
            String className = "Main";
            String memberName = "main";
            String value = "";

            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        if ("type".equalsIgnoreCase(pair[0])) type = pair[1];
                        else if ("class".equalsIgnoreCase(pair[0])) className = pair[1];
                        else if ("member".equalsIgnoreCase(pair[0]) || "method".equalsIgnoreCase(pair[0]) || "field".equalsIgnoreCase(pair[0])) memberName = pair[1];
                        else if ("value".equalsIgnoreCase(pair[0])) value = pair[1];
                    }
                }
            }

            if ("FieldModification".equalsIgnoreCase(type)) {
                highlightField(className, memberName, value);
            } else if ("ClassPrepare".equalsIgnoreCase(type)) {
                highlightClass(className);
            } else {
                highlightMethod(className, memberName, !"MethodExit".equalsIgnoreCase(type));
            }

            byte[] resp = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    private class ClearApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            broadcastEvent("clear-highlights", "{}");
            byte[] resp = "{\"status\":\"cleared\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    private class StatusApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            String json = String.format("{\"running\":%b,\"clients\":%d,\"port\":%d}",
                    running.get(), sseClients.size(), port);
            byte[] resp = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Retorna la plantilla HTML completa con diseño moderno, panel de control,
     * visor SVG interactivo y soporte de eventos en vivo SSE.
     */
    private String getViewerHtml() {
        return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>TucanTrace - Live UML Visualizer</title>
          <style>
            :root {
              --bg-main: #0f111a;
              --bg-panel: #1e1e2e;
              --bg-card: #252538;
              --border-color: #313244;
              --text-main: #cdd6f4;
              --text-muted: #a6adc8;
              --accent-blue: #89b4fa;
              --accent-green: #a6e3a1;
              --accent-red: #f38ba8;
              --accent-yellow: #f9e2af;
              --accent-purple: #cba6f7;
            }

            * { box-sizing: border-box; margin: 0; padding: 0; }

            body {
              font-family: 'Segoe UI', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
              background-color: var(--bg-main);
              color: var(--text-main);
              display: flex;
              flex-direction: column;
              height: 100vh;
              overflow: hidden;
            }

            header {
              background-color: var(--bg-panel);
              border-bottom: 1px solid var(--border-color);
              padding: 12px 24px;
              display: flex;
              align-items: center;
              justify-content: space-between;
              flex-shrink: 0;
            }

            .logo-title {
              display: flex;
              align-items: center;
              gap: 12px;
            }

            .logo-title h1 {
              font-size: 20px;
              font-weight: 700;
              color: var(--accent-blue);
              letter-spacing: 0.5px;
            }

            .badge {
              font-size: 11px;
              font-weight: 600;
              padding: 3px 8px;
              border-radius: 6px;
              text-transform: uppercase;
            }

            .badge-live {
              background-color: rgba(166, 227, 161, 0.2);
              color: var(--accent-green);
              border: 1px solid var(--accent-green);
              display: flex;
              align-items: center;
              gap: 6px;
            }

            .dot {
              width: 8px;
              height: 8px;
              border-radius: 50%;
              background-color: var(--accent-green);
              animation: pulse 1.5s infinite ease-in-out;
            }

            @keyframes pulse {
              0% { opacity: 0.4; transform: scale(0.8); }
              50% { opacity: 1; transform: scale(1.2); }
              100% { opacity: 0.4; transform: scale(0.8); }
            }

            .actions-bar {
              display: flex;
              align-items: center;
              gap: 12px;
            }

            .btn {
              background: var(--bg-card);
              color: var(--text-main);
              border: 1px solid var(--border-color);
              padding: 7px 14px;
              border-radius: 6px;
              font-size: 13px;
              font-weight: 500;
              cursor: pointer;
              transition: all 0.2s ease;
              display: inline-flex;
              align-items: center;
              gap: 6px;
            }

            .btn:hover {
              background: var(--border-color);
              color: #fff;
              border-color: var(--accent-blue);
            }

            .btn-primary {
              background: var(--accent-blue);
              color: #11111b;
              font-weight: 600;
              border-color: var(--accent-blue);
            }

            .btn-primary:hover {
              background: #b4befe;
            }

            .btn-danger {
              background: rgba(243, 139, 168, 0.15);
              color: var(--accent-red);
              border-color: rgba(243, 139, 168, 0.4);
            }

            .btn-danger:hover {
              background: rgba(243, 139, 168, 0.3);
            }

            main {
              display: flex;
              flex: 1;
              overflow: hidden;
            }

            #diagram-container {
              flex: 1;
              background: #181825;
              overflow: auto;
              display: flex;
              align-items: center;
              justify-content: center;
              padding: 24px;
              position: relative;
            }

            #diagram-svg-wrapper {
              background: #ffffff;
              border-radius: 8px;
              box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
              padding: 20px;
              max-width: 95%;
              max-height: 95%;
              overflow: visible;
              transition: transform 0.2s ease;
            }

            #diagram-svg-wrapper svg {
              display: block;
              max-width: 100%;
              height: auto;
            }

            /* Live Highlighting CSS */
            .highlight-active-class {
              fill: #fef08a !important;
              stroke: #e11d48 !important;
              stroke-width: 3px !important;
              filter: drop-shadow(0 0 12px rgba(225, 29, 72, 0.8)) !important;
              transition: all 0.2s ease-in-out;
            }

            .highlight-active-method {
              fill: #e11d48 !important;
              font-weight: 900 !important;
              font-size: 12px !important;
              text-decoration: underline !important;
              filter: drop-shadow(0 0 6px rgba(225, 29, 72, 0.9)) !important;
              transition: all 0.2s ease-in-out;
            }

            .highlight-active-field {
              fill: #7c3aed !important;
              font-weight: 900 !important;
              font-size: 12px !important;
              filter: drop-shadow(0 0 6px rgba(124, 58, 237, 0.9)) !important;
              transition: all 0.2s ease-in-out;
            }

            .highlight-class-prepare {
              fill: #bae6fd !important;
              stroke: #0284c7 !important;
              stroke-width: 3px !important;
              filter: drop-shadow(0 0 10px rgba(2, 132, 199, 0.7)) !important;
              transition: all 0.2s ease-in-out;
            }

            /* Sidebar Logs */
            aside {
              width: 380px;
              background-color: var(--bg-panel);
              border-left: 1px solid var(--border-color);
              display: flex;
              flex-direction: column;
              flex-shrink: 0;
            }

            .aside-header {
              padding: 14px 18px;
              border-bottom: 1px solid var(--border-color);
              font-size: 14px;
              font-weight: 600;
              display: flex;
              justify-content: space-between;
              align-items: center;
            }

            #event-log {
              flex: 1;
              overflow-y: auto;
              padding: 12px;
              display: flex;
              flex-direction: column;
              gap: 8px;
              font-family: 'Consolas', 'Courier New', monospace;
              font-size: 12px;
            }

            .log-item {
              background: var(--bg-card);
              border: 1px solid var(--border-color);
              border-left: 4px solid var(--accent-blue);
              border-radius: 6px;
              padding: 8px 12px;
              display: flex;
              flex-direction: column;
              gap: 4px;
              animation: slideIn 0.2s ease;
            }

            .log-item.method-entry { border-left-color: var(--accent-red); }
            .log-item.method-exit { border-left-color: var(--accent-yellow); }
            .log-item.field-mod { border-left-color: var(--accent-purple); }
            .log-item.class-prepare { border-left-color: var(--accent-blue); }

            @keyframes slideIn {
              from { opacity: 0; transform: translateX(10px); }
              to { opacity: 1; transform: translateX(0); }
            }

            .log-item-header {
              display: flex;
              justify-content: space-between;
              font-size: 11px;
              color: var(--text-muted);
            }

            .log-item-title {
              font-weight: 600;
              color: #fff;
            }

            .log-item-detail {
              color: var(--text-muted);
              font-size: 11px;
              word-break: break-all;
            }

            /* Controls footer */
            .sidebar-footer {
              padding: 12px;
              border-top: 1px solid var(--border-color);
              display: flex;
              flex-direction: column;
              gap: 8px;
            }

            .sim-buttons {
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 8px;
            }
          </style>
        </head>
        <body>
          <header>
            <div class="logo-title">
              <h1>🦜 TucanTrace</h1>
              <span class="badge badge-live"><span class="dot"></span> Live SVG Highlighting</span>
            </div>
            <div class="actions-bar">
              <button class="btn" onclick="zoomIn()">🔍 Zoom +</button>
              <button class="btn" onclick="zoomOut()">🔍 Zoom -</button>
              <button class="btn" onclick="resetZoom()">↺ Reset</button>
              <button class="btn btn-danger" onclick="clearHighlights()">🧹 Limpiar Resaltados</button>
            </div>
          </header>

          <main>
            <section id="diagram-container">
              <div id="diagram-svg-wrapper">
                <!-- SVG inyectado aquí -->
              </div>
            </section>

            <aside>
              <div class="aside-header">
                <span>📡 Eventos de Ejecución JDI</span>
                <span id="event-count" class="badge" style="background: var(--bg-card);">0 eventos</span>
              </div>
              <div id="event-log">
                <!-- Logs en vivo -->
              </div>
              <div class="sidebar-footer">
                <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 4px;">Simulación interactiva (demo):</div>
                <div class="sim-buttons">
                  <button class="btn" onclick="simulateEvent('MethodEntry', 'DiagramViewer', 'updateDiagram')">▶ MethodEntry</button>
                  <button class="btn" onclick="simulateEvent('MethodExit', 'DiagramViewer', 'updateDiagram')">⏹ MethodExit</button>
                  <button class="btn" onclick="simulateEvent('FieldModification', 'DiagramViewer', 'currentImage', 'Image@4a2f8b')">✎ FieldMod</button>
                  <button class="btn" onclick="simulateEvent('ClassPrepare', 'TraceAgent', '')">📦 ClassPrepare</button>
                </div>
              </div>
            </aside>
          </main>

          <script>
            let currentZoom = 1.0;
            let eventCounter = 0;
            const wrapper = document.getElementById('diagram-svg-wrapper');
            const eventLog = document.getElementById('event-log');
            const eventCountBadge = document.getElementById('event-count');

            // Cargar SVG inicial
            function loadSvg() {
              fetch('/diagram.svg')
                .then(res => res.text())
                .then(svgText => {
                  wrapper.innerHTML = svgText;
                  bindSvgEvents();
                })
                .catch(err => console.error('Error cargando SVG:', err));
            }

            loadSvg();

            // Zoom controls
            function zoomIn() {
              currentZoom += 0.15;
              wrapper.style.transform = `scale(${currentZoom})`;
            }

            function zoomOut() {
              if (currentZoom > 0.3) {
                currentZoom -= 0.15;
                wrapper.style.transform = `scale(${currentZoom})`;
              }
            }

            function resetZoom() {
              currentZoom = 1.0;
              wrapper.style.transform = 'scale(1)';
            }

            // Bind click handlers to SVG elements
            function bindSvgEvents() {
              const svg = wrapper.querySelector('svg');
              if (!svg) return;

              // Click en clases
              svg.querySelectorAll('.uml-class-group, .uml-class-box').forEach(el => {
                el.addEventListener('click', (e) => {
                  const clsName = el.getAttribute('data-class') || el.id.replace('class_', '').replace('elem_', '');
                  addLogItem('ManualClick', clsName, 'Elemento seleccionado');
                });
              });
            }

            // Server-Sent Events (SSE)
            const eventSource = new EventSource('/events');

            eventSource.addEventListener('connected', (e) => {
              console.log('SSE conectado:', e.data);
            });

            eventSource.addEventListener('svg-update', (e) => {
              loadSvg();
            });

            eventSource.addEventListener('clear-highlights', (e) => {
              clearSvgHighlights();
            });

            eventSource.addEventListener('trace-event', (e) => {
              try {
                const event = JSON.parse(e.data);
                handleTraceEvent(event);
              } catch (err) {
                console.error('Error parseando evento SSE:', err);
              }
            });

            function handleTraceEvent(event) {
              const type = event.type;
              const cls = event.className;
              const member = event.methodName || event.fieldName || '';
              const value = event.value || '';

              addLogItem(type, cls, member + (value ? ' = ' + value : ''));
              applySvgHighlight(type, cls, member);
            }

            function applySvgHighlight(type, className, memberName) {
              const svg = wrapper.querySelector('svg');
              if (!svg) return;

              // Simple class name (strip package if any)
              const simpleCls = className.includes('.') ? className.split('.').pop() : className;

              // 1. Resaltar rectángulo de la clase
              const classRect = svg.querySelector(`#class_${simpleCls}`) || svg.querySelector(`#${simpleCls}`) || svg.querySelector(`[data-class="${simpleCls}"]`);
              if (classRect) {
                if (type === 'ClassPrepare') {
                  classRect.classList.add('highlight-class-prepare');
                  setTimeout(() => classRect.classList.remove('highlight-class-prepare'), 3000);
                } else {
                  classRect.classList.add('highlight-active-class');
                  setTimeout(() => classRect.classList.remove('highlight-active-class'), 2500);
                }
              }

              // 2. Resaltar método
              if (type === 'MethodEntry' || type === 'MethodExit') {
                const methodEl = svg.querySelector(`#method_${simpleCls}_${memberName}`) ||
                                 svg.querySelector(`[data-class="${simpleCls}"][data-method="${memberName}"]`);
                if (methodEl) {
                  methodEl.classList.add('highlight-active-method');
                  setTimeout(() => methodEl.classList.remove('highlight-active-method'), 2500);
                }
              }

              // 3. Resaltar atributo / campo
              if (type === 'FieldModification') {
                const fieldEl = svg.querySelector(`#field_${simpleCls}_${memberName}`) ||
                                svg.querySelector(`[data-class="${simpleCls}"][data-field="${memberName}"]`);
                if (fieldEl) {
                  fieldEl.classList.add('highlight-active-field');
                  setTimeout(() => fieldEl.classList.remove('highlight-active-field'), 2500);
                }
              }
            }

            function clearSvgHighlights() {
              const svg = wrapper.querySelector('svg');
              if (!svg) return;
              svg.querySelectorAll('.highlight-active-class, .highlight-class-prepare, .highlight-active-method, .highlight-active-field')
                 .forEach(el => {
                    el.classList.remove('highlight-active-class', 'highlight-class-prepare', 'highlight-active-method', 'highlight-active-field');
                 });
            }

            function clearHighlights() {
              clearSvgHighlights();
              fetch('/api/clear').catch(() => {});
            }

            function addLogItem(type, className, details) {
              eventCounter++;
              eventCountBadge.textContent = eventCounter + ' eventos';

              const item = document.createElement('div');
              let typeClass = 'method-entry';
              if (type === 'MethodExit') typeClass = 'method-exit';
              else if (type === 'FieldModification') typeClass = 'field-mod';
              else if (type === 'ClassPrepare') typeClass = 'class-prepare';

              item.className = 'log-item ' + typeClass;

              const time = new Date().toLocaleTimeString();
              item.innerHTML = `
                <div class="log-item-header">
                  <span>${type}</span>
                  <span>${time}</span>
                </div>
                <div class="log-item-title">${className}</div>
                ${details ? `<div class="log-item-detail">${details}</div>` : ''}
              `;

              eventLog.insertBefore(item, eventLog.firstChild);

              // Limitar tamaño de log a 50
              while (eventLog.children.length > 50) {
                eventLog.removeChild(eventLog.lastChild);
              }
            }

            // Simulación manual desde cliente
            function simulateEvent(type, cls, member, value) {
              fetch(`/api/highlight?type=${encodeURIComponent(type)}&class=${encodeURIComponent(cls)}&member=${encodeURIComponent(member || '')}&value=${encodeURIComponent(value || '')}`)
                .catch(err => console.error('Error simulando evento:', err));
            }
          </script>
        </body>
        </html>
        """;
    }
}
