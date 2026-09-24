package tucantrace.parser;

import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.FileFormat;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Genera diagramas UML en formato PlantUML a partir de la información
 * extraída por JavaParserAdapter.
 * <p>
 * Usa {@link SourceStringReader} de PlantUML para generar PNG/SVG
 * programáticamente y enriquece el SVG con IDs semánticos y clases CSS
 * para soportar interacción y resaltado dinámico en vivo.
 * </p>
 */
public class PlantUMLGenerator {

    /**
     * Genera el texto PlantUML completo a partir de la lista de clases.
     */
    public String generate(List<JavaParserAdapter.UMLClassInfo> classes) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("' Generado automáticamente por TucanTrace\n");
        sb.append("skinparam classAttributeIconSize 0\n");
        sb.append("skinparam classAttributeFontSize 10\n");
        sb.append("skinparam classMethodFontSize 10\n");
        sb.append("skinparam classFontSize 11\n");
        sb.append("skinparam svgDimensionStyle false\n\n");

        // Definir clases
        for (JavaParserAdapter.UMLClassInfo cls : classes) {
            sb.append(generateClass(cls));
        }

        // Relaciones
        for (JavaParserAdapter.UMLClassInfo cls : classes) {
            for (String ext : cls.extendsTypes) {
                sb.append(ext).append(" <|-- ").append(cls.name).append("\n");
            }
            for (String impl : cls.implementsTypes) {
                sb.append(impl).append(" <|.. ").append(cls.name).append("\n");
            }
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    private String generateClass(JavaParserAdapter.UMLClassInfo cls) {
        StringBuilder sb = new StringBuilder();
        String stereotype = cls.isInterface ? "interface" : (cls.isAbstract ? "abstract" : "class");
        sb.append(stereotype).append(" ").append(cls.name).append(" {\n");

        // Atributos
        for (JavaParserAdapter.UMLAttributeInfo attr : cls.attributes) {
            String visibility = visibilitySymbol(attr.modifiers);
            sb.append("  ").append(visibility).append(" ")
              .append(attr.name).append(" : ").append(attr.type).append("\n");
        }

        // Métodos
        for (JavaParserAdapter.UMLMethodInfo method : cls.methods) {
            String visibility = visibilitySymbol(method.modifiers);
            String params = String.join(", ", method.parameters);
            sb.append("  ").append(visibility).append(" ")
              .append(method.name).append("(").append(params).append(")")
              .append(" : ").append(method.returnType).append("\n");
        }

        sb.append("}\n\n");
        return sb.toString();
    }

    private String visibilitySymbol(String modifiers) {
        if (modifiers.contains("private")) return "-";
        if (modifiers.contains("protected")) return "#";
        if (modifiers.contains("public")) return "+";
        return "~"; // package-private
    }

    /**
     * Guarda el texto PlantUML en un archivo .puml.
     */
    public void saveToFile(String plantUML, String filePath) {
        try {
            Path path = Path.of(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, plantUML, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando " + filePath, e);
        }
    }

    /**
     * Genera PNG desde el texto PlantUML.
     */
    public byte[] generatePNG(String plantUML) {
        try {
            SourceStringReader reader = new SourceStringReader(plantUML);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            String desc = reader.outputImage(os).getDescription();
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando PNG", e);
        }
    }

    /**
     * Genera SVG desde el texto PlantUML.
     */
    public String generateSVG(String plantUML) {
        try {
            SourceStringReader reader = new SourceStringReader(plantUML);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            String desc = reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
            return new String(os.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error generando SVG", e);
        }
    }

    /**
     * Genera SVG enriquecido con soporte de IDs y clases CSS para clases, métodos y campos.
     *
     * @param plantUML código PlantUML
     * @param classes lista de metadatos de clases
     * @return SVG con IDs semánticos (id="class_ClassName", id="method_ClassName_methodName", etc.) y estilos
     */
    public String generateInteractiveSVG(String plantUML, List<JavaParserAdapter.UMLClassInfo> classes) {
        String svg = generateSVG(plantUML);
        return enhanceSVG(svg, classes);
    }

    /**
     * Enriquece un SVG de PlantUML insertando IDs y clases CSS en los elementos vectoriales.
     *
     * @param rawSvg SVG crudo generado por PlantUML
     * @param classes información de clases
     * @return SVG enriquecido con IDs y estilos
     */
    public String enhanceSVG(String rawSvg, List<JavaParserAdapter.UMLClassInfo> classes) {
        if (classes == null || classes.isEmpty() || rawSvg == null) {
            return rawSvg;
        }

        String result = rawSvg;

        for (JavaParserAdapter.UMLClassInfo cls : classes) {
            String className = cls.name;
            String startMarker = "<g id=\"elem_" + className + "\">";
            int startIdx = result.indexOf(startMarker);
            if (startIdx == -1) {
                continue;
            }

            // Encontrar el correspondiente </g> contando etiquetas anidadas <g y </g>
            int contentStart = startIdx + startMarker.length();
            int depth = 1;
            int curr = contentStart;
            int endIdx = -1;

            while (curr < result.length()) {
                int nextOpen = result.indexOf("<g", curr);
                int nextClose = result.indexOf("</g>", curr);

                if (nextClose == -1) break;

                if (nextOpen != -1 && nextOpen < nextClose) {
                    char after = nextOpen + 2 < result.length() ? result.charAt(nextOpen + 2) : ' ';
                    if (after == ' ' || after == '>') {
                        depth++;
                    }
                    curr = nextOpen + 2;
                } else {
                    depth--;
                    if (depth == 0) {
                        endIdx = nextClose;
                        break;
                    }
                    curr = nextClose + 4;
                }
            }

            if (endIdx == -1) {
                continue;
            }

            String groupBody = result.substring(contentStart, endIdx);

            // Reemplazar rect id="ClassName" -> id="class_ClassName" class="uml-class-box" data-class="ClassName"
            String newGroupHeader = "<g id=\"elem_" + className + "\" class=\"uml-class-group\" data-class=\"" + className + "\">";

            String newGroupBody = groupBody.replaceAll(
                "<rect ([^>]*)id=\"" + Pattern.quote(className) + "\"",
                "<rect $1id=\"class_" + className + "\" class=\"uml-class-box\" data-class=\"" + className + "\""
            );

            // Enriquecer métodos
            for (JavaParserAdapter.UMLMethodInfo m : cls.methods) {
                Pattern methodPattern = Pattern.compile("<text ([^>]*)>([+~#\\-]\\s*" + Pattern.quote(m.name) + "\\b[^<]*)</text>");
                Matcher methodMatcher = methodPattern.matcher(newGroupBody);
                if (methodMatcher.find()) {
                    String matchedText = methodMatcher.group(0);
                    String attrs = methodMatcher.group(1);
                    String content = methodMatcher.group(2);
                    String methodId = "method_" + className + "_" + m.name;
                    String replacement = "<text id=\"" + methodId + "\" class=\"uml-method\" data-class=\"" + className + "\" data-method=\"" + m.name + "\" " + attrs + ">" + content + "</text>";
                    newGroupBody = newGroupBody.replace(matchedText, replacement);
                }
            }

            // Enriquecer atributos
            for (JavaParserAdapter.UMLAttributeInfo attr : cls.attributes) {
                Pattern fieldPattern = Pattern.compile("<text ([^>]*)>([+~#\\-]\\s*" + Pattern.quote(attr.name) + "\\s*:[^<]*)</text>");
                Matcher fieldMatcher = fieldPattern.matcher(newGroupBody);
                if (fieldMatcher.find()) {
                    String matchedText = fieldMatcher.group(0);
                    String attrs = fieldMatcher.group(1);
                    String content = fieldMatcher.group(2);
                    String fieldId = "field_" + className + "_" + attr.name;
                    String replacement = "<text id=\"" + fieldId + "\" class=\"uml-field\" data-class=\"" + className + "\" data-field=\"" + attr.name + "\" " + attrs + ">" + content + "</text>";
                    newGroupBody = newGroupBody.replace(matchedText, replacement);
                }
            }

            result = result.substring(0, startIdx) + newGroupHeader + newGroupBody + "</g>" + result.substring(endIdx + 4);
        }

        // Inyectar definiciones de estilos CSS para resaltado interactivo
        String customStyle = """
        <style>
          .uml-class-group { cursor: pointer; }
          .uml-class-box { transition: fill 0.3s ease, stroke 0.3s ease, stroke-width 0.3s ease, filter 0.3s ease; }
          .uml-method { font-family: monospace; transition: fill 0.2s ease, font-size 0.2s ease, font-weight 0.2s ease; cursor: pointer; }
          .uml-field { font-family: monospace; transition: fill 0.2s ease, font-size 0.2s ease, font-weight 0.2s ease; cursor: pointer; }

          /* Resaltados de clases */
          .highlight-class {
            fill: #fef08a !important;
            stroke: #e11d48 !important;
            stroke-width: 2.5px !important;
            filter: drop-shadow(0 0 10px rgba(225, 29, 72, 0.65));
          }
          .highlight-class-prepare {
            fill: #bae6fd !important;
            stroke: #0284c7 !important;
            stroke-width: 2.5px !important;
            filter: drop-shadow(0 0 8px rgba(2, 132, 199, 0.6));
          }

          /* Resaltados de métodos */
          .highlight-method {
            fill: #e11d48 !important;
            font-weight: bold !important;
            font-size: 11.5px !important;
          }
          .highlight-method-box {
            fill: #fee2e2 !important;
            stroke: #ef4444 !important;
            stroke-width: 2px !important;
          }

          /* Resaltados de atributos */
          .highlight-field {
            fill: #7c3aed !important;
            font-weight: bold !important;
            font-size: 11.5px !important;
          }

          /* Animación de pulso */
          .pulse-active {
            animation: pulse-glow 1.2s infinite alternate ease-in-out;
          }
          @keyframes pulse-glow {
            0% { filter: drop-shadow(0 0 4px #e11d48); }
            100% { filter: drop-shadow(0 0 14px #fb7185); }
          }
        </style>
        """;

        if (result.contains("<defs/>")) {
            result = result.replace("<defs/>", "<defs>" + customStyle + "</defs>");
        } else if (result.contains("<defs>")) {
            result = result.replace("<defs>", "<defs>" + customStyle);
        }

        return result;
    }

    /**
     * Guarda el diagrama como archivo PNG.
     */
    public void savePNG(String plantUML, String filePath) {
        try {
            byte[] png = generatePNG(plantUML);
            Path path = Path.of(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, png);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando PNG " + filePath, e);
        }
    }

    /**
     * Guarda el diagrama como archivo SVG interactivo.
     */
    public void saveSVG(String plantUML, List<JavaParserAdapter.UMLClassInfo> classes, String filePath) {
        String svg = generateInteractiveSVG(plantUML, classes);
        saveSVG(svg, filePath);
    }

    /**
     * Guarda directamente un contenido SVG ya generado en un archivo.
     */
    public void saveSVG(String svgContent, String filePath) {
        try {
            Path path = Path.of(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, svgContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando SVG " + filePath, e);
        }
    }
}