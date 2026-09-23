package tucantrace.agent;

import java.lang.instrument.Instrumentation;

/**
 * Agente Java para instrumentación en tiempo de carga (load-time).
 * <p>
 * Uso: <pre>-javaagent:TucanTraceAgent.jar</pre>
 * </p>
 * <p>
 * Permite transformar bytecode al cargar clases para emitir eventos
 * de entrada/salida de método y modificación de campo sin JDI.
 * </p>
 */
public class TraceAgent {

    /**
     * Punto de entrada del agente (premain).
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("🔧 TucanTrace Agent cargado: " + agentArgs);

        // Aquí se registraría un ClassFileTransformer para instrumentar
        // las clases del paquete del estudiante (ej. tucantrace.*, co.edu.uniamazonia.*)
        //
        // inst.addTransformer(new TraceTransformer(), true);
        //
        // El transformer usaría ASM o Byte Buddy para inyectar llamadas
        // a un EventBus central al entrar/salir de métodos y escribir campos.

        System.out.println("⚠️ Transformador no implementado aún (MVP usa JDI)");
    }

    /**
     * Punto de entrada para agente dinámico (attach en tiempo de ejecución).
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }
}