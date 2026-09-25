package co.edu.uniamazonia.logica2.modelo;

public class Moto {

    private String placa;
    private String marca;
    private String modelo;
    private int cilindraje;
    private boolean soatVigente;
    private String numeroSoat;
    private boolean activa;

    public Moto(String placa, String marca, String modelo, int cilindraje, boolean soatVigente, String numeroSoat) {
        if (cilindraje <= 0) {
            throw new IllegalArgumentException("El cilindraje debe ser mayor a 0");
        }
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.cilindraje = cilindraje;
        this.soatVigente = soatVigente;
        this.numeroSoat = numeroSoat;
        this.activa = false;
    }

    public Moto(String placa, String marca, String modelo, int cilindraje, boolean soatVigente, String numeroSoat, boolean activa) {
        if (cilindraje <= 0) {
            throw new IllegalArgumentException("El cilindraje debe ser mayor a 0");
        }
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.cilindraje = cilindraje;
        this.soatVigente = soatVigente;
        this.numeroSoat = numeroSoat;
        this.activa = activa;
    }

    public boolean validarSoat() {
        return this.soatVigente && this.numeroSoat != null && !this.numeroSoat.isBlank();
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public int getCilindraje() {
        return cilindraje;
    }

    public void setCilindraje(int cilindraje) {
        if (cilindraje <= 0) {
            throw new IllegalArgumentException("El cilindraje debe ser mayor a 0");
        }
        this.cilindraje = cilindraje;
    }

    public boolean isSoatVigente() {
        return soatVigente;
    }

    public void setSoatVigente(boolean soatVigente) {
        this.soatVigente = soatVigente;
    }

    public String getNumeroSoat() {
        return numeroSoat;
    }

    public void setNumeroSoat(String numeroSoat) {
        this.numeroSoat = numeroSoat;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return "Moto{" +
                "placa='" + placa + '\'' +
                ", marca='" + marca + '\'' +
                ", modelo='" + modelo + '\'' +
                ", cilindraje=" + cilindraje +
                ", soatVigente=" + soatVigente +
                ", numeroSoat='" + numeroSoat + '\'' +
                ", activa=" + activa +
                '}';
    }
}
