package co.edu.uniamazonia.logica2.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Pago {

    private double valor;
    private MetodoPago metodo;
    private EstadoPago estado;
    private boolean pagadoPorEstudiante;
    private boolean confirmadoPorMotorista;
    private String fechaHora;

    public Pago(double valor, MetodoPago metodo) {
        if (valor < 0) {
            throw new IllegalArgumentException("El valor del pago no puede ser negativo");
        }
        this.valor = valor;
        this.metodo = metodo;
        this.estado = EstadoPago.PENDIENTE;
        this.pagadoPorEstudiante = false;
        this.confirmadoPorMotorista = false;
        this.fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void reportarPagoEstudiante(MetodoPago metodo) {
        this.metodo = metodo;
        this.pagadoPorEstudiante = true;
        this.estado = EstadoPago.PAGADO_REPORTADO;
        this.fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public boolean confirmarRecepcionMotorista() {
        if (this.pagadoPorEstudiante || this.estado == EstadoPago.PAGADO_REPORTADO || this.estado == EstadoPago.PENDIENTE) {
            this.confirmadoPorMotorista = true;
            this.estado = EstadoPago.CONFIRMADO_RECIBIDO;
            this.fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return true;
        }
        return false;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("El valor del pago no puede ser negativo");
        }
        this.valor = valor;
    }

    public MetodoPago getMetodo() {
        return metodo;
    }

    public void setMetodo(MetodoPago metodo) {
        this.metodo = metodo;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public boolean isPagadoPorEstudiante() {
        return pagadoPorEstudiante;
    }

    public void setPagadoPorEstudiante(boolean pagadoPorEstudiante) {
        this.pagadoPorEstudiante = pagadoPorEstudiante;
    }

    public boolean isConfirmadoPorMotorista() {
        return confirmadoPorMotorista;
    }

    public void setConfirmadoPorMotorista(boolean confirmadoPorMotorista) {
        this.confirmadoPorMotorista = confirmadoPorMotorista;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    @Override
    public String toString() {
        return "Pago{" +
                "valor=$" + valor +
                ", metodo=" + metodo +
                ", estado=" + estado +
                ", pagadoPorEstudiante=" + pagadoPorEstudiante +
                ", confirmadoPorMotorista=" + confirmadoPorMotorista +
                ", fechaHora='" + fechaHora + '\'' +
                '}';
    }
}
