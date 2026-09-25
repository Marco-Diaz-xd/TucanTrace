package co.edu.uniamazonia.logica2.servicio;

import co.edu.uniamazonia.logica2.modelo.Viaje;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicioReportes {

    private List<Viaje> historicoViajes;

    public ServicioReportes() {
        this.historicoViajes = new ArrayList<>();
    }

    public ServicioReportes(List<Viaje> historicoViajes) {
        this.historicoViajes = historicoViajes != null ? historicoViajes : new ArrayList<>();
    }

    public void registrarViaje(Viaje viaje) {
        if (viaje != null) {
            this.historicoViajes.add(viaje);
        }
    }

    public List<String> obtenerDestinosMasFrecuentes(int limite) {
        Map<String, Integer> conteo = new HashMap<>();
        for (Viaje v : historicoViajes) {
            String dest = v.getDestino();
            if (dest != null) {
                conteo.put(dest, conteo.getOrDefault(dest, 0) + 1);
            }
        }
        List<Map.Entry<String, Integer>> lista = new ArrayList<>(conteo.entrySet());
        lista.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<String> resultado = new ArrayList<>();
        for (int i = 0; i < Math.min(limite, lista.size()); i++) {
            resultado.add(lista.get(i).getKey() + " (" + lista.get(i).getValue() + " viajes)");
        }
        return resultado;
    }

    public List<Viaje> consultarViajesPorDestino(String destino) {
        List<Viaje> filtrados = new ArrayList<>();
        if (destino == null) {
            return filtrados;
        }
        for (Viaje v : historicoViajes) {
            if (v.getDestino() != null && v.getDestino().toLowerCase().contains(destino.toLowerCase())) {
                filtrados.add(v);
            }
        }
        return filtrados;
    }

    public double calcularPorcentajePorSector(String sector) {
        if (sector == null || historicoViajes.isEmpty()) {
            return 0.0;
        }
        long coincidencias = 0;
        for (Viaje v : historicoViajes) {
            if ((v.getDestino() != null && v.getDestino().toLowerCase().contains(sector.toLowerCase())) ||
                (v.getOrigen() != null && v.getOrigen().toLowerCase().contains(sector.toLowerCase()))) {
                coincidencias++;
            }
        }
        return ((double) coincidencias / historicoViajes.size()) * 100.0;
    }

    public List<Viaje> getHistoricoViajes() {
        return historicoViajes;
    }
}
