# Arquitectura del Sistema TucanTrace

## 1. Visión General

**TucanTrace** es una plataforma pedagógica diseñada para la enseñanza de la Programación Orientada a Objetos (POO) y el análisis de algoritmos en cursos como *Lógica y Algoritmos II* de la Universidad de la Amazonia. Su objetivo principal es cerrar la brecha conceptual entre el código fuente estático y el comportamiento dinámico de los programas en tiempo de ejecución.

Para lograr este propósito sin alterar el código fuente de los estudiantes ni imponer dependencias intrusivas en sus proyectos, TucanTrace implementa un desacoplamiento estricto estructurado en **tres capas operacionales**:

1. **Capa de Análisis Estático (JavaParser & AST)**
2. **Capa de Generación y Transformación Vectorial Reactiva (PlantUML & SVG Post-Processor)**
3. **Capa de Runtime y Difusión en Tiempo Real (JDI Client & SSE Web Server)**

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                             CÓDIGO FUENTE DEL ESTUDIANTE                        │
│                           (Archivos .java del proyecto)                          │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│                   CAPA 1: ANÁLISIS ESTÁTICO (JavaParser)                         │
│  • Recorrido recursivo del árbol de directorios                                  │
│  • Construcción del Abstract Syntax Tree (CompilationUnit)                       │
│  • Extracción de Metadatos: Clases, Interfaces, Métodos, Atributos y Herencia   │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│              CAPA 2: GENERACIÓN VECTORIAL REACTIVA (PlantUML / SVG)              │
│  • Emisión de especificación PlantUML (@startuml ... @enduml)                    │
│  • Renderizado vectorial mediante SourceStringReader                             │
│  • Inyección de IDs semánticos (id='class_X', id='method_X_Y', id='field_X_Z')  │
│  • Inyección de Reglas CSS dinámicas y filtros de brillo (drop-shadow)           │
└────────────────────────────────────────┬─────────────────────────────────────────┘
                                         │
                                         ▼
┌──────────────────────────────────────────────────────────────────────────────────┐
│             CAPA 3: RUNTIME & STREAMING (JDI Client & Web Server SSE)            │
│  • JVM Estudiante: -agentlib:jdwp=transport=dt_socket,address=5005,server=y      │
│  • Conexión SocketAttach vía com.sun.jdi (VirtualMachine)                        │
│  • Eventos: ClassPrepare, MethodEntry, MethodExit, ModificationWatchpoint        │
│  • Servidor HTTP Embebido (TraceWebServer) transmitiendo por Server-Sent Events  │
│  • Interfaz Web: Manipulación del DOM SVG sin recargas (0 ms de latencia)        │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Capa 1: Análisis Estático de Código Fuente

### 2.1. Propósito y Tecnologías
La capa de análisis estático se encarga de inspeccionar el código fuente escrito por el estudiante sin ejecutarlo. Utiliza la biblioteca **JavaParser** (`com.github.javaparser`), una herramienta de análisis sintáctico que genera un Árbol de Sintaxis Abstracta (*Abstract Syntax Tree* o AST) completo para cada archivo `.java`.

### 2.2. Componentes Clave
El componente central es `tucantrace.parser.JavaParserAdapter`.

- **Recorrido de Archivos:** A través de la API `java.nio.file.Files.walk()`, el adaptador descubre todos los archivos `.java` en el directorio raíz proporcionado por el usuario.
- **Construcción del AST:** Cada archivo es procesado por una instancia de `JavaParser`, produciendo objetos `CompilationUnit`.
- **Extracción de Entidades:** El método `extractClassInfo(List<CompilationUnit>)` itera sobre las declaraciones de tipo (`ClassOrInterfaceDeclaration`), extrayendo:
  - **Identificadores y Modificadores:** Nombre de la clase, paquete de pertenencia, modificadores de acceso (`public`, `package-private`), si es abstracta o interfaz.
  - **Jerarquía y Contratos:** Clases extendidas (`getExtendedTypes()`) e interfaces implementadas (`getImplementedTypes()`).
  - **Atributos (`FieldDeclaration`):** Nombre de variable, tipo de dato y visibilidad.
  - **Métodos (`MethodDeclaration`):** Nombre, tipo de retorno, visibilidad y lista estructurada de parámetros formales con sus tipos.

### 2.3. Estructuras de Datos de Transferencia (DTOs)
Para garantizar la independencia entre la biblioteca de análisis y los generadores visuales, la información se normaliza en los siguientes objetos de transferencia de datos:
- `UMLClassInfo`: Representa una entidad de clase o interfaz.
- `UMLAttributeInfo`: Representa un campo/atributo con su respectivo tipo y modificador.
- `UMLMethodInfo`: Representa la signatura de un método y sus parámetros.

