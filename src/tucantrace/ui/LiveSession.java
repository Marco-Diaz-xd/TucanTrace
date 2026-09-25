package tucantrace.ui;

import tucantrace.runtime.JDIClient;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

/**
 * Orquesta una sesión de visualización en vivo:
 * sirve el diagrama UML y la terminal en el navegador, y publica los eventos
 * JDI (resaltado) y la salida del programa.
 */
public class LiveSession implements AutoCloseable {

    private final LiveServer server;
    private final long delayMs;

    /**
     * @param svg     contenido SVG del diagrama (generado por PlantUML)
     * @param puerto  puerto HTTP local
     * @param delayMs retardo artificial por evento (0 = sin retardo)
     */
    public LiveSession(String svg, int puerto, long delayMs) throws IOException {
        this.delayMs = delayMs;
        String umlPage = LiveDiagramPage.build(svg);
        String terminalPage = TerminalPage.build();
        this.server = new LiveServer(puerto, umlPage, terminalPage);
        this.server.start();
    }

    public String getUrl() {
        return server.getUrl();
    }

    public String getUrlTerminal() {
        return server.getUrlTerminal();
    }

    public int getPuerto() {
        return server.getPuerto();
    }

    /** Abre una URL en el navegador por defecto. */
    private void abrir(String url) {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {
            // fallback abajo
        }
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
        } catch (Exception e) {
            System.out.println("[!] Abre manualmente: " + url);
        }
    }

    /** Abre el visor UML en una pestaña. */
    public void abrirNavegador() {
        abrir(server.getUrl());
    }

    /** Abre el visor UML y la terminal en dos pestañas. */
    public void abrirDosPestanas() {
        abrir(server.getUrl());
        try {
            Thread.sleep(700);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        abrir(server.getUrlTerminal());
    }

    /** Publica un evento JDI (resaltado del diagrama). */
    public void publicar(JDIClient.JDIEvent ev) throws InterruptedException {
        publicar(ev, true);
    }

    /**
     * Publica un evento JDI.
     *
     * @param conDelay si es {@code true}, aplica el retardo configurado
     */
    public void publicar(JDIClient.JDIEvent ev, boolean conDelay) throws InterruptedException {
        String json = "{"
                + "\"kind\":\"" + esc(ev.kind()) + "\","
                + "\"className\":\"" + esc(ev.className()) + "\","
                + "\"member\":\"" + esc(ev.memberName()) + "\","
                + "\"value\":\"" + esc(ev.valueText()) + "\","
                + "\"text\":\"" + esc(ev.describe()) + "\""
                + "}";
        server.publicar(json);
        if (conDelay && delayMs > 0) {
            Thread.sleep(delayMs);
        }
    }

    /** Publica una línea de salida del programa (terminal). */
    public void publicarSalida(String linea) {
        server.publicar("{\"kind\":\"OUT\",\"text\":\"" + esc(linea) + "\"}");
    }

    /** Publica un mensaje informativo en la terminal. */
    public void publicarMeta(String texto) {
        server.publicar("{\"kind\":\"META\",\"text\":\"" + esc(texto) + "\"}");
    }

    /** Publica un error del programa en la terminal. */
    public void publicarError(String texto) {
        server.publicar("{\"kind\":\"ERR\",\"text\":\"" + esc(texto) + "\"}");
    }

    public boolean hayNavegador() {
        return server.getClientesConectados() > 0;
    }

    /** Registra la acción de re-ejecución (botón del navegador). */
    public void setRunHandler(Runnable handler) {
        server.setRunHandler(handler);
    }

    @Override
    public void close() {
        server.close();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}