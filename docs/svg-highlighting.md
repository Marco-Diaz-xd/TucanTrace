# Técnica de Inyección y Resaltado Reactivo SVG en TucanTrace

## 1. Introducción y Justificación Técnica

Uno de los principales desafíos en el diseño de herramientas de visualización de software en tiempo real reside en la **tasa de refresco y la eficiencia del renderizado**. Cuando una aplicación orientada a objetos entra en ejecución, se producen decenas o cientos de invocaciones a métodos y modificaciones de atributos por segundo. 

Los enfoques tradicionales basados en imágenes rasterizadas (como PNG o JPEG) presentan serias limitaciones arquitectónicas:
- **Costo computacional elevado:** Para reflejar un cambio de estado, la herramienta debe regenerar por completo el código PlantUML, compilarlo con GraphViz y emitir un nuevo archivo binario de mapa de bits.
- **Latencia perceptible:** El ciclo de regeneración de una imagen rasterizada toma entre **150 ms y 600 ms** por fotograma, introduciendo parpadeos constantes (*flickering*) y desincronizando la visualización con respecto al hilo de ejecución real.
- **Degradación de calidad gráfica:** Al realizar operaciones de ampliación (*zoom*), las imágenes PNG pierden nitidez y presentan artefactos de pixelado que dificultan la lectura de nombres de métodos y atributos.

Para superar estas restricciones, **TucanTrace** implementa un modelo de **renderizado vectorial reactivo basado en SVG (Scalable Vector Graphics)**. En este modelo, el diagrama UML se genera una única vez como un documento XML vectorial estructurado. Posteriormente, cada elemento gráfico (clases, métodos y atributos) se manipula de forma atómica en el Document Object Model (DOM) mediante **inyección de identificadores semánticos** y **reglas de estilo CSS3 aceleradas por hardware**.

---

## 2. Inyección de Identificadores Semánticos (Semantic ID Injection)

PlantUML produce internamente un documento SVG donde las entidades son identificadas con una convención genérica (ej. `<g id="elem_Estudiante">`), pero los rectángulos de clase, textos de métodos y textos de atributos carecen de identificadores semánticos normalizados.

El componente `tucantrace.parser.PlantUMLGenerator` intercepta el flujo SVG resultante de `SourceStringReader` y ejecuta un algoritmo de post-procesamiento que enriquece el árbol XML con identificadores deterministas.

```
       SVG Crudo de PlantUML                         SVG Enriquecido por TucanTrace
┌─────────────────────────────────┐           ┌───────────────────────────────────────────────┐
│ <g id="elem_Estudiante">        │           │ <g id="elem_Estudiante"                       │
│   <rect id="Estudiante" .../>   │           │    class="uml-class-group"                    │
│   <text>+ solicitarViaje()</text>│───►Post──►│    data-class="Estudiante">                   │
│   <text>- codigo : String</text>│ Processor │   <rect id="class_Estudiante"                 │
│ </g>                            │           │         class="uml-class-box" .../>           │
│                                 │           │   <text id="method_Estudiante_solicitarViaje" │
│                                 │           │         class="uml-method" ...>               │
│                                 │           │   <text id="field_Estudiante_codigo"          │
│                                 │           │         class="uml-field" ...>                │
│                                 │           │ </g>                                          │
└─────────────────────────────────┘           └───────────────────────────────────────────────┘
```

### 2.1. Estructura de Identificadores Inyectados

TucanTrace utiliza el siguiente esquema canónico de nomenclatura:

| Nivel del Elemento | Patrón de Identificador (`id`) | Clases CSS Asignadas | Atributos `data-*` |
|--------------------|--------------------------------|----------------------|--------------------|
| **Grupo de Clase** | `elem_{ClassName}` | `uml-class-group` | `data-class="{ClassName}"` |
| **Caja/Contorno de Clase** | `class_{ClassName}` | `uml-class-box` | `data-class="{ClassName}"` |
| **Método** | `method_{ClassName}_{MethodName}` | `uml-method` | `data-class="{ClassName}"`<br>`data-method="{MethodName}"` |
| **Atributo / Campo** | `field_{ClassName}_{FieldName}` | `uml-field` | `data-class="{ClassName}"`<br>`data-field="{FieldName}"` |

### 2.2. Algoritmo de Enriquecimiento y Mapeo
El método `PlantUMLGenerator.enhanceSVG(...)` implementa la transformación en tres etapas:

