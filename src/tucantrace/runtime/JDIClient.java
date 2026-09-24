package tucantrace.runtime;

import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final String classPattern;
    private VirtualMachine vm;
    private final BlockingQueue<JDIEvent> eventQueue = new LinkedBlockingQueue<>();
    private final List<JDIEventListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean running = false;

    /**
     * Interfaz de listener para eventos JDI.
     */
    @FunctionalInterface
    public interface JDIEventListener {
        void onEvent(JDIEvent event);
    }

    public JDIClient(String host, int port, String classPattern) {
        this.host = host;
        this.port = port;
        this.classPattern = classPattern != null ? classPattern : "*";
    }

    public JDIClient(String host, int port) {
        this(host, port, "*");
    }

    public void addListener(JDIEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(JDIEventListener listener) {
        listeners.remove(listener);
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

        // 1. MethodEntryEvent
        MethodEntryRequest mer = erm.createMethodEntryRequest();
        if (!"*".equals(classPattern)) {
            mer.addClassFilter(classPattern);
        }
        mer.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        mer.enable();

        // 2. MethodExitEvent
        MethodExitRequest mexr = erm.createMethodExitRequest();
        if (!"*".equals(classPattern)) {
            mexr.addClassFilter(classPattern);
        }
        mexr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        mexr.enable();

        // 3. ClassPrepareEvent (clases cargadas)
        ClassPrepareRequest cpr = erm.createClassPrepareRequest();
        if (!"*".equals(classPattern)) {
            cpr.addClassFilter(classPattern);
        }
        cpr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        cpr.enable();

        // 4. Watchpoints para campos de clases ya cargadas
        setupFieldModificationRequests(erm);

        // Iniciar hilo de procesamiento de eventos
        running = true;
        Thread eventThread = new Thread(this::eventLoop, "JDI-EventLoop");
        eventThread.setDaemon(true);
        eventThread.start();
    }

    private void setupFieldModificationRequests(EventRequestManager erm) {
        try {
            for (ReferenceType refType : vm.allClasses()) {
                if (matchesPattern(refType.name(), classPattern)) {
                    registerFieldWatchpoints(erm, refType);
                }
            }
        } catch (Exception e) {
            // Ignorar si no se pueden registrar campos inicialmente
        }
    }

    private void registerFieldWatchpoints(EventRequestManager erm, ReferenceType refType) {
        try {
            for (Field field : refType.fields()) {
                ModificationWatchpointRequest mwr = erm.createModificationWatchpointRequest(field);
                mwr.setSuspendPolicy(EventRequest.SUSPEND_NONE);
                mwr.enable();
            }
        } catch (Exception ignored) {
        }
    }

    private boolean matchesPattern(String className, String pattern) {
        if ("*".equals(pattern)) return true;
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return className.startsWith(prefix);
        }
        return className.equals(pattern);
    }

    private void eventLoop() {
        try {
            while (running) {
                EventSet eventSet = vm.eventQueue().remove();
                for (Event event : eventSet) {
                    // Si se prepara una nueva clase, registrar automáticamente watchpoints de sus atributos
                    if (event instanceof ClassPrepareEvent cpe) {
                        ReferenceType refType = cpe.referenceType();
                        if (matchesPattern(refType.name(), classPattern)) {
                            registerFieldWatchpoints(vm.eventRequestManager(), refType);
                        }
                    }

                    JDIEvent jdiEvent = new JDIEvent(event);
                    eventQueue.offer(jdiEvent);

                    // Notificar a listeners registrados
                    for (JDIEventListener l : listeners) {
                        try {
                            l.onEvent(jdiEvent);
                        } catch (Exception ex) {
                            System.err.println("Error en JDI listener: " + ex.getMessage());
                        }
                    }
                }
                eventSet.resume();
            }
        } catch (VMDisconnectedException e) {
            System.out.println("ℹ️ VM objetivo desconectada.");
        } catch (Exception e) {
            if (running) {
                System.err.println("Error en event loop: " + e.getMessage());
            }
        }
    }

    /**
     * Obtiene el siguiente evento (bloqueante).
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
        running = false;
        if (vm != null) {
            try {
                vm.dispose();
            } catch (Exception ignored) {}
        }
    }

    /**
     * DTO para encapsular eventos JDI y proveer acceso amigable a sus detalles.
     */
    public static class JDIEvent {
        private final Event event;

        public JDIEvent(Event event) {
            this.event = event;
        }

        public Event getEvent() {
            return event;
        }

        public boolean isMethodEntry() {
            return event instanceof MethodEntryEvent;
        }

        public boolean isMethodExit() {
            return event instanceof MethodExitEvent;
        }

        public boolean isFieldModification() {
            return event instanceof ModificationWatchpointEvent;
        }

        public boolean isClassPrepare() {
            return event instanceof ClassPrepareEvent;
        }

        public String getClassName() {
            if (event instanceof MethodEntryEvent mee) {
                return mee.location().declaringType().name();
            } else if (event instanceof MethodExitEvent mex) {
                return mex.location().declaringType().name();
            } else if (event instanceof ModificationWatchpointEvent mwe) {
                return mwe.field().declaringType().name();
            } else if (event instanceof ClassPrepareEvent cpe) {
                return cpe.referenceType().name();
            }
            return "";
        }

        public String getSimpleClassName() {
            String full = getClassName();
            int idx = full.lastIndexOf('.');
            return idx != -1 ? full.substring(idx + 1) : full;
        }

        public String getMethodName() {
            if (event instanceof MethodEntryEvent mee) {
                return mee.method().name();
            } else if (event instanceof MethodExitEvent mex) {
                return mex.method().name();
            }
            return "";
        }

        public String getFieldName() {
            if (event instanceof ModificationWatchpointEvent mwe) {
                return mwe.field().name();
            }
            return "";
        }

        public String getValueToBe() {
            if (event instanceof ModificationWatchpointEvent mwe) {
                Value val = mwe.valueToBe();
                return val != null ? val.toString() : "null";
            }
            return "";
        }

        @Override
        public String toString() {
            if (isMethodEntry()) {
                return "MethodEntry: " + getSimpleClassName() + "." + getMethodName() + "()";
            } else if (isMethodExit()) {
                return "MethodExit: " + getSimpleClassName() + "." + getMethodName() + "()";
            } else if (isFieldModification()) {
                return "FieldModification: " + getSimpleClassName() + "." + getFieldName() + " = " + getValueToBe();
            } else if (isClassPrepare()) {
                return "ClassPrepare: " + getSimpleClassName();
            }
            return "JDIEvent: " + event.getClass().getSimpleName();
        }
    }
}