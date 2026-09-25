package tucantrace.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida el código Java escaneado contra los estándares de codificación
 * del curso Lógica & Algoritmos II (Guías 1 y 2).
 *
 * <p>Reglas verificadas:</p>
 * <ul>
 *   <li>Clases: sustantivo en singular, UpperCamelCase.</li>
 *   <li>Atributos: {@code private}, lowerCamelCase, tipos coherentes.</li>
 *   <li>Métodos: {@code public}, verbo en infinitivo, lowerCamelCase.</li>
 *   <li>Encapsulamiento: atributos no públicos.</li>
 * </ul>
 */
public class CourseStandardValidator {

    private static final Pattern UPPER_CAMEL = Pattern.compile("^[A-Z][A-Za-z0-9]*$");
    private static final Pattern LOWER_CAMEL = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    private static final Pattern ACCESSOR = Pattern.compile("^(get|set|is|has)[A-Z].*$");

    private static final Set<String> TIPOS_VALIDOS = Set.of(
            "String", "int", "double", "boolean", "char", "float", "long",
            "Integer", "Double", "Boolean", "List", "ArrayList", "Map", "HashMap", "Set"
    );

    public enum Severidad { OK, ADVERTENCIA, ERROR }

    /** Un hallazgo de la validación. */
    public static class Hallazgo {
        public final Severidad severidad;
        public final String elemento;
        public final String mensaje;

        public Hallazgo(Severidad severidad, String elemento, String mensaje) {
            this.severidad = severidad;
            this.elemento = elemento;
            this.mensaje = mensaje;
        }

        @Override
        public String toString() {
            String icono = switch (severidad) {
                case OK -> "[OK]";
                case ADVERTENCIA -> "[!] ";
                case ERROR -> "[X] ";
            };
            return icono + " " + elemento + " -> " + mensaje;
        }
    }

