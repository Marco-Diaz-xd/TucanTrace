package co.edu.uniamazonia.logica2.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Calificacion {

    private double puntaje;
    private String comentario;
    private String rolEmisor;
    private String fechaHora;

    public Calificacion(double puntaje, String comentario, String rolEmisor) {
        validarPuntaje(puntaje);
        this.puntaje = puntaje;
        this.comentario = comentario;
        this.rolEmisor = rolEmisor;
        this.fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Calificacion(double puntaje, String comentario, String rolEmisor, String fechaHora) {
        validarPuntaje(puntaje);
        this.puntaje = puntaje;
        this.comentario = comentario;
        this.rolEmisor = rolEmisor;
        this.fechaHora = fechaHora;
    }

    private void validarPuntaje(double puntaje) {
        if (puntaje < 1.0 || puntaje > 5.0) {
            throw new IllegalArgumentException("El puntaje debe estar estrictamente en el rango [1.0, 5.0]");
        }
    }

    public void registrarCalificacion(double puntaje, String comentario, String emisor) {
        validarPuntaje(puntaje);
        this.puntaje = puntaje;
        this.comentario = comentario;
        this.rolEmisor = emisor;
        this.fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public double obtenerPuntaje() {
        return this.puntaje;
    }

    public double getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(double puntaje) {
        validarPuntaje(puntaje);
        this.puntaje = puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getRolEmisor() {
        return rolEmisor;
    }

    public void setRolEmisor(String rolEmisor) {
        this.rolEmisor = rolEmisor;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    @Override
    public String toString() {
        return "Calificacion{" +
                "puntaje=" + puntaje +
                ", comentario='" + comentario + '\'' +
                ", rolEmisor='" + rolEmisor + '\'' +
                ", fechaHora='" + fechaHora + '\'' +
                '}';
    }
}
