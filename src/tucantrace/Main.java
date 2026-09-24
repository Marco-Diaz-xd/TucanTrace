package tucantrace;

import tucantrace.parser.JavaParserAdapter;
import tucantrace.parser.PlantUMLGenerator;
import tucantrace.runtime.JDIClient;
import tucantrace.ui.DiagramViewer;
import tucantrace.ui.TraceWebServer;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

/**
 * Punto de entrada principal de TucanTrace.
 * <p>
 * Modo demo: parsea un directorio de código Java, genera el UML estático con SVG enriquecido,
 * inicia el servidor web embebido para visualización interactiva y (opcional) se conecta vía JDI
 * a una JVM en ejecución para transmitir y resaltar eventos en tiempo real.
 * </p>
 *
 * @author Equipo TucanTrace
 * @version 0.1.0 (MVP)
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("  TUCANTRACE v0.1 - Live UML Visualization for Java     ");
        System.out.println("  Universidad de la Amazonia - Ingeniería de Sistemas   ");
        System.out.println("=========================================================\n");

        // 1. Configuración por defecto (puede venir de args o properties)
        String sourceDir = args.length > 0 ? args[0] : "src";
        boolean enableJDI = false;
        int webPort = 8080;

        for (int i = 1; i < args.length; i++) {
            if ("--jdi".equalsIgnoreCase(args[i])) {
                enableJDI = true;
            } else if (args[i].startsWith("--port=")) {
                try {
                    webPort = Integer.parseInt(args[i].substring(7));
                } catch (NumberFormatException ignored) {}
            }
        }

        System.out.println("Directorio de código fuente: " + sourceDir);
        System.out.println("Servidor Web puerto: " + webPort);
        System.out.println("JDI habilitado: " + enableJDI);
        System.out.println();

        // 2. Paso estático: parsear código → UML
        System.out.println("--- 1. Análisis estático (JavaParser → PlantUML) ---");
        String plantUML;
        String interactiveSvg;
        try {
            Path srcPath = Paths.get(sourceDir).toAbsolutePath().normalize();
            if (!srcPath.toFile().exists()) {
                System.err.println("❌ Directorio no encontrado: " + srcPath);
                System.err.println("Uso: java tucantrace.Main <directorio-fuente> [--jdi] [--port=8080]");
                return;
            }

            JavaParserAdapter parser = new JavaParserAdapter();
            var units = parser.parseProject(srcPath);
            var ast = parser.extractClassInfo(units);
            System.out.println("  Clases parseadas: " + ast.size());

            PlantUMLGenerator generator = new PlantUMLGenerator();
            plantUML = generator.generate(ast);
            System.out.println("  PlantUML generado (" + plantUML.length() + " chars)");

            // Guardar .puml y .svg para inspección
            generator.saveToFile(plantUML, "build/tucantrace-diagram.puml");
            interactiveSvg = generator.generateInteractiveSVG(plantUML, ast);
            Path svgPath = Paths.get("build/tucantrace-diagram.svg");
            if (svgPath.getParent() != null) {
                Files.createDirectories(svgPath.getParent());
            }
            Files.writeString(svgPath, interactiveSvg, StandardCharsets.UTF_8);
            System.out.println("  Archivo .puml guardado en build/tucantrace-diagram.puml");
            System.out.println("  Archivo .svg interactivo guardado en build/tucantrace-diagram.svg");

        } catch (Exception e) {
            System.err.println("❌ Error en análisis estático: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 3. Iniciar Servidor Web embebido
        System.out.println("\n--- 2. Servidor Web Embebido (Visor SVG Live) ---");
        TraceWebServer webServer = new TraceWebServer(webPort);
        webServer.updateSvg(interactiveSvg);

        try {
            webServer.start();
            System.out.println("  ✅ Servidor Web iniciado correctamente en http://localhost:" + webPort + "/");
        } catch (Exception e) {
            System.err.println("❌ Error iniciando servidor web: " + e.getMessage());
        }

        // 4. (Opcional) JDI en vivo
        JDIClient client = null;
        if (enableJDI) {
            System.out.println("\n--- 3. Conexión JDI en vivo ---");
            System.out.println("  Conectando a localhost:5005 ...");
            try {
                client = new JDIClient("localhost", 5005, "tucantrace.*");
                client.addListener(event -> {
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
                client.connect();
                System.out.println("  ✅ Conectado a JVM objetivo");
                System.out.println("  Escuchando eventos: MethodEntry, FieldModification, ClassPrepare...");
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo conectar a JDI en localhost:5005: " + e.getMessage());
                System.out.println("  (El servidor web continuará funcionando en modo interactivo/simulación)");
            }
        }

        System.out.println("\n=========================================================");
        System.out.println("  TUCANTRACE ACTIVO: Abre http://localhost:" + webPort + "/ en tu navegador");
        System.out.println("  Presiona ENTER o Ctrl+C para finalizar...");
        System.out.println("=========================================================");

        try {
            System.in.read();
        } catch (Exception ignored) {}

        if (client != null) {
            client.close();
        }
        webServer.close();

        System.out.println("\n=========================================================");
        System.out.println("  TUCANTRACE - Finalizado exitosamente                   ");
        System.out.println("=========================================================");
    }
}