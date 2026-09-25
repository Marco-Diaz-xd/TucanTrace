package co.edu.uniamazonia.logica2.modelo;

public class Administrador extends Persona {

    private RolAdmin rol;

    public Administrador(String identificacion, String nombre, String telefono, String correoInstitucional, RolAdmin rol) {
        super(identificacion, nombre, telefono, correoInstitucional);
        this.rol = rol;
    }

    public void consultarReportesDemanda() {
        System.out.println("Consultando reportes de demanda del campus para rol: " + rol);
    }

    public void gestionarUsuarios() {
        System.out.println("Gestionando usuarios universitarios para rol: " + rol);
    }

    public RolAdmin getRol() {
        return rol;
    }

    public void setRol(RolAdmin rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "Administrador{" +
                "rol=" + rol +
                ", identificacion='" + identificacion + '\'' +
                ", nombre='" + nombre + '\'' +
                ", telefono='" + telefono + '\'' +
                ", correoInstitucional='" + correoInstitucional + '\'' +
                '}';
    }
}
