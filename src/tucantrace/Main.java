package tucantrace;

import tucantrace.parser.JavaParserAdapter;
import tucantrace.parser.PlantUMLGenerator;
import tucantrace.runtime.JDIClient;
import tucantrace.ui.LiveController;
import tucantrace.ui.LiveSession;
import tucantrace.ui.TraceWebServer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Punto de entrada principal de TucanTrace.
 * <p>
 * Modos de ejecución disponibles:
 * <ul>
 *   <li><b>Análisis estático (por defecto):</b> parsea código Java, genera PlantUML y renderiza SVG interactivo.</li>
 *   <li><b>Servidor Web SSE (<code>--web</code> o <code>--port</code>):</b> levanta {@link TraceWebServer} para visualización reactiva moderna.</li>
 *   <li><b>Trazado JDI en consola (<code>--jdi</code>):</b> se conecta a la JVM vía JDI y reporta eventos en la terminal.</li>
 *   <li><b>Visor interactivo en vivo de dos pestañas (<code>--live</code>):</b> lanza el programa objetivo con {@link LiveController} y muestra UML + terminal en vivo.</li>
 * </ul>
 * </p>
 *
 * @author Equipo TucanTrace
 * @version 0.2.0
 */
public class Main {

    private static final String DEFAULT_SOURCE = "case-study/tucango-model/src";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5005;

    /** Modo silencioso: menos salida técnica (para el modo interactivo). */
    private static boolean quiet = false;

    /** Imprime solo si no estamos en modo silencioso. */
    private static void log(String s) {
        if (!quiet) {
            System.out.println(s);
        }
    }

