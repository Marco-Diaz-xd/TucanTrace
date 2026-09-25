package co.edu.uniamazonia.logica2.servicio;

import co.edu.uniamazonia.logica2.modelo.Persona;
import java.util.HashMap;
import java.util.Map;

public class ServicioAutenticacion {

    private Map<String, Persona> usuariosRegistrados;
    private Map<String, String> credenciales;
    private Map<String, Persona> sesionesActivas;

    public ServicioAutenticacion() {
        this.usuariosRegistrados = new HashMap<>();
        this.credenciales = new HashMap<>();
        this.sesionesActivas = new HashMap<>();
    }

    public boolean registrarse(Persona persona, String clave) {
        if (persona == null || clave == null || clave.isBlank()) {
            return false;
        }
        if (persona.getCorreoInstitucional() == null || persona.getCorreoInstitucional().isBlank()) {
            return false;
        }
        String correo = persona.getCorreoInstitucional().toLowerCase();
        if (credenciales.containsKey(correo)) {
            return false;
        }
        usuariosRegistrados.put(correo, persona);
        credenciales.put(correo, clave);
        return true;
    }

    public boolean iniciarSesion(String correo, String clave) {
        if (correo == null || clave == null) {
            return false;
        }
        String correoNormalizado = correo.toLowerCase();
        if (credenciales.containsKey(correoNormalizado) && credenciales.get(correoNormalizado).equals(clave)) {
            Persona usuario = usuariosRegistrados.get(correoNormalizado);
            sesionesActivas.put(usuario.getIdentificacion(), usuario);
            return true;
        }
        return false;
    }

    public void cerrarSesion(String identificacion) {
        if (identificacion != null) {
            sesionesActivas.remove(identificacion);
        }
    }

    public boolean estaAutenticado(String identificacion) {
        return identificacion != null && sesionesActivas.containsKey(identificacion);
    }
}
