package tucantrace.ui;

import tucantrace.runtime.JDIClient;
import tucantrace.runtime.TargetLauncher;

/**
 * Controla las ejecuciones del programa objetivo para el visor en vivo.
 * <p>
 * Permite lanzar el mismo programa varias veces (botón "Ejecutar" del navegador),
 * reconectando JDI en cada corrida y transmitiendo los eventos y la salida.
 * </p>
 */
public class LiveController {

    private final LiveSession session;
    private final String host;
    private final int jdiPort;
    private final String rootPackage;
    private final String execMain;
    private final String execCp;

    private final Object lock = new Object();
    private volatile boolean ejecutando = false;

    public LiveController(LiveSession session, String host, int jdiPort, String rootPackage,
                          String execMain, String execCp) {
        this.session = session;
        this.host = host;
        this.jdiPort = jdiPort;
        this.rootPackage = rootPackage;
        this.execMain = execMain;
        this.execCp = execCp;
    }

    public boolean isEjecutando() {
        return ejecutando;
    }

    /** Lanza una ejecución en segundo plano (no bloquea). */
    public void ejecutarAsync() {
        synchronized (lock) {
            if (ejecutando) {
                session.publicarMeta("[i] Ya hay una ejecucion en curso...");
                return;
            }
            ejecutando = true;
        }
        Thread t = new Thread(this::ejecutarUnaVez, "tt-run");
        t.setDaemon(true);
        t.start();
    }

    private void ejecutarUnaVez() {
        TargetLauncher target = null;
        try {
            session.publicarMeta("");
            session.publicarMeta("========== NUEVA EJECUCION ==========");
            session.publicarMeta("$ java -cp " + execCp + " " + execMain);

            // 1. Lanzar el programa objetivo (suspendido, esperando al debugger)
            target = new TargetLauncher(execCp, execMain, jdiPort, session::publicarSalida);

            // 2. Conectar JDI (con reintentos: el puerto JDWP tarda en abrirse)
            try (JDIClient client = new JDIClient(host, jdiPort, rootPackage)) {
                conectarConReintentos(client);

                int count = 0;
                while (!client.isTerminated()) {
                    JDIClient.JDIEvent ev = client.pollEvent(400);
                    if (ev != null) {
                        session.publicar(ev);
                        count++;
                    }
                }
                JDIClient.JDIEvent resto;
                while ((resto = client.pollEventNow()) != null) {
                    session.publicar(resto, false);
                    count++;
                }
                session.publicarMeta("---------- fin: " + count + " eventos ----------");
            }

            target.esperar();

        } catch (Exception e) {
            session.publicarError("[X] " + e.getMessage());
        } finally {
            if (target != null) {
                target.detener();
            }
            synchronized (lock) {
                ejecutando = false;
            }
        }
    }

    /**
     * Intenta conectar a JDI reintentando hasta ~15s (el puerto JDWP del
     * programa recién lanzado tarda un momento en abrirse).
     */
    private void conectarConReintentos(JDIClient client) throws Exception {
        Exception ultimo = null;
        for (int i = 0; i < 50; i++) {
            try {
                client.connect();
                return;
            } catch (Exception e) {
                ultimo = e;
                Thread.sleep(300);
            }
        }
        throw ultimo != null ? ultimo : new IllegalStateException("No se pudo conectar a JDI");
    }
}