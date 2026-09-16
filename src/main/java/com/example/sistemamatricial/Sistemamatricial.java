
package com.example.sistemamatricial;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/sistemamatricial")
public class Sistemamatricial {
    @PostMapping("/procesar")
    public Map<String, Object> procesarDistribucion(@RequestBody DatosEntrada datos) {
        Map<String, Object> respuesta = new HashMap<>();

        // Paso 1 & 2: Validación de datos (según diagrama de flujo)
        if (datos.getCentros() == null || datos.getMedicamentos() == null || 
            datos.getDisponibilidad() == null || datos.getRequerimientos() == null) {
            respuesta.put("estado", "NO");
            respuesta.put("mensaje", "Los datos son incorrectos o están incompletos.");
            return respuesta;
        }

        int numCentros = datos.getCentros().length;
        int numMedicamentos = datos.getMedicamentos().length;

        if (datos.getRequerimientos().length != numCentros || datos.getRequerimientos()[0].length != numMedicamentos) {
            respuesta.put("estado", "NO");
            respuesta.put("mensaje", "Las dimensiones de la matriz de requerimientos no coinciden con los centros y medicamentos.");
            return respuesta;
        }

        // Crear matrices de disponibilidad y requerimientos
        int[] disponibilidad = datos.getDisponibilidad();
        int[][] requerimientos = datos.getRequerimientos();
        int[][] asignacion = new int[numCentros][numMedicamentos];
        int[][] faltantes = new int[numCentros][numMedicamentos];

        // Calcular demanda total por medicamento para verificar suficiencia
        int[] demandaTotal = new int[numMedicamentos];
        for (int j = 0; j < numMedicamentos; j++) {
            for (int i = 0; i < numCentros; i++) {
                demandaTotal[j] += requerimientos[i][j];
            }
        }

        boolean suficienciaGlobal = true;
        for (int j = 0; j < numMedicamentos; j++) {
            if (disponibilidad[j] < demandaTotal[j]) {
                suficienciaGlobal = false;
                break;
            }
        }

        // Procesamiento matricial según disponibilidad vs requerimientos
        if (suficienciaGlobal) {
            // SÍ: Hay suficiente medicamento -> Asignar cantidades necesarias
            for (int i = 0; i < numCentros; i++) {
                for (int j = 0; j < numMedicamentos; j++) {
                    asignacion[i][j] = requerimientos[i][j];
                    faltantes[i][j] = 0;
                }
            }
            respuesta.put("condicion_disponibilidad", "SÍ: Hay suficiente medicamento para cubrir la demanda total.");
        } else {
            // NO: No hay suficiente -> Distribuir disponibles de forma proporcional/parcial y calcular faltantes
            for (int j = 0; j < numMedicamentos; j++) {
                int disponibleRestante = disponibilidad[j];
                for (int i = 0; i < numCentros; i++) {
                    if (disponibleRestante >= requerimientos[i][j]) {
                        asignacion[i][j] = requerimientos[i][j];
                        disponibleRestante -= requerimientos[i][j];
                    } else {
                        asignacion[i][j] = disponibleRestante;
                        disponibleRestante = 0;
                    }
                    faltantes[i][j] = requerimientos[i][j] - asignacion[i][j];
                }
            }
            respuesta.put("condicion_disponibilidad", "NO: Disponibilidad insuficiente. Se realizaron asignaciones parciales y se calcularon faltantes.");
        }

        // Preparar resultados finales
        respuesta.put("estado", "SÍ");
        respuesta.put("centros", datos.getCentros());
        respuesta.put("medicamentos", datos.getMedicamentos());
        respuesta.put("matrizAsignacion", asignacion);
        respuesta.put("matrizFaltantes", faltantes);

        return respuesta;
    }

    // Clase auxiliar para mapear el JSON de entrada
    public static class DatosEntrada {
        private String[] centros;
        private String[] medicamentos;
        private int[] disponibilidad;
        private int[][] requerimientos;

        public String[] getCentros() { return centros; }
        public void setCentros(String[] centros) { this.centros = centros; }

        public String[] getMedicamentos() { return medicamentos; }
        public void setMedicamentos(String[] medicamentos) { this.medicamentos = medicamentos; }

        public int[] getDisponibilidad() { return disponibilidad; }
        public void setDisponibilidad(int[] disponibilidad) { this.disponibilidad = disponibilidad; }

        public int[][] getRequerimientos() { return requerimientos; }
        public void setRequerimientos(int[][] requerimientos) { this.requerimientos = requerimientos; }
    }
}
