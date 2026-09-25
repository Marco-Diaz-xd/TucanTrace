package tucantrace.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lanza el programa Java objetivo (el código del estudiante) en una JVM
 * separada, habilitando JDWP para que TucanTrace pueda observarlo, y
 * redirige su salida línea por línea.
 */
public class TargetLauncher {

    private final Process proceso;
    private final Thread hiloSalida;

    /**
     * @param classpath classpath del programa objetivo
     * @param mainClass clase principal a ejecutar
     * @param jdiPort   puerto JDWP a abrir
     * @param onLine    callback por cada línea de salida
     */
    public TargetLauncher(String classpath, String mainClass, int jdiPort, Consumer<String> onLine)
            throws IOException {
        String javaExe = System.getProperty("java.home") + "/bin/java";

        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:" + jdiPort);
        cmd.add("-Dfile.encoding=UTF-8");
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(mainClass);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        // El programa objetivo hereda la ENTRADA de esta consola, para que el
        // usuario pueda escribir (prototipo interactivo) mientras su salida se
        // captura y se transmite al navegador.
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        this.proceso = pb.start();

        this.hiloSalida = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    // Ocultar el ruido del arranque del debugger
                    if (line.startsWith("Listening for transport")) {
                        continue;
                    }
                    onLine.accept(line);
                    // Reflejo directo en consola (el programa "se ve" en la terminal)
                    System.out.println(line);
                }
            } catch (IOException ignored) {
                // proceso terminado
            }
        }, "target-output");
        this.hiloSalida.setDaemon(true);
        this.hiloSalida.start();
    }

    public boolean estaVivo() {
        return proceso.isAlive();
    }

    public int esperar() throws InterruptedException {
        return proceso.waitFor();
    }

    public void detener() {
        proceso.destroy();
    }
}