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

/**
 * Genera diagramas UML en formato PlantUML a partir de la información
 * extraída por JavaParserAdapter.
 * <p>
 * Usa {@link SourceStringReader} de PlantUML para generar PNG/SVG
 * programáticamente (confirmado en plantuml.com/api).
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
        sb.append("skinparam classFontSize 11\n\n");

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
            Files.createDirectories(path.getParent());
            Files.writeString(path, plantUML, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando " + filePath, e);
        }
    }

    /**
     * Genera PNG desde el texto PlantUML.
     */
    public byte[] generatePNG(String plantUML) {
        SourceStringReader reader = new SourceStringReader(plantUML);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String desc = reader.outputImage(os).getDescription();
        return os.toByteArray();
    }

    /**
     * Genera SVG desde el texto PlantUML.
     */
    public String generateSVG(String plantUML) {
        SourceStringReader reader = new SourceStringReader(plantUML);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String desc = reader.generateImage(os, new FileFormatOption(FileFormat.SVG));
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Guarda el diagrama como archivo PNG.
     */
    public void savePNG(String plantUML, String filePath) {
        try {
            byte[] png = generatePNG(plantUML);
            Path path = Path.of(filePath);
            Files.createDirectories(path.getParent());
            Files.write(path, png);
        } catch (IOException e) {
            throw new RuntimeException("Error guardando PNG " + filePath, e);
        }
    }
}