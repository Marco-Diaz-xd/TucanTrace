package tucantrace.ui;

import tucantrace.runtime.JDIClient;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

/**
 * Visor del diagrama UML con resaltado en vivo.
 * <p>
 * Puede iniciar el servidor web embebido ({@link TraceWebServer}) para visualización
 * interactiva en navegador con soporte completo de SVG y CSS, y opcionalmente una ventana Swing local.
 * </p>
 */
public class DiagramViewer extends JFrame {

    private final JLabel diagramLabel;
    private Image currentImage;
    private TraceWebServer webServer;

    public DiagramViewer() {
        super("TucanTrace - Live UML Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        diagramLabel = new JLabel();
        diagramLabel.setHorizontalAlignment(SwingConstants.CENTER);
        diagramLabel.setVerticalAlignment(SwingConstants.CENTER);

        JScrollPane scrollPane = new JScrollPane(diagramLabel);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // Panel de estado y controles
        JPanel statusPanel = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel(" Servidor Web SVG activo en http://localhost:8080/");
        statusPanel.add(statusLabel, BorderLayout.WEST);

        JButton openBrowserBtn = new JButton("Abrir en Navegador 🌐");
        openBrowserBtn.addActionListener(e -> openInBrowser("http://localhost:8080/"));
        statusPanel.add(openBrowserBtn, BorderLayout.EAST);

        add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Vincula el servidor web al DiagramViewer.
     */
    public void setWebServer(TraceWebServer webServer) {
        this.webServer = webServer;
    }

    /**
     * Obtiene el servidor web asociado.
     */
    public TraceWebServer getWebServer() {
        return webServer;
    }

    /**
     * Abre la URL en el navegador por defecto del sistema.
     */
    public static void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                System.out.println("🔗 Abre la interfaz en tu navegador: " + url);
            }
        } catch (Exception e) {
            System.out.println("🔗 Abre la interfaz en tu navegador: " + url);
        }
    }

    /**
     * Actualiza el diagrama con una nueva imagen (PNG renderizado) y actualiza el visor web con el SVG.
     */
    public void updateDiagram(Image image) {
        this.currentImage = image;
        diagramLabel.setIcon(new ImageIcon(image));
        repaint();
    }

    /**
     * Actualiza el SVG servido por el servidor web embebido.
     */
    public void updateSvg(String svg) {
        if (webServer != null) {
            webServer.updateSvg(svg);
        }
    }

    /**
     * Resalta un elemento específico en el diagrama (clase, método, atributo).
     */
    public void highlightElement(String elementName, Color color) {
        System.out.println("🎨 Highlight: " + elementName + " -> " + color);
        if (webServer != null) {
            if (elementName.contains(".")) {
                String[] parts = elementName.split("\\.", 2);
                webServer.highlightMethod(parts[0], parts[1], true);
            } else {
                webServer.highlightClass(elementName);
            }
        }
    }

    /**
     * Limpia todos los resaltados.
     */
    public void clearHighlights() {
        if (webServer != null) {
            webServer.broadcastEvent("clear-highlights", "{}");
        }
    }

    /**
     * Conecta el cliente JDI con el servidor web para transmitir los eventos en vivo.
     */
    public void connectJDI(JDIClient jdiClient) {
        if (jdiClient == null || webServer == null) return;

        jdiClient.addListener(event -> {
            String className = event.getSimpleClassName();
            if (event.isMethodEntry()) {
                webServer.highlightMethod(className, event.getMethodName(), true);
            } else if (event.isMethodExit()) {
                webServer.highlightMethod(className, event.getMethodName(), false);
            } else if (event.isFieldModification()) {
                webServer.highlightField(className, event.getFieldName(), event.getValueToBe());
            } else if (event.isClassPrepare()) {
                webServer.highlightClass(className);
            }
        });
    }

    /**
     * Método de conveniencia para lanzar el visor standalone.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DiagramViewer viewer = new DiagramViewer();
            viewer.setVisible(true);
        });
    }
}