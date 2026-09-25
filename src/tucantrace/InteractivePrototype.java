package tucantrace;

import tucantrace.parser.CourseStandardValidator;
import tucantrace.parser.JavaParserAdapter;
import tucantrace.parser.PlantUMLGenerator;
import tucantrace.runtime.JDIClient;
import tucantrace.ui.LiveSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Prototipo INTERACTIVO de TucanTrace.
 * <p>
 * Menú por consola que permite: escanear un proyecto Java, explorar su estructura,
 * validar los estándares del curso (Guías 1 y 2), generar el diagrama UML y
 * trazar la ejecución en vivo vía JDI.
 * </p>
 *
 * <p>Uso: {@code java tucantrace.InteractivePrototype}</p>
 *
 * @author Equipo TucanTrace
 * @version 0.2.0
 */
public class InteractivePrototype {

    private static final String DEFAULT_SOURCE = "case-study/tucango-model/src";
    private static final String SEP = "-----------------------------------------------------";

    private final Scanner scanner = new Scanner(System.in);
    private final JavaParserAdapter parser = new JavaParserAdapter();
    private final PlantUMLGenerator generator = new PlantUMLGenerator();
    private final CourseStandardValidator validator = new CourseStandardValidator();

    private List<JavaParserAdapter.UMLClassInfo> clases;

    public static void main(String[] args) {
        new InteractivePrototype().run();
    }

    public void run() {
        banner();
        boolean salir = false;
        while (!salir) {
            menu();
            String opcion = scanner.nextLine().trim();
            switch (opcion) {
                case "1" -> escanear();
                case "2" -> verDetalleClase();
                case "3" -> validarEstandares();
                case "4" -> generarDiagrama();
                case "5" -> trazarEnVivo();
                case "6" -> visorEnVivo();
                case "0", "q", "salir" -> salir = true;
                default -> System.out.println("\n[!] Opcion no valida.\n");
            }
        }
        System.out.println("\nSaliendo de TucanTrace. Hasta pronto!\n");
    }

    private void banner() {
        System.out.println("#####################################################");
        System.out.println("#                                                   #");
        System.out.println("#   TUCANTRACE  -  Prototipo Interactivo  (v0.2)    #");
        System.out.println("#   Scanner de codigo + UML + Trazado en vivo       #");
        System.out.println("#   Universidad de la Amazonia - Logica II          #");
        System.out.println("#                                                   #");
        System.out.println("#####################################################\n");
    }

    private void menu() {
        System.out.println(SEP);
        System.out.println("  MENU PRINCIPAL"
                + (clases != null ? "   [proyecto cargado: " + clases.size() + " clases]" : ""));
        System.out.println(SEP);
        System.out.println("  1. Escanear proyecto Java (codigo -> estructura)");
        System.out.println("  2. Ver detalle de una clase");
        System.out.println("  3. Validar estandares del curso (Guias 1 y 2)");
        System.out.println("  4. Generar diagrama UML (.puml + .svg)");
        System.out.println("  5. Trazar ejecucion en vivo (JDI, consola)");
        System.out.println("  6. VISOR EN VIVO en el navegador (recomendado)");
        System.out.println("  0. Salir");
        System.out.print("  Opcion > ");
    }

    // ------------------------------------------------------------------
    // 1. Escanear
    // ------------------------------------------------------------------
    private void escanear() {
        System.out.print("\nRuta del proyecto Java [" + DEFAULT_SOURCE + "]: ");
        String ruta = scanner.nextLine().trim();
        if (ruta.isEmpty()) {
            ruta = DEFAULT_SOURCE;
        }

        Path path = Paths.get(ruta).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            System.out.println("[X] No existe la ruta: " + path + "\n");
            return;
        }

