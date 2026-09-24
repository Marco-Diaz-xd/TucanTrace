# Caso de Estudio: TucanGo v5.0 — Traza de Ejecución y Validación en Vivo

## 1. Contexto Académico y Dominio del Problema

El proyecto **TucanGo v5.0** es un sistema de gestión de movilidad estudiantil universitaria desarrollado como proyecto de aula para la asignatura *Lógica y Algoritmos II* en el programa de Ingeniería de Sistemas de la Universidad de la Amazonia. Su objetivo es coordinar viajes compartidos en motocicleta entre estudiantes de la sede principal y sedes periféricas de Florencia (Caquetá), garantizando trazabilidad, validación de requisitos de seguridad y reporte de pagos.

Este sistema constituye el **banco de pruebas principal (benchmark)** para validar las capacidades de análisis estático, generación vectorial e intercepción en tiempo real de **TucanTrace**.

---

## 2. Catálogo de Clases del Dominio (12 Entidades)

El modelo de dominio de TucanGo v5.0 está compuesto por 12 entidades que abarcan clases abstractas, herencia simple, relaciones de agregación y composición, colecciones y servicios de orquestación de negocio.

```
                               ┌───────────────────────────┐
                               │     <<abstract>>          │
                               │        Persona            │
                               └─────────────┬─────────────┘
                                             │
                      ┌──────────────────────┼──────────────────────┐
                      │ <|--                 │ <|--                 │ <|--
          ┌───────────┴───────────┐ ┌────────┴───────────┐ ┌────────┴───────────┐
          │      Estudiante       │ │     Motorista      │ │    Administrador    │
          └───────────┬───────────┘ └────────┬───────────┘ └─────────────────────┘
                      │                      │
                      │                      │ 1..*
                      │                      ▼
                      │             ┌────────────────────┐
                      │             │        Moto        │
                      │             └────────────────────┘
                      │                      │
                      │ 1                    │ 1
                      ▼                      ▼
           ┌──────────────────────────────────────────────┐
           │                    Viaje                     │
           └──────────────────┬───────────────────────────┘
                              │
               ┌──────────────┴──────────────┐
             1 │ 1                         1 │ 1..*
               ▼                             ▼
       ┌───────────────┐             ┌───────────────┐
       │     Pago      │             │  Calificacion │
       └───────────────┘             └───────────────┘
```

A continuación se detallan las 12 clases procesadas por el parser AST de TucanTrace:

### 2.1. `Persona` (Clase Abstracta)
- **Rol:** Clase base de la jerarquía de actores del sistema.
- **Atributos Protegidos:** `identificacion: String`, `nombre: String`, `telefono: String`, `correoInstitucional: String`.
- **Métodos:** `obtenerIdentificacion()`, `obtenerNombreCompleto()`, getters y setters encapsulados.

### 2.2. `Estudiante` (Especialización de Persona)
- **Rol:** Usuario que demanda servicios de transporte.
- **Atributos:** `codigoEstudiantil: String`, `viajesSolicitados: List<Viaje>`.
- **Métodos:** `solicitarViaje(origen, destino)`, `solicitarViaje(origen, destino, tarifa)` (sobrecarga polimórfica), `marcarLlegadaSegura(codigoViaje)`.

### 2.3. `Motorista` (Especialización de Persona)
- **Rol:** Conductor universitario autorizado que presta el servicio.
- **Atributos:** `disponible: boolean`, `motos: List<Moto>`, `viajesAtendidos: List<Viaje>`.
- **Métodos:** `registrarMoto(moto)`, `seleccionarMotoActiva(placa)`, `obtenerMotoActiva()`, `aceptarViaje(codigoViaje)`, `aceptarViaje(viaje)`, `registrarDisponibilidad(estado)`, `verificarDocumentos()`.

### 2.4. `Administrador` (Especialización de Persona)
- **Rol:** Gestión de seguridad y control operativo.
- **Atributos:** `rol: RolAdmin`.
- **Métodos:** `consultarReportesDemanda()`, `gestionarUsuarios()`.

### 2.5. `Moto` (Entidad de Recurso Físico)
- **Rol:** Vehículo registrado para la prestación del servicio.
- **Atributos:** `placa: String`, `marca: String`, `modelo: String`, `cilindraje: int`, `soatVigente: boolean`, `numeroSoat: String`, `activa: boolean`.
- **Métodos:** `validarSoat()`.