    public static void main(String[] args) {
        // 1. Argumentos y opciones CLI
        String sourceDir = DEFAULT_SOURCE;
        boolean enableJDI = false;
        boolean live = false;
        boolean startWebServer = false;
        int webPort = 8080;
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        int httpPort = 8077;
        long delay = 150;
        String execMain = null;
        String execCp = null;
        long keepAlive = -1; // <= 0 = mantener vivo indefinidamente

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--jdi".equalsIgnoreCase(arg)) {
                enableJDI = true;
            } else if ("--live".equalsIgnoreCase(arg)) {
                live = true;
                enableJDI = true;
            } else if ("--web".equalsIgnoreCase(arg)) {
                startWebServer = true;
            } else if (arg.startsWith("--port=")) {
                try {
                    webPort = Integer.parseInt(arg.substring(7));
                    startWebServer = true;
                } catch (NumberFormatException ignored) {}
            } else if ("--host".equalsIgnoreCase(arg) && i + 1 < args.length) {
                host = args[++i];
            } else if ("--port".equalsIgnoreCase(arg) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--http-port".equalsIgnoreCase(arg) && i + 1 < args.length) {
                httpPort = Integer.parseInt(args[++i]);
            } else if ("--delay".equalsIgnoreCase(arg) && i + 1 < args.length) {
                delay = Long.parseLong(args[++i]);
            } else if ("--exec".equalsIgnoreCase(arg) && i + 1 < args.length) {
                execMain = args[++i];
            } else if ("--exec-cp".equalsIgnoreCase(arg) && i + 1 < args.length) {
                execCp = args[++i];
            } else if ("--keep-alive".equalsIgnoreCase(arg) && i + 1 < args.length) {
                keepAlive = Long.parseLong(args[++i]);
            } else if ("--quiet".equalsIgnoreCase(arg)) {
                quiet = true;
            } else if (!arg.startsWith("--")) {
                sourceDir = arg;
            }
        }

        if (!quiet) {
            System.out.println("=========================================================");
            System.out.println("  TUCANTRACE v0.2 - Live UML Visualization for Java     ");
            System.out.println("  Universidad de la Amazonia - Ingeniería de Sistemas   ");
            System.out.println("=========================================================\n");
            System.out.println("Directorio de código fuente: " + sourceDir);
            System.out.println("Modo JDI en vivo: " + (enableJDI ? "SÍ (" + host + ":" + port + ")" : "NO"));
            System.out.println("Visor navegador live: " + (live ? "SÍ (puerto " + httpPort + ")" : "NO"));
            if (execMain != null) {
                System.out.println("Programa a ejecutar: " + execMain + "  [cp: " + execCp + "]");
            }
            System.out.println();
        }

        // 2. Análisis estático: código → UML (JavaParser → PlantUML)
        log("--- 1. Análisis estático (JavaParser → PlantUML) ---");
        String plantUML;
        String interactiveSvg = "";
        String rootPackage;
        List<JavaParserAdapter.UMLClassInfo> classes;

        try {
            Path srcPath = Paths.get(sourceDir).toAbsolutePath().normalize();
            if (!Files.exists(srcPath)) {
                System.err.println("[X] Directorio no encontrado: " + srcPath);
                System.err.println("Uso: java tucantrace.Main [directorio-fuente] [--jdi] [--live] [--web]");
                return;
            }

            JavaParserAdapter parser = new JavaParserAdapter();
            classes = parser.extractClassInfo(parser.parseProject(srcPath));
            log("  Clases parseadas: " + classes.size());
            if (!quiet) {
                for (JavaParserAdapter.UMLClassInfo c : classes) {
                    System.out.println("    · " + c.name
                            + "  [" + c.attributes.size() + " atributos, "
                            + c.methods.size() + " métodos]"
                            + (c.extendsTypes.isEmpty() ? "" : "  extiende " + c.extendsTypes));
                }
            }

            PlantUMLGenerator generator = new PlantUMLGenerator();
            plantUML = generator.generate(classes);
            log("  PlantUML generado: " + plantUML.length() + " caracteres");

            Path pumlOut = Paths.get("build/tucantrace-diagram.puml");
            generator.saveToFile(plantUML, pumlOut.toString());
            log("  [OK] Diagrama guardado: " + pumlOut.toAbsolutePath());

            // Renderizar SVG interactivo enriquecido
            try {
                interactiveSvg = generator.generateInteractiveSVG(plantUML, classes);
                Path svgOut = Paths.get("build/tucantrace-diagram.svg");
                if (svgOut.getParent() != null) {
                    Files.createDirectories(svgOut.getParent());
                }
                Files.writeString(svgOut, interactiveSvg, StandardCharsets.UTF_8);
                log("  [OK] SVG interactivo renderizado: " + svgOut.toAbsolutePath());
            } catch (Throwable t) {
                log("  [!] No se pudo renderizar SVG interactivo: " + t.getMessage());
            }

            rootPackage = commonPackagePrefix(classes);

        } catch (Exception e) {
            System.err.println("[X] Error en análisis estático: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 3. Caso visor Live interactivo de 2 pestañas (--live)
        if (live) {
            runLiveViewer(plantUML, rootPackage, host, port, httpPort, delay, execMain, execCp, keepAlive);
            return;
        }

        // 4. Caso Servidor Web Embebido SSE (--web o --port=...)
        if (startWebServer) {
            runTraceWebServer(interactiveSvg, enableJDI, host, port, webPort, rootPackage);
            return;
        }

        // 5. Caso Trazado JDI en consola (--jdi)
        if (enableJDI) {
            runConsoleJDI(host, port, rootPackage);
            return;
        }

        // Modo estático terminado
        System.out.println("\nTip: usa '--jdi' para trazar en consola, '--web' para servidor SSE, o '--live' para el visor interactivo de 2 pestañas.");
        System.out.println("\n=========================================================");
        System.out.println("  TUCANTRACE - Análisis estático finalizado             ");
        System.out.println("=========================================================");
    }

    /**
     * Inicia el servidor web reactivo SSE (TraceWebServer).
     */
    private static void runTraceWebServer(String interactiveSvg, boolean enableJDI,
                                          String host, int port, int webPort, String rootPackage) {
        System.out.println("\n--- 2. Servidor Web Embebido (Visor SVG Live) ---");
        TraceWebServer webServer = new TraceWebServer(webPort);
        webServer.updateSvg(interactiveSvg);

        try {
            webServer.start();
            System.out.println("  ✅ Servidor Web iniciado correctamente en http://localhost:" + webPort + "/");
        } catch (Exception e) {
            System.err.println("❌ Error iniciando servidor web: " + e.getMessage());
            return;
        }

        JDIClient client = null;
        if (enableJDI) {
            System.out.println("\n--- 3. Conexión JDI en vivo ---");
            System.out.println("  Conectando a " + host + ":" + port + " ...");
            try {
                String filter = rootPackage.isEmpty() ? "*" : rootPackage + ".*";
                client = new JDIClient(host, port, filter);
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
                System.err.println("⚠️ No se pudo conectar a JDI en " + host + ":" + port + ": " + e.getMessage());
                System.out.println("  (El servidor web continuará funcionando en modo visor/simulación)");
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
        System.out.println("  TUCANTRACE - Servidor web finalizado                   ");
        System.out.println("=========================================================");
    }

    /**
     * Trazado en vivo por consola.
     */
    private static void runConsoleJDI(String host, int port, String rootPackage) {
        System.out.println("\n--- 2. Conexión JDI en vivo (filtro: " + rootPackage + ".*) ---");
        try (JDIClient client = new JDIClient(host, port, rootPackage)) {
            client.connect();
            System.out.println("  Escuchando eventos... (Ctrl+C para salir)\n");

            int count = 0;
            while (!client.isTerminated()) {
                JDIClient.JDIEvent ev = client.pollEvent(500);
                if (ev != null) {
                    System.out.println("  " + ev.describe());
                    count++;
                }
            }
            JDIClient.JDIEvent resto;
            while ((resto = client.pollEventNow()) != null) {
                System.out.println("  " + resto.describe());
                count++;
            }
            System.out.println("\n  Total eventos capturados: " + count);

        } catch (Exception e) {
            System.err.println("[X] Error JDI: " + e.getMessage());
            System.err.println("   Verifica que la JVM objetivo esté corriendo con:");
            System.err.println("   -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:" + port);
        }

        System.out.println("\n=========================================================");
        System.out.println("  TUCANTRACE - Demo finalizado                           ");
        System.out.println("=========================================================");
    }

    /**
     * Visor en vivo: sirve el diagrama, lanza el programa objetivo y transmite
     * los eventos JDI y la salida del programa en dos pestañas del navegador.
     */
    private static void runLiveViewer(String plantUML, String rootPackage,
                                      String host, int jdiPort, int httpPort, long delay,
                                      String execMain, String execCp, long keepAlive) {
        if (execMain == null) {
            System.err.println("[X] El modo --live requiere --exec <clase> [--exec-cp <cp>].");
            return;
        }
        try {
            PlantUMLGenerator generator = new PlantUMLGenerator();
            String svg = generator.generateSVG(plantUML);

            LiveSession session = new LiveSession(svg, httpPort, delay);
            try {
                LiveController controller = new LiveController(
                        session, host, jdiPort, rootPackage, execMain, execCp);
                session.setRunHandler(controller::ejecutarAsync);

                log("\n--- 2. Visor en vivo (navegador) ---");
                log("  Programa  : " + execMain + "  [cp: " + execCp + "]");
                log("  Visor UML : " + session.getUrl());
                log("  Terminal  : " + session.getUrlTerminal());
                session.abrirDosPestanas();

                // Esperar al navegador (hasta 12s)
                for (int i = 0; i < 48 && !session.hayNavegador(); i++) {
                    Thread.sleep(250);
                }
                log(session.hayNavegador() ? "  [OK] Navegador conectado." : "  [!] Sin navegador.");

                // Guía para el usuario en consola
                guiaConsola();

                // Primera ejecución
                controller.ejecutarAsync();

                // Mantener el servidor vivo para re-ejecutar desde el navegador
                if (keepAlive <= 0) {
                    while (true) {
                        Thread.sleep(1000);
                    }
                } else {
                    Thread.sleep(keepAlive * 1000);
                }
            } finally {
                session.close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[X] Error en el visor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Imprime una guía clara para que el usuario sepa qué escribir en la consola.
     */
    private static void guiaConsola() {
        System.out.println();
        System.out.println("########################################################################");
        System.out.println("#                                                                      #");
        System.out.println("#                   >>>   ESCRIBÍ EN ESTA VENTANA   <<<                #");
        System.out.println("#                                                                      #");
        System.out.println("#   El programa ya arrancó y espera que le escribas ABAJO.             #");
        System.out.println("#   Escribí el número y presioná ENTER.                                 #");
        System.out.println("#                                                                      #");
        System.out.println("#     >>>  EMPEZÁ ESCRIBIENDO:   1   y ENTER                          #");
        System.out.println("#          (registra al estudiante)                                    #");
        System.out.println("#          Luego responde: nombre, cédula, teléfono, correo, código    #");
        System.out.println("#                                                                      #");
        System.out.println("#   Menú rápido (escribí el número + ENTER):                           #");
        System.out.println("#     1 = registrar estudiante    2 = registrar motorista + moto       #");
        System.out.println("#     3 = pedir viaje             4 = aceptar viaje                    #");
        System.out.println("#     5 = iniciar viaje           6 = finalizar viaje                  #");
        System.out.println("#     7 = reportar pago           8 = confirmar pago                   #");
        System.out.println("#     9 = calificar              10 = ver estado                       #");
        System.out.println("#     0 = salir                                                        #");
        System.out.println("#                                                                      #");
        System.out.println("#   Mientras escribís, mirá el NAVEGADOR: el UML se ilumina en vivo.   #");
        System.out.println("#                                                                      #");
        System.out.println("########################################################################");
        System.out.println();
    }

    /**
     * Calcula el prefijo de paquete común a todas las clases parseadas.
     */
    private static String commonPackagePrefix(List<JavaParserAdapter.UMLClassInfo> classes) {
        String prefix = null;
        for (JavaParserAdapter.UMLClassInfo c : classes) {
            if (c.packageName == null || c.packageName.isEmpty()) {
                continue;
            }
            if (prefix == null) {
                prefix = c.packageName;
            } else {
                prefix = commonPrefix(prefix, c.packageName);
            }
        }
        return prefix == null ? "" : prefix;
    }

    private static String commonPrefix(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        StringBuilder sb = new StringBuilder();
        int n = Math.min(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            if (!pa[i].equals(pb[i])) {
                break;
            }
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(pa[i]);
        }
        return sb.toString();
    }
}
