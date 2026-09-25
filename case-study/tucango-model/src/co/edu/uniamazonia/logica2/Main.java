package co.edu.uniamazonia.logica2;

import co.edu.uniamazonia.logica2.modelo.Administrador;
import co.edu.uniamazonia.logica2.modelo.Calificacion;
import co.edu.uniamazonia.logica2.modelo.EstadoPago;
import co.edu.uniamazonia.logica2.modelo.Estudiante;
import co.edu.uniamazonia.logica2.modelo.MetodoPago;
import co.edu.uniamazonia.logica2.modelo.Moto;
import co.edu.uniamazonia.logica2.modelo.Motorista;
import co.edu.uniamazonia.logica2.modelo.RolAdmin;
import co.edu.uniamazonia.logica2.modelo.Viaje;
import co.edu.uniamazonia.logica2.servicio.ServicioAutenticacion;
import co.edu.uniamazonia.logica2.servicio.ServicioReportes;

/**
 * Demostración del Ecosistema de Clases de TucanGo v5.0.
 * Valida la colaboración de objetos, herencia de Persona, multi-motos,
 * pagos, calificaciones [1.0 - 5.0], servicios de autenticación y reportes.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("       TUCANGO v5.0 - ECOSISTEMA DE MOVILIDAD CAMPUS          ");
        System.out.println("        Lógica y Algoritmos II · Univ. de la Amazonia          ");
        System.out.println("===============================================================\n");

        // 1. Instanciación de Servicios
        ServicioAutenticacion authService = new ServicioAutenticacion();
        ServicioReportes reportesService = new ServicioReportes();

        // 2. Instanciación de Actores (Jerarquía Persona)
        Estudiante estudiante = new Estudiante(
                "1117540001",
                "Gian Marco Castañeda",
                "3101234567",
                "g.castaneda@udla.edu.co",
                "EST-2026-042"
        );

        Motorista motorista = new Motorista(
                "1117540002",
                "Jhonatan Saavedra",
                "3209876543",
                "j.saavedra@udla.edu.co"
        );

        Administrador admin = new Administrador(
                "1117540003",
                "Dra. Maria Perez",
                "3155551234",
                "m.perez@udla.edu.co",
                RolAdmin.BIENESTAR_UNIVERSITARIO
        );

        // 3. Registro y Autenticación
        System.out.println("--- 1. Registro e Inicio de Sesión ---");
        authService.registrarse(estudiante, "claveEstudiante123");
        authService.registrarse(motorista, "claveMotorista456");
        authService.registrarse(admin, "claveAdmin789");

        boolean loginOk = authService.iniciarSesion("g.castaneda@udla.edu.co", "claveEstudiante123");
        System.out.println("Login estudiante: " + (loginOk ? "EXITOSO" : "FALLIDO"));

        // 4. Gestión de Motos del Motorista (Multi-moto)
        System.out.println("\n--- 2. Registro y Selección de Motos ---");
        Moto moto1 = new Moto("ABC-12D", "Yamaha", "FZ 2.0", 150, true, "SOAT-2026-999");
        Moto moto2 = new Moto("XYZ-89E", "Suzuki", "GN 125", 125, true, "SOAT-2026-888");

        motorista.registrarMoto(moto1);
        motorista.registrarMoto(moto2);
        System.out.println("Total motos registradas: " + motorista.getMotos().size());
        System.out.println("Moto activa inicial: " + motorista.obtenerMotoActiva().getPlaca());

        motorista.seleccionarMotoActiva("XYZ-89E");
        System.out.println("Moto activa seleccionada: " + motorista.obtenerMotoActiva().getPlaca());
        System.out.println("Documentos válidos: " + motorista.verificarDocumentos());

        // 5. Solicitud y Ciclo de Vida del Viaje
        System.out.println("\n--- 3. Solicitud y Ejecución de Viaje ---");
        estudiante.solicitarViaje("Campus Porvenir", "Barrio Juan XXIII", 4500.0);
        Viaje viaje = estudiante.getViajesSolicitados().get(0);
        System.out.println("Viaje creado: " + viaje.getCodigoViaje() + " | Estado: " + viaje.getEstado());

        // Motorista acepta viaje
        boolean aceptado = motorista.aceptarViaje(viaje);
        System.out.println("Motorista acepta viaje: " + (aceptado ? "SI" : "NO") + " | Estado: " + viaje.getEstado());

        // Inicia el viaje
        viaje.iniciarViaje();
        System.out.println("Viaje en ruta -> Estado: " + viaje.getEstado());

        // Estudiante confirma llegada segura
        estudiante.marcarLlegadaSegura(viaje.getCodigoViaje());
        System.out.println("Llegada confirmada -> Estado: " + viaje.getEstado());

        // 6. Proceso de Pago
        System.out.println("\n--- 4. Pago y Confirmación ---");
        viaje.getPago().reportarPagoEstudiante(MetodoPago.NEQUI);
        System.out.println("Estudiante reportó pago Nequi -> Estado pago: " + viaje.getPago().getEstado());

        viaje.getPago().confirmarRecepcionMotorista();
        System.out.println("Motorista confirmó recepción -> Estado pago: " + viaje.getPago().getEstado());

        // 7. Calificaciones Mutuas (Rango 1.0 - 5.0)
        System.out.println("\n--- 5. Calificaciones Mutuas ---");
        Calificacion calEstudiante = new Calificacion(4.8, "Viaje seguro y moto en excelente estado", "ESTUDIANTE");
        Calificacion calMotorista = new Calificacion(5.0, "Estudiante puntual y amable", "MOTORISTA");

        viaje.agregarCalificacion(calEstudiante);
        viaje.agregarCalificacion(calMotorista);

        System.out.println("Calificaciones registradas: " + viaje.getCalificaciones().size());
        for (Calificacion c : viaje.getCalificaciones()) {
            System.out.println(" - [" + c.getRolEmisor() + "] " + c.getPuntaje() + " pts: " + c.getComentario());
        }

        // 8. Servicios de Reportes y Auditoría
        System.out.println("\n--- 6. Reportes y Gestión Universitaria ---");
        reportesService.registrarViaje(viaje);

        // Viaje adicional para enriquecer reportes
        estudiante.solicitarViaje("Campus Porvenir", "Barrio Centro", 3500.0);
        Viaje viaje2 = estudiante.getViajesSolicitados().get(1);
        viaje2.iniciarViaje();
        viaje2.finalizarViaje();
        reportesService.registrarViaje(viaje2);

        System.out.println("Destinos más frecuentes: " + reportesService.obtenerDestinosMasFrecuentes(5));
        System.out.println("Porcentaje viajes hacia Porvenir/Centro: " + reportesService.calcularPorcentajePorSector("Porvenir") + "%");

        admin.consultarReportesDemanda();
        admin.gestionarUsuarios();

        System.out.println("\n===============================================================");
        System.out.println("       ✓ COMPILACIÓN Y FLUJO V5.0 EJECUTADOS EXITOSAMENTE     ");
        System.out.println("===============================================================");
    }
}
