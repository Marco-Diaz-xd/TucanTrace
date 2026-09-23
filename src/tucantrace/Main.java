package tucantrace;

import tucantrace.parser.JavaParserAdapter;
import tucantrace.parser.PlantUMLGenerator;
import tucantrace.runtime.JDIClient;
import tucantrace.ui.DiagramViewer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Punto de entrada principal de TucanTrace.
 * <p>
 * Modo demo: parsea un directorio de código Java, genera el UML estático
 * y (opcional) se conecta vía JDI a una JVM en ejecución para mostrar
 * resaltado en vivo.
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
        String sourceDir = args.length > 0 ? args[0] : "../TucanGo/src";
        boolean enableJDI = args.length > 1 && "--jdi".equals(args[1]);

        System.out.println("Directorio de código fuente: " + sourceDir);
        System.out.println("JDI habilitado: " + enableJDI);
        System.out.println();

        // 2. Paso estático: parsear código → UML
        System.out.println("--- 1. Análisis estático (JavaParser → PlantUML) ---");
        try {
            Path srcPath = Paths.get(sourceDir).toAbsolutePath().normalize();
            if (!srcPath.toFile().exists()) {
                System.err.println("❌ Directorio no encontrado: " + srcPath);
                System.err.println("Uso: java tucantrace.Main <directorio-fuente> [--jdi]");
                return;
            }

            JavaParserAdapter parser = new JavaParserAdapter();
            var ast = parser.parseProject(srcPath);
            System.out.println("  Clases parseadas: " + ast.size());

            PlantUMLGenerator generator = new PlantUMLGenerator();
            String plantUML = generator.generate(ast);
            System.out.println("  PlantUML generado (" + plantUML.length() + " chars)");

            // Guardar .puml y .svg para inspección
            generator.saveToFile(plantUML, "build/tucantrace-diagram.puml");
            System.out.println("  Archivo .puml guardado en build/tucantrace-diagram.puml");

        } catch (Exception e) {
            System.err.println("❌ Error en análisis estático: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 3. (Opcional) JDI en vivo
        if (enableJDI) {
            System.out.println("\n--- 2. Conexión JDI en vivo ---");
            System.out.println("  Conectando a localhost:5005 ...");
            try (JDIClient client = new JDIClient("localhost", 5005)) {
                client.connect();
                System.out.println("  ✅ Conectado a JVM objetivo");
                System.out.println("  Escuchando eventos: MethodEntry, FieldModification, ClassPrepare...");

                // Aquí se conectaría al visor UI
                // DiagramViewer viewer = new DiagramViewer();
                // viewer.start(client.getEventBus());

                // Mantener vivo para demo
                System.out.println("  Presiona ENTER para salir...");
                System.in.read();

            } catch (Exception e) {
                System.err.println("❌ Error JDI: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n=========================================================");
        System.out.println("  TUCANTRACE - Demo finalizado                           ");
        System.out.println("=========================================================");
    }
}