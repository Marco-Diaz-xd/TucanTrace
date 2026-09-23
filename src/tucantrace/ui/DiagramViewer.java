package tucantrace.ui;

import tucantrace.runtime.JDIClient;

import javax.swing.*;
import java.awt.*;

/**
 * Visor gráfico del diagrama UML con resaltado en vivo.
 * <p>
 * Implementación inicial en Swing (compatible con JDK 21 sin dependencias extra).
 * Futura versión: JavaFX para mejor renderizado SVG/animaciones.
 * </p>
 */
public class DiagramViewer extends JFrame {

    private final JLabel diagramLabel;
    private Image currentImage;

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

        // Panel de estado
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("Esperando conexión JDI...");
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    /**
     * Actualiza el diagrama con una nueva imagen (PNG/SVG renderizado).
     */
    public void updateDiagram(Image image) {
        this.currentImage = image;
        diagramLabel.setIcon(new ImageIcon(image));
        repaint();
    }

    /**
     * Resalta un elemento específico en el diagrama (clase, método, atributo).
     * <p>
     * Implementación real: sobreponer gráficamente un rectángulo de color
     * sobre las coordenadas del elemento en el SVG/PNG.
     * </p>
     */
    public void highlightElement(String elementName, Color color) {
        // TODO: Implementar overlay gráfico sobre el diagrama
        System.out.println("🎨 Highlight: " + elementName + " -> " + color);
    }

    /**
     * Limpia todos los resaltados.
     */
    public void clearHighlights() {
        // TODO: Limpiar overlays
    }

    /**
     * Método de conveniencia para lanzar el visor standalone.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DiagramViewer());
    }
}