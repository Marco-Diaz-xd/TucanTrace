package tucantrace.runtime;

import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Cliente JDI para conectarse a una JVM en ejecución y escuchar eventos
 * de ejecución en tiempo real.
 * <p>
 * Usa la API estándar JDI (com.sun.jdi) disponible en el JDK.
 * No requiere modificar el código del estudiante; solo requiere que la JVM
 * objetivo se inicie con:
 * <pre>
 * -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
 * </pre>
 * </p>
 */
public class JDIClient implements AutoCloseable {

    private final String host;
    private final int port;
    private VirtualMachine vm;
    private final BlockingQueue<JDIEvent> eventQueue = new LinkedBlockingQueue<>();

    public JDIClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Conecta a la JVM objetivo.
     */
    public void connect() throws Exception {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
        List<AttachingConnector> connectors = vmm.attachingConnectors();

        AttachingConnector connector = connectors.stream()
            .filter(c -> c.name().equals("com.sun.jdi.SocketAttach"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("SocketAttach connector no disponible"));

        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        Connector.Argument hostArg = arguments.get("hostname");
        Connector.Argument portArg = arguments.get("port");
        hostArg.setValue(this.host);
        portArg.setValue(String.valueOf(this.port));

        this.vm = connector.attach(arguments);
        System.out.println("✅ JDI conectado a " + host + ":" + port);

        // Solicitar eventos de interés
        EventRequestManager erm = vm.eventRequestManager();

        // MethodEntryEvent
        MethodEntryRequest mer = erm.createMethodEntryRequest();
        mer.addClassFilter("tucantrace.*"); // Filtrar solo paquetes de interés
        mer.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        mer.enable();

        // MethodExitEvent
        MethodExitRequest mexr = erm.createMethodExitRequest();
        mexr.addClassFilter("tucantrace.*");
        mexr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        mexr.enable();

        // FieldModificationEvent (escritura de atributos)
        ModificationWatchpointRequest mwr = erm.createModificationWatchpointRequest(
            vm.classesByName("tucantrace.*").get(0).fields().get(0)); // Ejemplo: hay que iterar
        mwr.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        mwr.enable();

        // ClassPrepareEvent (clases cargadas)
        ClassPrepareRequest cpr = erm.createClassPrepareRequest();
        cpr.addClassFilter("tucantrace.*");
        cpr.enable();

        // Iniciar hilo de procesamiento de eventos
        new Thread(this::eventLoop, "JDI-EventLoop").start();
    }

    private void eventLoop() {
        try {
            while (true) {
                EventSet eventSet = vm.eventQueue().remove();
                for (Event event : eventSet) {
                    eventQueue.offer(new JDIEvent(event));
                }
                eventSet.resume();
            }
        } catch (Exception e) {
            if (!vm.isTerminated()) {
                System.err.println("Error en event loop: " + e.getMessage());
            }
        }
    }

    /**
     * Obtiene la siguiente evento (bloqueante).
     */
    public JDIEvent nextEvent() throws InterruptedException {
        return eventQueue.take();
    }

    /**
     * Obtiene la VM para consultas directas.
     */
    public VirtualMachine getVM() {
        return vm;
    }

    @Override
    public void close() {
        if (vm != null) {
            vm.dispose();
        }
    }

    // DTO para encapsular eventos JDI
    public static class JDIEvent {
        private final Event event;
        public JDIEvent(Event event) { this.event = event; }
        public Event getEvent() { return event; }
        public boolean isMethodEntry() { return event instanceof MethodEntryEvent; }
        public boolean isMethodExit() { return event instanceof MethodExitEvent; }
        public boolean isFieldModification() { return event instanceof ModificationWatchpointEvent; }
        public boolean isClassPrepare() { return event instanceof ClassPrepareEvent; }
    }
}