1. **Aislamiento de Bloques de Clase:**
   Localiza la etiqueta de apertura `<g id="elem_{ClassName}">` y calcula el índice de la etiqueta de cierre `</g>` balanceando la profundidad de apertura y cierre de etiquetas XML.

2. **Reemplazo de la Caja Delimitadora:**
   Sustituye la etiqueta `<rect>` primaria mediante la expresión regular:
   ```java
   String newGroupBody = groupBody.replaceAll(
       "<rect ([^>]*)id=\"" + Pattern.quote(className) + "\"",
       "<rect $1id=\"class_" + className + "\" class=\"uml-class-box\" data-class=\"" + className + "\""
   );
   ```

3. **Mapeo de Métodos y Atributos mediante Regex:**
   Para cada método extraído en el AST (`UMLMethodInfo`), se busca la etiqueta de texto coincidente con su visibilidad y nombre:
   ```java
   Pattern methodPattern = Pattern.compile(
       "<text ([^>]*)>([+~#\\-]\\s*" + Pattern.quote(m.name) + "\\b[^<]*)</text>"
   );
   ```
   Al encontrar coincidencia, la etiqueta de texto original es sustituida por:
   ```xml
   <text id="method_Estudiante_solicitarViaje" class="uml-method" 
         data-class="Estudiante" data-method="solicitarViaje" ...>
     + solicitarViaje(String origen, String destino) : boolean
   </text>
   ```

   El mismo procedimiento se realiza para los atributos mediante:
   ```java
   Pattern fieldPattern = Pattern.compile(
       "<text ([^>]*)>([+~#\\-]\\s*" + Pattern.quote(attr.name) + "\\s*:[^<]*)</text>"
   );
   ```

---

## 3. Hoja de Estilos CSS3 y Animaciones Vectoriales

Una vez normalizado el árbol SVG, `PlantUMLGenerator` inyecta un bloque `<style>` dentro de la sección `<defs>` del documento. Esta capa de presentación desacoplada define las propiedades visuales en estado de reposo y en estado activo.

### 3.1. Definición de Reglas CSS Inyectadas

```css
/* Estado Base de Componentes */
.uml-class-group {
  cursor: pointer;
}

.uml-class-box {
  transition: fill 0.3s ease, stroke 0.3s ease, stroke-width 0.3s ease, filter 0.3s ease;
}

.uml-method, .uml-field {
  font-family: 'Consolas', 'Courier New', monospace;
  transition: fill 0.2s ease, font-size 0.2s ease, font-weight 0.2s ease, filter 0.2s ease;
  cursor: pointer;
}

/* 1. Resaltado de Clases en Ejecución (MethodEntry / Ejecución Activa) */
.highlight-active-class {
  fill: #fef08a !important;                 /* Amarillo suave pastel */
  stroke: #e11d48 !important;               /* Rojo carmín vibrante */
  stroke-width: 3px !important;
  filter: drop-shadow(0 0 12px rgba(225, 29, 72, 0.8)) !important;
  transition: all 0.2s ease-in-out;
}

/* 2. Resaltado de Carga de Clase (ClassPrepare / Carga en Memoria) */
.highlight-class-prepare {
  fill: #bae6fd !important;                 /* Azul cielo claro */
  stroke: #0284c7 !important;               /* Azul cobalto */
  stroke-width: 3px !important;
  filter: drop-shadow(0 0 10px rgba(2, 132, 199, 0.7)) !important;
  transition: all 0.2s ease-in-out;
}

/* 3. Resaltado de Método Invocado (MethodEntry) */
.highlight-active-method {
  fill: #e11d48 !important;                 /* Rojo vivo */
  font-weight: 900 !important;
  font-size: 12px !important;
  text-decoration: underline !important;
  filter: drop-shadow(0 0 6px rgba(225, 29, 72, 0.9)) !important;
  transition: all 0.2s ease-in-out;
}

/* 4. Resaltado de Modificación de Atributo (FieldModification / Watchpoint) */
.highlight-active-field {
  fill: #7c3aed !important;                 /* Púrpura eléctrico */
  font-weight: 900 !important;
  font-size: 12px !important;
  filter: drop-shadow(0 0 6px rgba(124, 58, 237, 0.9)) !important;
  transition: all 0.2s ease-in-out;
}

/* 5. Animación de Pulso Continuo para Hilos Activos */
.pulse-active {
  animation: pulse-glow 1.2s infinite alternate ease-in-out;
}

@keyframes pulse-glow {
  0% {
    filter: drop-shadow(0 0 4px #e11d48);
  }
  100% {
    filter: drop-shadow(0 0 16px #fb7185);
  }
}
```

