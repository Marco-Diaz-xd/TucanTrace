package co.edu.uniamazonia.logica2;

import co.edu.uniamazonia.logica2.modelo.*;
import co.edu.uniamazonia.logica2.servicio.*;

/**
 * Demostración del modelo de dominio de TucanGo (Lógica y Algoritmos II).
 * Flujo completo con arquitectura POO v5.0:
 * - Herencia con clase abstracta Persona (Estudiante, Motorista, Administrador)
 * - Módulo Moto independiente (soporte multi-moto)
 * - Registro, solicitud, pago informado y calificaciones mutuas
 *
 * @author Juan Guillermo Ferrer Gasca & Gian Marco Castañeda
 * @version 5.0
 */
public class DemoTucanGo {

    public static void main(String[] args) {

        System.out.println("=====================================================");
        System.out.println("   TucanGo — Demostración del Modelo de Dominio v5.0");
        System.out.println("=====================================================\n");

        // 1. Crear e interactuar con Servicio de Autenticación
        ServicioAutenticacion auth = new ServicioAutenticacion();

        Estudiante estudiante = new Estudiante("1001", "Juan Guillermo Ferrer Gasca", "3001234567", "j.ferrer@uniamazonia.edu.co", "20231001");
        Motorista motorista = new Motorista("1000456789", "Carlos Motorista", "3109876543", "c.motorista@uniamazonia.edu.co", true);
        
        // Registrar moto con SOAT válido
        Moto motoPrincipal = new Moto("XYZ-123", "Yamaha", "FZ-25", 250, true, "SOAT-2026-999", true);
        motorista.registrarMoto(motoPrincipal);

        auth.registrarse(estudiante, "claveSegura123");
        auth.registrarse(motorista, "claveSegura456");

        System.out.println("--- 1. Actores registrados ---");
        System.out.println("Estudiante: " + estudiante.getNombre() + " | Código: " + estudiante.getCodigoEstudiantil());
        System.out.println("Motorista:  " + motorista.getNombre() + " | SOAT Vigente: " + motorista.verificarDocumentos());
        System.out.println("Moto:       " + motoPrincipal.getMarca() + " " + motoPrincipal.getModelo() + " [" + motoPrincipal.getPlaca() + "]\n");

        // 2. Solicitud de Viaje
        System.out.println("--- 2. Solicitud y Aceptación de Viaje ---");
        Viaje viaje = new Viaje("VIA-001", "Campus Porvenir", "Barrio Centro", 4500.0);
        estudiante.solicitarViaje(viaje.getOrigen(), viaje.getDestino());
        System.out.println("Viaje " + viaje.getCodigoViaje() + " solicitado de: " + viaje.getOrigen() + " -> " + viaje.getDestino());
        System.out.println("Tarifa pactada: $" + viaje.getTarifa());

        motorista.aceptarViaje(viaje.getCodigoViaje());
        viaje.iniciarViaje();
        System.out.println("Estado del viaje: " + viaje.getEstado());

        // 3. Finalización y Pago
        System.out.println("\n--- 3. Llegada Segura y Pago ---");
        estudiante.marcarLlegadaSegura(viaje.getCodigoViaje());
        viaje.finalizarViaje();
        System.out.println("Viaje finalizado. Estado: " + viaje.getEstado());

        Pago pago = new Pago(viaje.getTarifa(), MetodoPago.NEQUI);
        viaje.setPago(pago);
        pago.reportarPagoEstudiante(MetodoPago.NEQUI);
        pago.confirmarRecepcionMotorista();
        System.out.println("Pago registrado por: " + pago.getMetodo() + " | Estado: " + pago.getEstado());

        // 4. Calificaciones Mutuas
        System.out.println("\n--- 4. Calificaciones Mutuas ---");
        Calificacion califEstudiante = new Calificacion(5.0, "Motorista muy amable, viaje seguro y puntual.", "ESTUDIANTE");
        Calificacion califMotorista = new Calificacion(4.9, "Estudiante puntual en la salida.", "MOTORISTA");
        viaje.agregarCalificacion(califEstudiante);
        viaje.agregarCalificacion(califMotorista);

        for (Calificacion c : viaje.getCalificaciones()) {
            System.out.println(" - [" + c.getRolEmisor() + "] " + c.getPuntaje() + "/5.0: \"" + c.getComentario() + "\"");
        }

        System.out.println("\n=====================================================");
        System.out.println("   ✓ Demostración completada sin errores (Java 21)");
        System.out.println("=====================================================");
    }
}