        System.out.println("\n[...] Escaneando " + path + " ...");
        try {
            clases = parser.extractClassInfo(parser.parseProject(path));

            System.out.println("\n[OK] Escaneo completo: " + clases.size() + " clases encontradas\n");
            System.out.printf("  %-22s %-10s %-10s %s%n", "CLASE", "ATRIBUTOS", "METODOS", "HERENCIA");
            System.out.println("  " + "-".repeat(64));
            for (JavaParserAdapter.UMLClassInfo c : clases) {
                String herencia = c.extendsTypes.isEmpty() ? "-" : String.join(", ", c.extendsTypes);
                System.out.printf("  %-22s %-10d %-10d %s%n",
                        c.name, c.attributes.size(), c.methods.size(), herencia);
            }
            System.out.println();
        } catch (Exception e) {
            System.out.println("[X] Error al escanear: " + e.getMessage() + "\n");
        }
    }

    // ------------------------------------------------------------------
    // 2. Ver detalle
    // ------------------------------------------------------------------
    private void verDetalleClase() {
        if (!proyectoEsCargado()) return;

        System.out.print("\nNombre de la clase: ");
        String nombre = scanner.nextLine().trim();

        JavaParserAdapter.UMLClassInfo encontrada = clases.stream()
                .filter(c -> c.name.equalsIgnoreCase(nombre))
                .findFirst()
                .orElse(null);

        if (encontrada == null) {
            System.out.println("[X] No se encontro la clase '" + nombre + "'.\n");
            return;
        }

        System.out.println("\n+-------------------------------------------------");
        System.out.println("| CLASE: " + encontrada.name
                + (encontrada.isAbstract ? "  <<abstract>>" : "")
                + (encontrada.isInterface ? "  <<interface>>" : ""));
        System.out.println("| Paquete: " + encontrada.packageName);
        if (!encontrada.extendsTypes.isEmpty()) {
            System.out.println("| Extiende: " + String.join(", ", encontrada.extendsTypes));
        }
        if (!encontrada.implementsTypes.isEmpty()) {
            System.out.println("| Implementa: " + String.join(", ", encontrada.implementsTypes));
        }
        System.out.println("+-------------------------------------------------");
        System.out.println("| ATRIBUTOS:");
        if (encontrada.attributes.isEmpty()) {
            System.out.println("|   (ninguno)");
        }
        for (JavaParserAdapter.UMLAttributeInfo a : encontrada.attributes) {
            System.out.println("|   " + visibilidad(a.modifiers) + " " + a.name + " : " + a.type);
        }
        System.out.println("+-------------------------------------------------");
        System.out.println("| METODOS:");
        if (encontrada.methods.isEmpty()) {
            System.out.println("|   (ninguno)");
        }
        for (JavaParserAdapter.UMLMethodInfo m : encontrada.methods) {
            System.out.println("|   " + visibilidad(m.modifiers) + " " + m.name + "("
                    + String.join(", ", m.parameters) + ") : " + m.returnType);
        }
        System.out.println("+-------------------------------------------------\n");
    }

    // ------------------------------------------------------------------
    // 3. Validar estándares
    // ------------------------------------------------------------------
    private void validarEstandares() {
        if (!proyectoEsCargado()) return;
        System.out.println();
        List<CourseStandardValidator.Hallazgo> hallazgos = validator.validar(clases);
        System.out.println(validator.generarReporte(hallazgos));
    }

    // ------------------------------------------------------------------
    // 4. Generar diagrama
    // ------------------------------------------------------------------
    private void generarDiagrama() {
        if (!proyectoEsCargado()) return;

        try {
            String plantUML = generator.generate(clases);
            Path puml = Paths.get("build/tucantrace-diagram.puml");
            Path svg = Paths.get("build/tucantrace-diagram.svg");

            generator.saveToFile(plantUML, puml.toString());
            Files.createDirectories(svg.getParent());
            Files.writeString(svg, generator.generateSVG(plantUML));

            System.out.println("\n[OK] Diagrama generado:");
            System.out.println("     - PlantUML : " + puml.toAbsolutePath());
            System.out.println("     - SVG      : " + svg.toAbsolutePath());
            System.out.println("     Tip: abre el .svg en un navegador para verlo.\n");
        } catch (Exception e) {
            System.out.println("[X] Error generando el diagrama: " + e.getMessage() + "\n");
        }
    }

    // ------------------------------------------------------------------
    // 5. Trazar en vivo (JDI)
    // ------------------------------------------------------------------
    private void trazarEnVivo() {
        if (!proyectoEsCargado()) return;

        System.out.print("\nHost [localhost]: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Puerto [5005]: ");
        String puertoStr = scanner.nextLine().trim();
        int puerto = puertoStr.isEmpty() ? 5005 : Integer.parseInt(puertoStr);

        String filtro = paqueteComun();
        System.out.println("\n[...] Conectando a " + host + ":" + puerto + "  (filtro: " + filtro + ".*)");
        System.out.println("      La JVM objetivo debe correr con:");
        System.out.println("      -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:" + puerto);
        System.out.println("      (Ctrl+C para abortar)\n");

        try (JDIClient client = new JDIClient(host, puerto, filtro)) {
            client.connect();
            System.out.println("      [OK] Conectado. Eventos en vivo:\n");

            int count = 0;
            while (!client.isTerminated()) {
                JDIClient.JDIEvent ev = client.pollEvent(500);
                if (ev != null) {
                    System.out.println("      " + ev.describe());
                    count++;
                }
            }
            JDIClient.JDIEvent resto;
            while ((resto = client.pollEventNow()) != null) {
                System.out.println("      " + resto.describe());
                count++;
            }
            System.out.println("\n      [OK] Trazado finalizado. Total eventos: " + count + "\n");
        } catch (Exception e) {
            System.out.println("[X] Error JDI: " + e.getMessage() + "\n");
        }
    }

    // ------------------------------------------------------------------
    // 6. Visor en vivo (navegador)
    // ------------------------------------------------------------------
    private void visorEnVivo() {
        if (!proyectoEsCargado()) return;

        System.out.print("\nPuerto del visor web [8077]: ");
        String ps = scanner.nextLine().trim();
        int httpPort = ps.isEmpty() ? 8077 : Integer.parseInt(ps);

        System.out.print("Host JDI [localhost]: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost";

        System.out.print("Puerto JDI [5005]: ");
        String pj = scanner.nextLine().trim();
        int jdiPort = pj.isEmpty() ? 5005 : Integer.parseInt(pj);

        System.out.print("Retardo por evento en ms (camara lenta) [150]: ");
        String rd = scanner.nextLine().trim();
        long delay = rd.isEmpty() ? 150 : Long.parseLong(rd);

        try {
            String plantUML = generator.generate(clases);
            String svg = generator.generateSVG(plantUML);
            String filtro = paqueteComun();

            try (LiveSession session = new LiveSession(svg, httpPort, delay)) {
                System.out.println("\n[OK] Visor disponible en: " + session.getUrl());
                session.abrirNavegador();
                System.out.println("     Abriendo el navegador...");

                // Esperar a que el navegador se conecte al stream (hasta 10s)
                for (int i = 0; i < 40 && !session.hayNavegador(); i++) {
                    Thread.sleep(250);
                }
                if (!session.hayNavegador()) {
                    System.out.println("     [!] Ningun navegador conectado; el trazado continuara igual.");
                } else {
                    System.out.println("     [OK] Navegador conectado.");
                }

                System.out.println("     Conectando a JDI " + host + ":" + jdiPort
                        + " (filtro " + filtro + ".*) ...");

                try (JDIClient client = new JDIClient(host, jdiPort, filtro)) {
                    client.connect();
                    System.out.println("     [OK] Trazando en vivo. Mira la pantalla del navegador.\n");

                    int count = 0;
                    while (!client.isTerminated()) {
                        JDIClient.JDIEvent ev = client.pollEvent(500);
                        if (ev != null) {
                            session.publicar(ev);
                            count++;
                        }
                    }
                    JDIClient.JDIEvent resto;
                    while ((resto = client.pollEventNow()) != null) {
                        session.publicar(resto);
                        count++;
                    }
                    System.out.println("\n     [OK] Ejecucion finalizada. Eventos enviados: " + count);
                }

                System.out.println("     El visor sigue disponible en " + session.getUrl());
                System.out.print("     Presiona ENTER para cerrar el visor y volver al menu...");
                scanner.nextLine();
            }
        } catch (Exception e) {
            System.out.println("[X] Error en el visor en vivo: " + e.getMessage() + "\n");
        }
        System.out.println();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private boolean proyectoEsCargado() {
        if (clases == null) {
            System.out.println("\n[!] Primero escanea un proyecto (opcion 1).\n");
            return false;
        }
        return true;
    }

    private String visibilidad(String modifiers) {
        if (modifiers.contains("private")) return "-";
        if (modifiers.contains("protected")) return "#";
        if (modifiers.contains("public")) return "+";
        return "~";
    }

    private String paqueteComun() {
        String prefijo = null;
        for (JavaParserAdapter.UMLClassInfo c : clases) {
            if (c.packageName == null || c.packageName.isEmpty()) continue;
            if (prefijo == null) {
                prefijo = c.packageName;
            } else {
                String[] a = prefijo.split("\\.");
                String[] b = c.packageName.split("\\.");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(a.length, b.length); i++) {
                    if (!a[i].equals(b[i])) break;
                    if (sb.length() > 0) sb.append('.');
                    sb.append(a[i]);
                }
                prefijo = sb.toString();
            }
        }
        return prefijo == null ? "" : prefijo;
    }
}