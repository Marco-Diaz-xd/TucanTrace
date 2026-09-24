# 🦜 TucanTrace

**Live UML Visualization and Dynamic Execution Tracer for Java.**

Transforma el código fuente Java de estudiantes en diagramas UML interactivos y **resalta clases, métodos y atributos en tiempo real con latencia cero** mientras la aplicación se ejecuta en la JVM.

---

## 🎯 Objetivo Pedagógico

Herramienta de apoyo docente e investigación para los cursos de **Lógica & Algoritmos II** y **Programación Orientada a Objetos** en la Universidad de la Amazonia. Sus principales capacidades son:

1. **Análisis Estático Automático:** Genera diagramas UML de clases a partir del código fuente Java (`.java`) sin requerir diagramación manual previa.
2. **Visualización Vectorial Reactiva (SVG):** Emite diagramas SVG enriquecidos con IDs semánticos (`id="class_X"`, `id="method_X_Y"`, `id="field_X_Z"`) y estilos CSS acelerados por hardware.
3. **Trazabilidad de Ejecución en Vivo (JDI/JPDA):** Se conecta mediante sockets a la JVM del estudiante para interceptar invocaciones de métodos, modificaciones de atributos y carga de clases, iluminando el diagrama en vivo sin alterar el código fuente del estudiante.
4. **Visor Web con Streaming SSE:** Servidor web HTTP embebido (`http://localhost:8080/`) que transmite eventos en tiempo real mediante *Server-Sent Events* (SSE).

---

## 🏗️ Arquitectura en Tres Capas

```
┌──────────────────────────────────────────────────────────────────┐
│                   CÓDIGO FUENTE DEL ESTUDIANTE                   │
└────────────────────────────────┬─────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│             CAPA 1: ANÁLISIS ESTÁTICO (JavaParser)               │
│  • Recorrido de archivos .java y construcción del AST           │
│  • Extracción de Clases, Interfaces, Atributos, Métodos, Herencia│
└────────────────────────────────┬─────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│        CAPA 2: GENERADOR VECTORIAL REACTIVO (PlantUML / SVG)     │
│  • Generación de PlantUML y renderizado a SVG vectorial          │
│  • Inyección de IDs semánticos y reglas CSS (drop-shadow, pulse) │
└────────────────────────────────┬─────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│      CAPA 3: RUNTIME & STREAMING (JDI Client & Web Server SSE)   │
│  • JVM Estudiante: -agentlib:jdwp=transport=dt_socket,port=5005  │
│  • JDI Client: Captura MethodEntry, MethodExit, FieldModification│
│  • Servidor HTTP Embebido: Streaming SSE hacia el Visor Web      │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología Seleccionada | Justificación Técnica |
|------------|-------------------------|------------------------|
| **Lenguaje Base** | Java 21 LTS | Soporte de switch pattern matching, record types y APIs modernas. |
| **Sistema de Build** | Apache Ant 1.10+ | Compatibilidad nativa con Apache NetBeans y portabilidad CLI. |
| **Parser Sintáctico** | [JavaParser 3.25.0](https://javaparser.org/) | Análisis de AST robusto y tipado sin compilar el proyecto. |
| **Generación UML** | [PlantUML 1.2023.13](https://plantuml.com/) | Generación programática de SVG en memoria. |
| **Runtime Tracing** | JDI / JPDA (`com.sun.jdi`) | Inspección no invasiva con política `SUSPEND_NONE`. |
| **Servidor & Streaming**| `com.sun.net.httpserver` + SSE | Servidor HTTP ultraligero embebido sin dependencias pesadas. |
| **Visor Web** | HTML5 / CSS3 / SVG Reactivo | Manipulación del DOM en tiempo real con 0 ms de latencia. |
| **IDE Soportado** | Apache NetBeans 18+ | Integración mediante *VM Options* estándar. |

---

## 📁 Estructura del Proyecto

```
TucanTrace/
├── build.xml                 # Script Ant (compile, run, run-jdi, jar, clean)
├── README.md                 # Documentación principal del repositorio
├── LICENSE                   # Licencia de código abierto MIT
├── lib/                      # Dependencias JAR (JavaParser, PlantUML, Gson, SLF4J)
├── docs/                     # Documentación técnica y académica completa
│   ├── arquitectura.md       # Arquitectura en 3 capas (Parser -> SVG -> JDI/SSE)
│   ├── svg-highlighting.md   # IDs semánticos, CSS dinámico y comparativa SVG vs PNG
│   ├── jdi-guia.md           # Guía de configuración JDWP/JDI en NetBeans (puerto 5005)
│   └── caso-estudio-tucango.md # Caso de estudio de 12 clases con TucanGo v5.0
├── demo_visual/              # Demostraciones visuales autónomas y scripts
│   ├── demo_live.html        # Comparativa interactiva PNG vs SVG
│   ├── tucango_live.html     # Visor en vivo pre-renderizado de TucanGo v5.0
│   └── generate_tucango_demo.py
└── src/
    └── tucantrace/
        ├── Main.java                 # Punto de entrada principal y orquestador CLI
        ├── agent/
        │   └── TraceAgent.java       # Agente Java opcional (-javaagent)
        ├── parser/
        │   ├── JavaParserAdapter.java    # Extracción de AST y DTOs
        │   └── PlantUMLGenerator.java    # Generación y enriquecimiento de SVG
        ├── runtime/
        │   └── JDIClient.java            # Conexión socket JDI y despachador de eventos
        └── ui/
            ├── DiagramViewer.java        # Visor de escritorio Swing integrado
            └── TraceWebServer.java       # Servidor HTTP embebido y streaming SSE