    /**
     * Valida todas las clases y devuelve los hallazgos.
     */
    public List<Hallazgo> validar(List<JavaParserAdapter.UMLClassInfo> clases) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (JavaParserAdapter.UMLClassInfo cls : clases) {
            validarClase(cls, hallazgos);
        }
        return hallazgos;
    }

    private void validarClase(JavaParserAdapter.UMLClassInfo cls, List<Hallazgo> out) {
        String prefijo = cls.name;

        // --- Nombre de clase: UpperCamelCase ---
        if (!UPPER_CAMEL.matcher(cls.name).matches()) {
            out.add(new Hallazgo(Severidad.ERROR, prefijo,
                    "El nombre de clase debe ser UpperCamelCase (sustantivo singular)."));
        } else {
            out.add(new Hallazgo(Severidad.OK, prefijo, "Nombre de clase conforme (UpperCamelCase)."));
        }

        // --- Atributos ---
        for (JavaParserAdapter.UMLAttributeInfo attr : cls.attributes) {
            String id = prefijo + "." + attr.name;

            if (!attr.modifiers.contains("private") && !attr.modifiers.contains("protected")) {
                out.add(new Hallazgo(Severidad.ERROR, id,
                        "Atributo sin encapsulamiento: debe ser 'private' (o 'protected' en superclase)."));
            }

            if (!LOWER_CAMEL.matcher(attr.name).matches()) {
                out.add(new Hallazgo(Severidad.ERROR, id,
                        "El nombre del atributo debe ser lowerCamelCase."));
            }

            String tipoBase = attr.type.replaceAll("<.*>", "").trim();
            if (!TIPOS_VALIDOS.contains(tipoBase) && !tipoBase.isEmpty()) {
                out.add(new Hallazgo(Severidad.ADVERTENCIA, id,
                        "Tipo '" + attr.type + "' fuera de los sugeridos (String, int, double, boolean). "
                        + "Verifica que sea coherente con el dato."));
            }
        }

        // --- Métodos ---
        for (JavaParserAdapter.UMLMethodInfo m : cls.methods) {
            String id = prefijo + "." + m.name + "()";
            boolean esConstructor = m.name.equals(cls.name);

            if (esConstructor) {
                continue;
            }

            if (!m.modifiers.contains("public")) {
                out.add(new Hallazgo(Severidad.ADVERTENCIA, id,
                        "Método no público: la guía pide 'public' para las responsabilidades."));
            }

            if (!LOWER_CAMEL.matcher(m.name).matches()) {
                out.add(new Hallazgo(Severidad.ERROR, id,
                        "El nombre del método debe ser lowerCamelCase."));
            }

            if (ACCESSOR.matcher(m.name).matches()) {
                out.add(new Hallazgo(Severidad.ADVERTENCIA, id,
                        "Es un accesor (get/set/is). No cuenta como 'responsabilidad de negocio' "
                        + "según la Guía 2 (verbo en infinitivo)."));
            }
        }
    }

    /**
     * Genera un reporte formateado en texto.
     * <p>Los accesores (get/set/is) se agrupan por clase para reducir el ruido.</p>
     */
    public String generarReporte(List<Hallazgo> hallazgos) {
        long errores = hallazgos.stream().filter(h -> h.severidad == Severidad.ERROR).count();
        long advertencias = hallazgos.stream().filter(h -> h.severidad == Severidad.ADVERTENCIA).count();
        long oks = hallazgos.stream().filter(h -> h.severidad == Severidad.OK).count();

        StringBuilder sb = new StringBuilder();
        sb.append("=====================================================\n");
        sb.append("  VALIDACION DE ESTANDARES DEL CURSO (Guias 1 y 2)   \n");
        sb.append("=====================================================\n\n");

        // 1. Errores (individuales)
        List<Hallazgo> erroresList = hallazgos.stream()
                .filter(h -> h.severidad == Severidad.ERROR)
                .toList();
        if (!erroresList.isEmpty()) {
            sb.append("ERRORES (").append(erroresList.size()).append("):\n");
            for (Hallazgo h : erroresList) {
                sb.append("  ").append(h).append("\n");
            }
            sb.append("\n");
        }

        // 2. Advertencias NO-accesor (individuales)
        List<Hallazgo> otrasWarn = hallazgos.stream()
                .filter(h -> h.severidad == Severidad.ADVERTENCIA && !esAccesor(h))
                .toList();
        if (!otrasWarn.isEmpty()) {
            sb.append("ADVERTENCIAS (").append(otrasWarn.size()).append("):\n");
            for (Hallazgo h : otrasWarn) {
                sb.append("  ").append(h).append("\n");
            }
            sb.append("\n");
        }

        // 3. Accesores agrupados por clase
        java.util.Map<String, Integer> accesoresPorClase = new java.util.LinkedHashMap<>();
        for (Hallazgo h : hallazgos) {
            if (h.severidad == Severidad.ADVERTENCIA && esAccesor(h)) {
                String clase = h.elemento.contains(".") ? h.elemento.substring(0, h.elemento.indexOf('.')) : h.elemento;
                accesoresPorClase.merge(clase, 1, Integer::sum);
            }
        }
        if (!accesoresPorClase.isEmpty()) {
            int total = accesoresPorClase.values().stream().mapToInt(Integer::intValue).sum();
            sb.append("ACCESORES get/set/is (").append(total)
              .append(") — no cuentan como responsabilidades de negocio (Guia 2):\n");
            for (var e : accesoresPorClase.entrySet()) {
                sb.append("  - ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("-----------------------------------------------------\n");
        sb.append(String.format("Resumen: %d conformes | %d advertencias | %d errores%n",
                oks, advertencias, errores));
        if (errores == 0 && advertencias == 0) {
            sb.append("[OK] El codigo cumple los estandares del curso.\n");
        } else if (errores == 0) {
            sb.append("[!] Sin errores. Las advertencias son de estilo, no bloquean.\n");
        } else {
            sb.append("[X] Corrige los errores para cumplir la Guia 1.\n");
        }
        return sb.toString();
    }

    private boolean esAccesor(Hallazgo h) {
        return h.mensaje != null && h.mensaje.contains("accesor");
    }
}