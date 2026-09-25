package co.edu.uniamazonia.logica2.modelo;

import java.util.ArrayList;
import java.util.List;

public class Motorista extends Persona {

    private boolean disponible;
    private List<Moto> motos;
    private List<Viaje> viajesAtendidos;

    public Motorista(String identificacion, String nombre, String telefono, String correoInstitucional) {
        super(identificacion, nombre, telefono, correoInstitucional);
        this.disponible = true;
        this.motos = new ArrayList<>();
        this.viajesAtendidos = new ArrayList<>();
    }

    public Motorista(String identificacion, String nombre, String telefono, String correoInstitucional, boolean disponible) {
        super(identificacion, nombre, telefono, correoInstitucional);
        this.disponible = disponible;
        this.motos = new ArrayList<>();
        this.viajesAtendidos = new ArrayList<>();
    }

    public void registrarMoto(Moto moto) {
        if (moto != null) {
            if (this.motos.isEmpty()) {
                moto.setActiva(true);
            }
            this.motos.add(moto);
        }
    }

    public boolean seleccionarMotoActiva(String placa) {
        if (placa == null || placa.isBlank()) {
            return false;
        }
        boolean encontrada = false;
        for (Moto m : this.motos) {
            if (m.getPlaca().equalsIgnoreCase(placa)) {
                encontrada = true;
                break;
            }
        }
        if (!encontrada) {
            return false;
        }
        for (Moto m : this.motos) {
            m.setActiva(m.getPlaca().equalsIgnoreCase(placa));
        }
        return true;
    }

    public Moto obtenerMotoActiva() {
        for (Moto m : this.motos) {
            if (m.isActiva()) {
                return m;
            }
        }
        return null;
    }

    public boolean aceptarViaje(String codigoViaje) {
        if (this.disponible && verificarDocumentos() && codigoViaje != null && !codigoViaje.isBlank()) {
            this.disponible = false;
            return true;
        }
        return false;
    }

    public boolean aceptarViaje(Viaje viaje) {
        if (viaje != null && this.disponible && verificarDocumentos()) {
            viaje.setEstado(EstadoViaje.ACEPTADO);
            this.viajesAtendidos.add(viaje);
            this.disponible = false;
            return true;
        }
        return false;
    }

    public void registrarDisponibilidad(boolean estado) {
        this.disponible = estado;
    }

    public boolean verificarDocumentos() {
        Moto motoActiva = obtenerMotoActiva();
        return motoActiva != null && motoActiva.validarSoat();
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public List<Moto> getMotos() {
        return motos;
    }

    public List<Viaje> getViajesAtendidos() {
        return viajesAtendidos;
    }

    @Override
    public String toString() {
        Moto activa = obtenerMotoActiva();
        return "Motorista{" +
                "disponible=" + disponible +
                ", totalMotos=" + motos.size() +
                ", motoActiva=" + (activa != null ? activa.getPlaca() : "Ninguna") +
                ", identificacion='" + identificacion + '\'' +
                ", nombre='" + nombre + '\'' +
                ", telefono='" + telefono + '\'' +
                ", correoInstitucional='" + correoInstitucional + '\'' +
                '}';
    }
}
