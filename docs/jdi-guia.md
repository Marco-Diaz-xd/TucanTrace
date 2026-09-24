# Guía de Configuración y Conexión JDWP/JDI en Apache NetBeans

## 1. Fundamentos de la Arquitectura JPDA

La **Java Platform Debugger Architecture (JPDA)** es el marco estándar de depuración e inspección provisto por la especificación de Java. Se compone de tres capas integradas:

```
┌──────────────────────────────────────────────────────────────────┐
│                   JVM DEL ESTUDIANTE (Debuggee)                  │
│                     Ejecución del Código Java                    │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │     JVM TI (Java Virtual Machine Tool Interface)           │  │
│  └─────────────────────────────┬──────────────────────────────┘  │
│                                │                                 │
│  ┌─────────────────────────────▼──────────────────────────────┐  │
│  │     JDWP (Java Debug Wire Protocol) [Agente Nativo]         │  │
│  └─────────────────────────────┬──────────────────────────────┘  │
└────────────────────────────────┼─────────────────────────────────┘
                                 │
                 Socket TCP/IP (localhost:5005)
                                 │
┌────────────────────────────────┼─────────────────────────────────┐
│  ┌─────────────────────────────▼──────────────────────────────┐  │
│  │     JDI (Java Debug Interface - com.sun.jdi)                │  │
│  └─────────────────────────────┬──────────────────────────────┘  │
│                                │                                 │
│                   PROCESO TUCANTRACE (Debugger)                  │
│            Captura de Eventos y Difusión a Visor Web             │
└──────────────────────────────────────────────────────────────────┘
```

1. **JVM TI (Java Virtual Machine Tool Interface):** Interfaz nativa de bajo nivel provista por la máquina virtual para monitoreo y profiling.
2. **JDWP (Java Debug Wire Protocol):** Protocolo de comunicación que define el formato de paquetes y la señalización entre el proceso depurado y el proceso depurador a través de canales de transporte (típicamente sockets TCP/IP).
3. **JDI (Java Debug Interface):** API de alto nivel en Java puro (`com.sun.jdi`) que utiliza TucanTrace para suscribirse a eventos del ciclo de vida del código (invocación de métodos, mutación de variables y carga de clases).

Esta arquitectura permite que TucanTrace funcione como un observador completamente **desacoplado y no intrusivo**, sin requerir modificaciones en el código fuente ni bibliotecas adicionales en el proyecto del estudiante.

---

## 2. Configuración de la JVM del Estudiante en Apache NetBeans

Para habilitar el agente JDWP en la aplicación del estudiante, se deben especificar los parámetros de la máquina virtual (*VM Options*) en la configuración del proyecto dentro de Apache NetBeans.

### 2.1. Instrucciones Paso a Paso en NetBeans

1. **Abrir el Proyecto del Estudiante:**
   Inicie Apache NetBeans (versión 18 o superior) y abra el proyecto de Java (Ant o Maven) que desea supervisar (por ejemplo, `TucanGo`).

2. **Acceder a las Propiedades del Proyecto:**
   Haga clic derecho sobre el nodo raíz del proyecto en la pestaña *Projects* y seleccione la opción **Properties** (o presione `Alt + F12`).

   ```
   [Proyectos NetBeans]
   └── 📁 TucanGo  <-- (Clic derecho -> Properties)
   ```

3. **Navegar a la Sección de Ejecución (*Run*):**
   En el panel lateral izquierdo de la ventana de propiedades, seleccione la categoría **Run**.

4. **Configurar las Opciones de VM (*VM Options*):**
   En el campo de texto etiquetado como **VM Options**, agregue la siguiente línea de configuración:

   ```properties
   -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
   ```

   *(En entornos con Java 9 en adelante que requieran enlace explícito en todas las interfaces de red locales, puede utilizarse `address=*:5005` o `address=127.0.0.1:5005`).*

5. **Guardar los Cambios:**
   Haga clic en el botón **OK** para aplicar y persistir la configuración en los metadatos del proyecto.

---

## 3. Desglose de Parámetros JDWP

Cada parámetro dentro de la directiva `-agentlib:jdwp` cumple una función específica:

| Parámetro | Valor Configurado | Significado y Justificación Técnica |
|-----------|-------------------|--------------------------------------|
| `transport` | `dt_socket` | Define el mecanismo de transporte de datos. `dt_socket` establece la comunicación mediante sockets TCP/IP estándar de red local. |
| `server` | `y` | Configura la JVM del estudiante como **servidor de escucha**. La máquina virtual abre el puerto y espera la conexión de clientes depuradores externos. |
| `suspend` | `n` | **No suspende la ejecución.** La aplicación arranca de inmediato sin esperar a que TucanTrace se conecte. *(Si se configurara en `y`, la JVM quedaría congelada en el inicio hasta que TucanTrace establezca el enlace)*. |
| `address` | `5005` | Número de puerto TCP en el cual el agente JDWP escuchará solicitudes entrantes. |

---

## 4. Ejecución del Flujo de Trabajo y Conexión de TucanTrace