### 2.6. `Viaje` (Entidad Núcleo de Transacción)
- **Rol:** Representa el acuerdo de transporte entre un estudiante y un motorista.
- **Atributos:** `codigoViaje: String`, `origen: String`, `destino: String`, `tarifa: double`, `estado: EstadoViaje`, `fechaHora: String`, `pago: Pago`, `calificaciones: List<Calificacion>`.
- **Métodos:** `calcularTarifa()`, `iniciarViaje()`, `finalizarViaje()`, `cancelarViaje()`, `agregarCalificacion(calificacion)`.

### 2.7. `Pago` (Entidad Financiera)
- **Rol:** Registro y conciliación del intercambio económico.
- **Atributos:** `valor: double`, `metodo: MetodoPago`, `estado: EstadoPago`, `pagadoPorEstudiante: boolean`, `confirmadoPorMotorista: boolean`, `fechaHora: String`.
- **Métodos:** `reportarPagoEstudiante(metodo)`, `confirmarRecepcionMotorista()`.

### 2.8. `Calificacion` (Entidad de Reputación)
- **Rol:** Evaluación de calidad y seguridad del servicio prestado.
- **Atributos:** `puntaje: double`, `comentario: String`, `rolEmisor: String`, `fechaHora: String`.
- **Métodos:** `validarPuntaje(puntaje)`, `registrarCalificacion(puntaje, comentario, emisor)`, `obtenerPuntaje()`.

### 2.9. `ServicioAutenticacion` (Servicio de Dominio)
- **Rol:** Control de acceso institucional y validación de credenciales.
- **Atributos:** `usuariosRegistrados: Map<String, Persona>`, `credenciales: Map<String, String>`, `sesionesActivas: Map<String, Persona>`.
- **Métodos:** `registrarse(persona, clave)`, `iniciarSesion(correo, clave)`, `cerrarSesion(identificacion)`, `estaAutenticado(identificacion)`.

### 2.10. `ServicioReportes` (Servicio de Dominio)
- **Rol:** Análisis estadístico y métricas de movilidad.
- **Atributos:** `historicoViajes: List<Viaje>`.
- **Métodos:** `registrarViaje(viaje)`, `obtenerDestinosMasFrecuentes(limite)`, `consultarViajesPorDestino(destino)`, `calcularPorcentajePorSector(sector)`.

### 2.11. `DemoTucanGo` (Clase de Ejecución / Escenario)
- **Rol:** Orquestador del caso de uso de prueba que simula un ciclo de vida completo de viaje.
- **Métodos:** `main(String[] args)`.

### 2.12. `Main` (Punto de Entrada General)
- **Rol:** Lanzador de consola y configuración de la aplicación.
- **Métodos:** `main(String[] args)`.

---

## 3. Traza de Ejecución en Vivo (Live Trace Scenario)

Para la sustentación y demostración de TucanTrace, se ejecuta el escenario principal: **"Solicitud, Aceptación, Validación de SOAT, Pago Digital y Calificación de Viaje"**.

A continuación se presenta el registro temporal detallado de los eventos interceptados por el cliente JDI y transmitidos en tiempo real al visor SVG web:

```
┌────────────────────────────────────────────────────────────────────────────────────────────────┐
│                          SECUENCIA DE EJECUCIÓN TEMPORAL EN RUNTIME                            │
└────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                                                                  
 1. [ClassPrepare]      Persona, Estudiante, Motorista, Moto, Viaje, Pago, Calificacion           
 2. [MethodEntry]       Estudiante.solicitarViaje("Campus Porvenir", "Centro")                    
 3. [MethodEntry]       Viaje.<init>("V-102", "Campus Porvenir", "Centro")                        
 4. [FieldModification] Viaje.estado = SOLICITADO                                                 
 5. [MethodExit]        Estudiante.solicitarViaje -> true                                         
 6. [MethodEntry]       Motorista.aceptarViaje(viaje)                                             
 7. [MethodEntry]       Moto.validarSoat()                                                        
 8. [FieldModification] Moto.soatVigente = true                                                   
 9. [MethodExit]        Moto.validarSoat -> true                                                  
10. [FieldModification] Viaje.estado = EN_CURSO                                                   
11. [MethodExit]        Motorista.aceptarViaje -> true                                            
12. [MethodEntry]       Pago.reportarPagoEstudiante(NEQUI)                                        
13. [FieldModification] Pago.pagadoPorEstudiante = true                                            
14. [MethodEntry]       Pago.confirmarRecepcionMotorista()                                        
15. [FieldModification] Pago.estado = CONFIRMADO                                                  
16. [MethodEntry]       Viaje.finalizarViaje()                                                    
17. [FieldModification] Viaje.estado = FINALIZADO                                                 
18. [MethodEntry]       Calificacion.registrarCalificacion(5.0, "Excelente servicio", "Estudiante")
19. [MethodEntry]       Calificacion.validarPuntaje(5.0)                                          
20. [MethodExit]        Calificacion.registrarCalificacion                                       
```

---

## 4. Correlación entre Eventos JDI y Nodos SVG Resaltados

Durante la ejecución del escenario anterior, el motor de TucanTrace traduce los eventos de la JVM en selectores del DOM SVG:

| Paso | Evento JDI Capturado | Selector del Nodo SVG Afectado | Clase CSS Dinámica Aplicada | Efecto Visual en Pantalla |
|------|-----------------------|--------------------------------|-----------------------------|---------------------------|
| **1** | `ClassPrepare: Estudiante` | `#class_Estudiante` | `.highlight-class-prepare` | Resplandor azul perimetral indicando carga en memoria. |
| **2** | `MethodEntry: Estudiante.solicitarViaje` | `#method_Estudiante_solicitarViaje` | `.highlight-active-method` | Texto en rojo negrita con subrayado y resplandor. |
| **3** | `MethodEntry: Viaje.<init>` | `#class_Viaje` | `.highlight-active-class` | Fondo amarillo suave y borde rojo con animación de sombra. |
| **4** | `FieldModification: Viaje.estado` | `#field_Viaje_estado` | `.highlight-active-field` | Texto del atributo en color púrpura vibrante. |
| **5** | `MethodEntry: Motorista.aceptarViaje` | `#method_Motorista_aceptarViaje` | `.highlight-active-method` | Activación del método en el bloque de la clase `Motorista`. |
| **6** | `MethodEntry: Moto.validarSoat` | `#method_Moto_validarSoat` | `.highlight-active-method` | Resaltado instantáneo de la regla de validación de seguridad. |
| **7** | `FieldModification: Pago.pagadoPorEstudiante` | `#field_Pago_pagadoPorEstudiante` | `.highlight-active-field` | Confirmación de escritura en memoria del estado de pago. |
| **8** | `MethodEntry: Calificacion.registrarCalificacion` | `#method_Calificacion_registrarCalificacion` | `.highlight-active-method` | Resaltado del registro de calidad al concluir el viaje. |

---

## 5. Demostración Visual Embebida

Para propósitos de sustentación y pruebas offline, el repositorio cuenta con una demostración interactiva pre-renderizada en la ruta:

```text
demo_visual/tucango_live.html
```

Este archivo contiene el SVG generado para las 12 clases de TucanGo v5.0 con los estilos de animación inyectados y un panel de control con simulación paso a paso de la secuencia JDI, permitiendo evidenciar el comportamiento reactivo directamente en cualquier navegador web moderno.

---

## 6. Resultados y Valor Pedagógico

La aplicación de TucanTrace sobre TucanGo v5.0 demostró los siguientes beneficios en el aula:
1. **Comprensión Inmediata de la Delegación:** Los estudiantes observan visualmente cómo una llamada en `Estudiante.solicitarViaje` activa la creación de una instancia en `Viaje` y posteriormente interactúa con `Motorista` y `Moto`.
2. **Inspección de Encapsulamiento y Estado:** Los eventos `FieldModification` permiten verificar que las mutaciones de atributos ocurren a través de métodos autorizados y no por acceso directo indebido.
3. **Validación del Polimorfismo y Herencia:** El diagrama resalta la relación de especialización `Persona <|-- Estudiante` y `Persona <|-- Motorista`, facilitando el entendimiento de la reutilización de código en Java.