### 3.2. Mecanismo del Filtro `drop-shadow`
El uso de `filter: drop-shadow(x-offset y-offset blur-radius color)` proporciona un efecto de resplandor (*glow*) perimetral sobre los bordes vectoriales reales de las figuras geométricas y textos del SVG. Al ser procesado por la GPU del navegador mediante compositores de capas (*hardware accelerated layers*), el efecto se calcula en tiempo real sin degradar el rendimiento del hilo principal de JavaScript.

---

## 4. Manipulación del DOM en Tiempo Real (Cliente Web)

Cuando el navegador recibe una notificación de evento desde el canal SSE (`/events`), la función JavaScript `handleTraceEvent(event)` ejecuta la actualización visual:

```javascript
function applySvgHighlight(type, className, memberName) {
  const svg = document.querySelector('#diagram-svg-wrapper svg');
  if (!svg) return;

  const simpleCls = className.includes('.') ? className.split('.').pop() : className;

  // 1. Localización y activación de la clase
  const classRect = svg.querySelector(`#class_${simpleCls}`);
  if (classRect) {
    const cssClass = (type === 'ClassPrepare') ? 'highlight-class-prepare' : 'highlight-active-class';
    classRect.classList.add(cssClass);
    setTimeout(() => classRect.classList.remove(cssClass), 2500);
  }

  // 2. Localización y activación del método
  if (type === 'MethodEntry' || type === 'MethodExit') {
    const methodEl = svg.querySelector(`#method_${simpleCls}_${memberName}`);
    if (methodEl) {
      methodEl.classList.add('highlight-active-method');
      setTimeout(() => methodEl.classList.remove('highlight-active-method'), 2500);
    }
  }

  // 3. Localización y activación del atributo
  if (type === 'FieldModification') {
    const fieldEl = svg.querySelector(`#field_${simpleCls}_${memberName}`);
    if (fieldEl) {
      fieldEl.classList.add('highlight-active-field');
      setTimeout(() => fieldEl.classList.remove('highlight-active-field'), 2500);
    }
  }
}
```

---

## 5. Tabla Comparativa: Renderizado Raster (PNG) vs. Renderizado Vectorial (SVG)

| Criterio de Evaluación | Enfoque PNG / Raster Tradicional | Enfoque SVG Reactivo TucanTrace |
|------------------------|-----------------------------------|----------------------------------|
| **Tiempo de Respuesta ante Eventos** | **150 ms – 600 ms** (requiere re-generación completa y parsing de imagen). | **< 1 ms (0 ms perceptibles)** (adición de clase CSS en el DOM existente). |
| **Consumo de CPU / Memoria** | Alto: invoca el generador de imágenes y crea nuevos buffers binarios por cada evento. | Mínimo: una sola carga en memoria del árbol XML y reutilización del DOM. |
| **Escalabilidad Visual (Zoom)** | Deficiente: se pixela al ampliar; ilegible en pantallas de alta resolución (4K/Retina). | Perfecta: resolución infinita y renderizado nítido en cualquier escala. |
| **Interactividad con el Usuario** | Nula: imagen plana sin conocimiento de coordenadas semánticas. | Total: soporte para eventos `click`, `hover`, tooltips e inspección de elementos. |
| **Capacidades de Animación** | Inexistente o mediante sustitución abrupta de fotogramas. | Fluida: transiciones CSS3 (`ease-in-out`), animaciones de pulso (`@keyframes`) y sombras `drop-shadow`. |
| **Sobrecarga de Red (Streaming)** | Alta: retransmisión de archivos binarios de ~50 KB a ~300 KB por evento. | Mínima: paquetes JSON por SSE de menos de 120 bytes (`{"type":"MethodEntry",...}`). |

---

## 6. Conclusiones

La arquitectura de resaltado basada en **SVG Reactivo + IDs Semánticos** de TucanTrace resuelve de forma definitiva el cuello de botella de la visualización dinámica de código. Al separar el modelado estático (generado una única vez) de la capa de animación dinámica (controlada por eventos ligeros a través de CSS), se logra una herramienta de alto impacto didáctico con latencia nula y fidelidad gráfica óptima para entornos educativos universitarios.