Para que la captura y visualización en tiempo real opere correctamente, se recomienda seguir la secuencia de inicialización descrita a continuación:

### Paso 1: Ejecutar la Aplicación del Estudiante en NetBeans
1. En NetBeans, haga clic derecho sobre el proyecto del estudiante y seleccione **Run** (o presione `F6`).
2. La consola de salida de NetBeans indicará la activación del agente:
   ```text
   Listening for transport dt_socket at address: 5005
   Iniciando aplicación del estudiante...
   ```
3. La aplicación continúa su flujo normal de ejecución mientras el agente JDWP mantiene el puerto `5005` listo para transmitir eventos.

### Paso 2: Iniciar TucanTrace con Enlace JDI
Abra una terminal o ventana de comandos y ejecute TucanTrace indicando la ruta del código fuente del estudiante y activando la bandera `--jdi`:

```bash
# Opción A: Ejecución mediante Ant
ant run-jdi

# Opción B: Ejecución directa con el JAR compilado
java -cp "dist/TucanTrace.jar:lib/*" tucantrace.Main /ruta/al/proyecto/estudiante/src --jdi --port=8080
```

### Paso 3: Confirmación de Enlace en Consola
La consola de TucanTrace mostrará los mensajes de confirmación de las tres capas:

```text
=========================================================
  TUCANTRACE v0.1 - Live UML Visualization for Java     
  Universidad de la Amazonia - Ingeniería de Sistemas   
=========================================================

Directorio de código fuente: /home/estudiante/NetBeansProjects/TucanGo/src
Servidor Web puerto: 8080
JDI habilitado: true

--- 1. Análisis estático (JavaParser -> PlantUML) ---
  Clases parseadas: 12
  PlantUML generado (4820 chars)
  Archivo .puml guardado en build/tucantrace-diagram.puml
  Archivo .svg interactivo guardado en build/tucantrace-diagram.svg

--- 2. Servidor Web Embebido (Visor SVG Live) ---
🌐 TucanTrace Web Server iniciado en http://localhost:8080/
  ✅ Servidor Web iniciado correctamente en http://localhost:8080/

--- 3. Conexión JDI en vivo ---
  Conectando a localhost:5005 ...
✅ JDI conectado a localhost:5005
  Escuchando eventos: MethodEntry, FieldModification, ClassPrepare...

=========================================================
  TUCANTRACE ACTIVO: Abre http://localhost:8080/ en tu navegador
=========================================================
```

### Paso 4: Visualización Interactiva en el Navegador
1. Abra su navegador web preferido y navegue a `http://localhost:8080/`.
2. Observe el diagrama UML renderizado vectorialmente.
3. A medida que el estudiante interactúa con su aplicación (o se ejecutan pruebas unitarias), los rectángulos de las clases y los textos de los métodos se iluminarán con animaciones de resplandor rojo y amarillo en tiempo real.

---

## 5. Diagnóstico y Solución de Problemas Comunes (Troubleshooting)

### 5.1. Error: `java.net.ConnectException: Connection refused`
- **Causa:** TucanTrace intentó conectarse al puerto `5005`, pero la JVM del estudiante no está en ejecución o no tiene los parámetros JDWP configurados.
- **Solución:** Verifique que la aplicación del estudiante haya iniciado en NetBeans y que en la consola aparezca `Listening for transport dt_socket at address: 5005` antes de iniciar TucanTrace.

### 5.2. Error: `java.net.BindException: Address already in use: 5005`
- **Causa:** Otro proceso previo (una instancia anterior de Java o de depuración) está reteniendo el puerto `5005`.
- **Solución:**
  - En Linux/macOS: Identifique y cierre el proceso con:
    ```bash
    lsof -i :5005
    kill -9 <PID>
    ```
  - En Windows:
    ```cmd
    netstat -ano | findstr :5005
    taskkill /PID <PID> /F
    ```
  - Alternativamente, cambie el puerto a `5006` tanto en las *VM Options* de NetBeans como en el código/parámetros de TucanTrace.

### 5.3. Eventos no se Visualizan (Filtros de Paquete Inadecuados)
- **Causa:** El cliente JDI utiliza filtros de clase (`addClassFilter`) para omitir las clases internas del JDK (`java.lang.*`, `java.util.*`). Si el paquete del estudiante no coincide con el patrón configurado, los eventos son ignorados.
- **Solución:** Al instanciar `JDIClient`, asegúrese de pasar el patrón correspondiente al paquete del estudiante (por ejemplo, `co.edu.uniamazonia.*`, `tucango.*` o `*` para capturar todas las clases del dominio).

---

## 6. Resumen de Buenas Prácticas Docentes

1. **Mantener `suspend=n`:** Para demostraciones en clase y laboratorios, evita que los estudiantes crean que su programa se ha colgado.
2. **Utilizar el Navegador Web:** Proyectar la interfaz web `http://localhost:8080/` en pantalla dividida junto al IDE NetBeans permite a los estudiantes asociar visualmente cada línea de código ejecutada con la activación del método en el diagrama UML.