```

---

## ⚡ Guía Rápida de Uso

### 1. Prerrequisitos
- **Java Development Kit (JDK) 21** configurado en el `PATH`.
- **Apache NetBeans 18+** o **Apache Ant 1.10+**.

### 2. Compilar el Proyecto
```bash
ant compile
```

### 3. Ejecutar TucanTrace en Modo Estático / Demostración
Genera el diagrama UML del directorio fuente especificado y levanta el servidor web:
```bash
ant run
```
O especificando una ruta personalizada y puerto:
```bash
java -cp "dist/TucanTrace.jar:lib/*" tucantrace.Main /ruta/a/tu/codigo/src --port=8080
```
Abre tu navegador en: **`http://localhost:8080/`**

---

## 🔌 Conexión en Vivo con la JVM del Estudiante (JDI)

### Paso 1: Configurar NetBeans para el Proyecto del Estudiante
1. En Apache NetBeans, clic derecho en el proyecto del estudiante → **Properties**.
2. Selecciona la categoría **Run**.
3. En el campo **VM Options**, ingresa:
   ```properties
   -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
   ```
4. Haz clic en **OK** y ejecuta el proyecto en NetBeans (**F6**).

### Paso 2: Conectar TucanTrace con Enlace JDI
En una terminal, ejecuta TucanTrace con la bandera `--jdi`:
```bash
ant run-jdi
```
O directamente con el comando Java:
```bash
java -cp "dist/TucanTrace.jar:lib/*" tucantrace.Main /ruta/al/proyecto/estudiante/src --jdi --port=8080
```

### Paso 3: Observar el Trazado en Tiempo Real
Abre **`http://localhost:8080/`**. Al interactuar con la aplicación del estudiante, observarás cómo:
- Las clases cargadas se iluminan con resplandor azul (`ClassPrepare`).
- Los métodos invocados se resaltan en rojo negrita (`MethodEntry`).
- Los atributos modificados se destacan en color púrpura (`FieldModification`).
- La consola lateral registra la secuencia cronológica de eventos en milisegundos.

---

## 📚 Documentación Técnica Detallada

Para una profundización técnica y académica, consulta los documentos en la carpeta `docs/`:

- 📐 **[docs/arquitectura.md](docs/arquitectura.md):** Especificación del flujo de tres capas y diseño de componentes.
- 🎨 **[docs/svg-highlighting.md](docs/svg-highlighting.md):** Técnica de inyección de IDs semánticos, estilos CSS3, filtros `drop-shadow` y comparativa de rendimiento frente a PNG.
- ⚙️ **[docs/jdi-guia.md](docs/jdi-guia.md):** Guía paso a paso para la configuración del protocolo JDWP en Apache NetBeans y resolución de problemas.
- 🧪 **[docs/caso-estudio-tucango.md](docs/caso-estudio-tucango.md):** Validación integral con las 12 clases del modelo de dominio de *TucanGo v5.0*.

---

## 📄 Licencia

Este proyecto está bajo la Licencia **MIT** — libre para uso académico, investigativo y comercial.

---

## 👥 Autores y Reconocimientos

- **Semillero / Equipo TucanTrace & TucanGo**
- **Ingeniería de Sistemas** — Universidad de la Amazonia (Florencia, Caquetá, Colombia)
- Asignatura: *Lógica & Algoritmos II* — 2026
