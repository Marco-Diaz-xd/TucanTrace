package co.edu.uniamazonia.logica2.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Viaje {

    private String codigoViaje;
    private String origen;
    private String destino;
    private double tarifa;
    private EstadoViaje estado;
    private String fechaHora;
    private Pago pago;
    private List<Calificacion> calificaciones;

    public Viaje(String codigoViaje, String origen, String destino, double tarifa) {
        if (tarifa < 0) {
            throw new IllegalArgumentException("La tarifa no puede ser negativa");
        }
        this.codigoViaje = codigoViaje;
        this.origen = origen;
        this.destino = destino;
        this.tarifa = tarifa;
        this.estado = EstadoViaje.SOLICITADO;
        this.fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.pago = new Pago(tarifa, MetodoPago.EFECTIVO);
        this.calificaciones = new ArrayList<>();
    }

    public double calcularTarifa() {
        return this.tarifa;
    }

    public void iniciarViaje() {
        if (this.estado == EstadoViaje.SOLICITADO || this.estado == EstadoViaje.ACEPTADO) {
            this.estado = EstadoViaje.EN_CURSO;
        }
    }

    public void finalizarViaje() {
        if (this.estado == EstadoViaje.EN_CURSO || this.estado == EstadoViaje.ACEPTADO) {
            this.estado = EstadoViaje.FINALIZADO;
        }
    }

    public void cancelarViaje() {
        this.estado = EstadoViaje.CANCELADO;
    }

    public void agregarCalificacion(Calificacion calificacion) {
        if (calificacion != null && this.calificaciones.size() < 2) {
            this.calificaciones.add(calificacion);
        }
    }

    public String getCodigoViaje() {
        return codigoViaje;
    }

    public void setCodigoViaje(String codigoViaje) {
        this.codigoViaje = codigoViaje;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public double getTarifa() {
        return tarifa;
    }

    public void setTarifa(double tarifa) {
        if (tarifa < 0) {
            throw new IllegalArgumentException("La tarifa no puede ser negativa");
        }
        this.tarifa = tarifa;
        if (this.pago != null) {
            this.pago.setValor(tarifa);
        }
    }

    public EstadoViaje getEstado() {
        return estado;
    }

    public void setEstado(EstadoViaje estado) {
        this.estado = estado;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Pago getPago() {
        return pago;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }

    public List<Calificacion> getCalificaciones() {
        return calificaciones;
    }

    @Override
    public String toString() {
        return "Viaje{" +
                "codigoViaje='" + codigoViaje + '\'' +
                ", origen='" + origen + '\'' +
                ", destino='" + destino + '\'' +
                ", tarifa=$" + tarifa +
                ", estado=" + estado +
                ", fechaHora='" + fechaHora + '\'' +
                ", pago=" + pago +
                ", totalCalificaciones=" + calificaciones.size() +
                '}';
    }
}
