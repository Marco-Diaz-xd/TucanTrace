package tucantrace.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Adaptador para JavaParser: parsea un proyecto Java completo y extrae
 * la información relevante para generar UML (clases, atributos, métodos,
 * herencia, interfaces, relaciones).
 */
public class JavaParserAdapter {

    private final JavaParser parser;

    public JavaParserAdapter() {
        this.parser = new JavaParser();
    }

    /**
     * Parsea recursivamente todos los archivos .java en un directorio.
     *
     * @param sourceRoot directorio raíz del código fuente
     * @return lista de CompilationUnit parseadas
     */
    public List<CompilationUnit> parseProject(Path sourceRoot) {
        List<CompilationUnit> units = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(p -> {
                     try {
                         CompilationUnit cu = parser.parse(p).getResult().orElseThrow();
                         units.add(cu);
                     } catch (Exception e) {
                         System.err.println("⚠️ Error parseando " + p + ": " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            throw new RuntimeException("Error recorriendo " + sourceRoot, e);
        }

        return units;
    }

    /**
     * Extrae información UML de una unidad de compilación.
     */
    public List<UMLClassInfo> extractClassInfo(List<CompilationUnit> units) {
        List<UMLClassInfo> classes = new ArrayList<>();

        for (CompilationUnit cu : units) {
            for (TypeDeclaration<?> type : cu.getTypes()) {
                if (type instanceof ClassOrInterfaceDeclaration cid) {
                    UMLClassInfo info = new UMLClassInfo();
                    info.name = cid.getNameAsString();
                    info.isInterface = cid.isInterface();
                    info.isAbstract = cid.isAbstract();
                    info.packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");

                    // Herencia
                    cid.getExtendedTypes().forEach(t -> info.extendsTypes.add(t.getNameAsString()));
                    // Implementación
                    cid.getImplementedTypes().forEach(t -> info.implementsTypes.add(t.getNameAsString()));

                    // Atributos
                    for (FieldDeclaration fd : cid.getFields()) {
                        fd.getVariables().forEach(v -> {
                            UMLAttributeInfo attr = new UMLAttributeInfo();
                            attr.name = v.getNameAsString();
                            attr.type = v.getType().toString();
                            attr.modifiers = fd.getModifiers().toString();
                            info.attributes.add(attr);
                        });
                    }

                    // Métodos
                    for (MethodDeclaration md : cid.getMethods()) {
                        UMLMethodInfo method = new UMLMethodInfo();
                        method.name = md.getNameAsString();
                        method.returnType = md.getType().toString();
                        method.modifiers = md.getModifiers().toString();
                        md.getParameters().forEach(p -> method.parameters.add(p.getType() + " " + p.getNameAsString()));
                        info.methods.add(method);
                    }

                    classes.add(info);
                }
            }
        }

        return classes;
    }

    // ----- DTOs internos -----

    public static class UMLClassInfo {
        public String name;
        public String packageName;
        public boolean isInterface;
        public boolean isAbstract;
        public List<String> extendsTypes = new ArrayList<>();
        public List<String> implementsTypes = new ArrayList<>();
        public List<UMLAttributeInfo> attributes = new ArrayList<>();
        public List<UMLMethodInfo> methods = new ArrayList<>();
    }

    public static class UMLAttributeInfo {
        public String name;
        public String type;
        public String modifiers;
    }

    public static class UMLMethodInfo {
        public String name;
        public String returnType;
        public String modifiers;
        public List<String> parameters = new ArrayList<>();
    }
}