---

## 3. Capa 2: Generador Vectorial Reactivo

### 3.1. Pipeline de Generación
El componente `tucantrace.parser.PlantUMLGenerator` transforma los metadatos de las clases en una representación gráfica enriquecida siguiendo un pipeline de tres etapas:

1. **Generación del Script PlantUML:** Traduce la lista de `UMLClassInfo` en una gramática formal PlantUML, configurando directivas de dimensionamiento y estilos tipográficos (`skinparam svgDimensionStyle false`, visibilidad de iconos desactivada).
2. **Renderizado Vectorial Base:** Mediante la clase `net.sourceforge.plantuml.SourceStringReader`, se procesa el script en memoria y se obtiene el documento SVG crudo (`FileFormat.SVG`) sin tocar el disco.
3. **Post-Procesamiento e Inyección Semántica:** Se procesa el árbol XML/SVG mediante expresiones regulares y balanceo de etiquetas para enriquecer el documento.

### 3.2. Enriquecimiento del DOM Vectorial
El SVG generado por PlantUML agrupa los elementos bajo identificadores genéricos (`<g id="elem_ClassName">`). El post-procesador de TucanTrace reestructura este árbol:

- Asigna clases CSS a los grupos principales: `<g id="elem_ClassName" class="uml-class-group" data-class="ClassName">`.
- Identifica el rectángulo delimitador de la clase y le asigna un identificador determinista: `<rect id="class_ClassName" class="uml-class-box" data-class="ClassName">`.
- Mapea las etiquetas `<text>` correspondientes a métodos y campos, asignándoles identificadores únicos y atributos `data-*`:
  - Métodos: `<text id="method_ClassName_methodName" class="uml-method" data-class="ClassName" data-method="methodName">`
  - Campos: `<text id="field_ClassName_fieldName" class="uml-field" data-class="ClassName" data-field="fieldName">`
- Inyecta una hoja de estilos `<style>` embebida dentro de la sección `<defs>` del SVG, definiendo transiciones CSS suaves (`transition: all 0.3s ease`), animaciones de pulso (`@keyframes pulse-glow`) y filtros `drop-shadow`.

Este mecanismo convierte un gráfico estático en un lienzo totalmente manipulable mediante selectores estándar de JavaScript y hojas de estilo CSS.

---

## 4. Capa 3: Runtime JDI Client y Servidor Web SSE

### 4.1. Conexión de Depuración No Invasiva (JPDA / JDI)
La Java Platform Debugger Architecture (JPDA) provee la **Java Debug Interface (JDI)** (`com.sun.jdi`), una API de nivel superior que permite a un proceso externo inspeccionar el estado y controlar la ejecución de otra máquina virtual Java.

El componente `tucantrace.runtime.JDIClient` opera como un depurador desacoplado:
1. **Conexión por Sockets:** Utiliza el conector `com.sun.jdi.SocketAttach` para conectarse a la JVM del estudiante expuesta en la dirección configurada (por defecto `localhost:5005`).
2. **Registro de Solicitudes de Eventos (`EventRequestManager`):**
   - `MethodEntryRequest`: Se dispara inmediatamente cuando un hilo entra al cuerpo de cualquier método que cumpla con el filtro de paquetes (ej. `co.edu.uniamazonia.*` o `tucantrace.*`).
   - `MethodExitRequest`: Se emite cuando un método concluye su ejecución (retorno normal o excepción).
   - `ClassPrepareRequest`: Notifica la carga y preparación de nuevas clases en la memoria de la JVM.
   - `ModificationWatchpointRequest`: Se suscribe a los campos (`Field`) de cada tipo de referencia cargado para detectar escrituras y asignaciones de memoria.
3. **Política de Suspensión:** Se establece `EventRequest.SUSPEND_NONE` para que la máquina virtual del estudiante continúe su ejecución a velocidad nativa sin detenerse en puntos de interrupción.
4. **Bucle de Eventos en Hilo Daemon:** Un hilo dedicado (`JDI-EventLoop`) consume continuamente los conjuntos de eventos (`EventSet`) desde la cola `vm.eventQueue()`, los deserializa en instancias de `JDIEvent` y los despacha a los listeners suscritos.

### 4.2. Servidor Web Embebido y Streaming SSE
El componente `tucantrace.ui.TraceWebServer` está construido sobre el servidor HTTP nativo del JDK (`com.sun.net.httpserver.HttpServer`).

