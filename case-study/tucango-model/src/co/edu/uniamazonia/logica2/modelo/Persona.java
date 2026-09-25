package co.edu.uniamazonia.logica2.modelo;

public abstract class Persona {

    protected String identificacion;
    protected String nombre;
    protected String telefono;
    protected String correoInstitucional;

    public Persona(String identificacion, String nombre, String telefono, String correoInstitucional) {
        this.identificacion = identificacion;
        this.nombre = nombre;
        this.telefono = telefono;
        this.correoInstitucional = correoInstitucional;
    }

    public String obtenerIdentificacion() {
        return this.identificacion;
    }

    public String obtenerNombreCompleto() {
        return this.nombre;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreoInstitucional() {
        return correoInstitucional;
    }

    public void setCorreoInstitucional(String correoInstitucional) {
        this.correoInstitucional = correoInstitucional;
    }

    @Override
    public String toString() {
        return "Persona{" +
                "identificacion='" + identificacion + '\'' +
                ", nombre='" + nombre + '\'' +
                ", telefono='" + telefono + '\'' +
                ", correoInstitucional='" + correoInstitucional + '\'' +
                '}';
    }
}
