package co.edu.uniamazonia.logica2.modelo;

import java.util.ArrayList;
import java.util.List;

public class Estudiante extends Persona {

    private String codigoEstudiantil;
    private List<Viaje> viajesSolicitados;

    public Estudiante(String identificacion, String nombre, String telefono, String correoInstitucional, String codigoEstudiantil) {
        super(identificacion, nombre, telefono, correoInstitucional);
        this.codigoEstudiantil = codigoEstudiantil;
        this.viajesSolicitados = new ArrayList<>();
    }

    public boolean solicitarViaje(String origen, String destino) {
        if (origen == null || origen.isBlank() || destino == null || destino.isBlank()) {
            return false;
        }
        String codigoV = "VIA-" + (this.viajesSolicitados.size() + 1);
        Viaje nuevoViaje = new Viaje(codigoV, origen, destino, 0.0);
        this.viajesSolicitados.add(nuevoViaje);
        return true;
    }

    public boolean solicitarViaje(String origen, String destino, double tarifa) {
        if (origen == null || origen.isBlank() || destino == null || destino.isBlank() || tarifa < 0) {
            return false;
        }
        String codigoV = "VIA-" + (this.viajesSolicitados.size() + 1);
        Viaje nuevoViaje = new Viaje(codigoV, origen, destino, tarifa);
        this.viajesSolicitados.add(nuevoViaje);
        return true;
    }

    public boolean marcarLlegadaSegura(String codigoViaje) {
        if (codigoViaje == null || codigoViaje.isBlank()) {
            return false;
        }
        for (Viaje v : viajesSolicitados) {
            if (v.getCodigoViaje().equalsIgnoreCase(codigoViaje)) {
                v.finalizarViaje();
                return true;
            }
        }
        return false;
    }

    public String getCodigoEstudiantil() {
        return codigoEstudiantil;
    }

    public void setCodigoEstudiantil(String codigoEstudiantil) {
        this.codigoEstudiantil = codigoEstudiantil;
    }

    public List<Viaje> getViajesSolicitados() {
        return viajesSolicitados;
    }

    @Override
    public String toString() {
        return "Estudiante{" +
                "codigoEstudiantil='" + codigoEstudiantil + '\'' +
                ", identificacion='" + identificacion + '\'' +
                ", nombre='" + nombre + '\'' +
                ", telefono='" + telefono + '\'' +
                ", correoInstitucional='" + correoInstitucional + '\'' +
                ", totalViajes=" + viajesSolicitados.size() +
                '}';
    }
}