```
┌─────────────────┐       HTTP GET /diagram.svg       ┌─────────────────┐
│                 ├──────────────────────────────────►│                 │
│                 │◄──────────────────────────────────┤                 │
│                 │          SVG Enriquecido          │                 │
│  Navegador Web  │                                   │ TraceWebServer  │
│  (Cliente DOM)  │       GET /events (SSE Stream)    │  (Puerto 8080)  │
│                 ├──────────────────────────────────►│                 │
│                 │◄──────────────────────────────────┤                 │
│                 │  event: trace-event (JSON Payload)│                 │
└─────────────────┘                                   └────────▲────────┘
                                                               │
                                                       onEvent │
                                                               │
                                                      ┌────────┴────────┐
                                                      │    JDIClient    │
                                                      │  (Puerto 5005)  │
                                                      └─────────────────┘
```

- **Endpoints Servidos:**
  - `GET /`: Entrega la interfaz de usuario web responsiva (HTML5, CSS3, JavaScript nativo).
  - `GET /diagram.svg`: Suministra el documento SVG enriquecido generado en la Capa 2.
  - `GET /events`: Mantiene abierta una conexión HTTP persistente utilizando **Server-Sent Events (SSE)** (`Content-Type: text/event-stream`).
  - `GET/POST /api/highlight`: Endpoint REST para pruebas y simulación manual de eventos.
  - `GET/POST /api/clear`: Limpia todos los resaltados visuales activos.
  - `GET /api/status`: Retorna el estado operativo del servidor y el número de clientes conectados.

- **Mecanismo de Difusión en Tiempo Real:**
  Cuando el `JDIClient` captura un evento de ejecución (por ejemplo, la entrada al método `solicitarViaje`), `TraceWebServer` serializa el evento a formato JSON y lo transmite inmediatamente por la tubería de SSE a todos los navegadores conectados:

  ```http
  event: trace-event
  data: {"type":"MethodEntry","className":"Estudiante","methodName":"solicitarViaje","timestamp":1727189000000}
  ```

- **Actualización Reactiva en el Cliente Web:**
  El script del navegador recibe el evento en su listener `EventSource.onmessage`, localiza el nodo correspondiente mediante `document.querySelector('#method_Estudiante_solicitarViaje')` y añade dinámicamente la clase CSS `.highlight-active-method`. El motor de renderizado del navegador aplica la transición visual y el efecto de brillo instantáneamente, programando un temporizador para retirar el resaltado tras un intervalo configurable.

---

## 5. Matriz de Resumen de Componentes

| Capa | Componente | Responsabilidad Técnica | Dependencias Principales |
|------|------------|-------------------------|--------------------------|
| **1. Análisis Estático** | `JavaParserAdapter` | Análisis sintáctico del código fuente, recorrido de ficheros `.java`, extracción de clases, jerarquías, atributos y métodos en DTOs. | `com.github.javaparser:javaparser-core` |
| **2. Generación Vectorial** | `PlantUMLGenerator` | Conversión de DTOs a lenguaje PlantUML, renderizado a SVG en memoria, post-procesamiento e inyección de IDs semánticos y CSS. | `net.sourceforge.plantuml:plantuml` |
| **3. Runtime & Eventos** | `JDIClient` | Conexión por socket a la JVM objetivo (JDWP), intercepción asíncrona de `MethodEntry`, `MethodExit`, `ClassPrepare` y `FieldModification`. | `com.sun.jdi` (JDK Tools API) |
| **3. Servidor & Visualización** | `TraceWebServer` | Servidor HTTP ligero embebido, gestión de clientes SSE, entrega del SVG interactivo y log en vivo. | `com.sun.net.httpserver` (JDK core) |
| **3. Visor de Escritorio (Opcional)** | `DiagramViewer` | Ventana Swing opcional para entornos de escritorio, con acceso directo al navegador del sistema. | `java.desktop` (Swing/AWT) |

---

## 6. Conclusiones Arquitectónicas

1. **Cero Modificación de Código:** El estudiante programa siguiendo las convenciones habituales de Java; no requiere anotaciones, heredar de clases especiales ni compilar con dependencias ajenas a su proyecto.
2. **Bajo Overhead de Ejecución:** El uso de `SUSPEND_NONE` en JDI garantiza que la máquina virtual del estudiante opere a velocidad nativa sin bloqueos indeseados.
3. **Representación Vectorial de Alto Rendimiento:** A diferencia de las soluciones basadas en imágenes rasterizadas (PNG) que exigen regenerar y recargar el diagrama en cada invocación de método, TucanTrace manipula propiedades del DOM SVG en el navegador con un costo computacional de **O(1)** y **0 ms de latencia**.
