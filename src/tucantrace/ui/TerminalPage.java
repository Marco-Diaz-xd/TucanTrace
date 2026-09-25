package tucantrace.ui;

/**
 * Construye la página HTML de la terminal en vivo.
 * <p>
 * Muestra la salida estándar del programa que se está ejecutando (eventos
 * {@code kind:"OUT"} del stream SSE).
 * </p>
 */
public class TerminalPage {

    public static String build() {
        return TEMPLATE;
    }

    private static final String TEMPLATE = """
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>TucanTrace - Terminal en vivo</title>
<style>
  body {
    margin: 0;
    background: #0c0c0c;
    color: #cccccc;
    font-family: Consolas, 'Courier New', monospace;
    font-size: 14px;
    display: flex;
    flex-direction: column;
    height: 100vh;
  }
  header {
    background: #1a1a1a;
    border-bottom: 1px solid #333;
    padding: 8px 14px;
    display: flex;
    align-items: center;
    gap: 10px;
    font-family: 'Segoe UI', system-ui, sans-serif;
  }
  header h1 { font-size: 14px; margin: 0; font-weight: 600; color: #eee; }
  #dot { width: 10px; height: 10px; border-radius: 50%; background: #f44336; }
  #dot.on { background: #4caf50; }
  header .sub { font-size: 12px; color: #777; }
  #btn {
    margin-left: auto;
    background: #0e639c;
    color: #fff;
    border: none;
    padding: 7px 16px;
    border-radius: 4px;
    cursor: pointer;
    font-family: 'Segoe UI', system-ui, sans-serif;
    font-size: 13px;
    font-weight: 600;
  }
  #btn:hover { background: #1177bb; }
  #btn:disabled { background: #444; cursor: default; }
  #term {
    flex: 1;
    overflow: auto;
    padding: 12px 16px;
    white-space: pre-wrap;
    line-height: 1.45;
  }
  #term .cursor { color: #4caf50; }
  .meta { color: #569cd6; }
  .err { color: #f48771; }
</style>
</head>
<body>
<header>
  <span id="dot"></span>
  <h1>TucanTrace - Terminal (salida del programa)</h1>
  <span class="sub" id="estado">conectando...</span>
  <button id="btn">Ejecutar de nuevo</button>
</header>
<div id="term"></div>
<script>
  const dot = document.getElementById('dot');
  const term = document.getElementById('term');
  const estado = document.getElementById('estado');
  const btn = document.getElementById('btn');

  // Botón: re-ejecuta el programa
  btn.onclick = function () {
    term.textContent = '';
    btn.disabled = true;
    estado.textContent = 'ejecutando...';
    fetch('/run').then(function () {
      btn.disabled = false;
    }).catch(function () {
      btn.disabled = false;
    });
  };

  const es = new EventSource('/events');
  es.onopen = function () { dot.classList.add('on'); estado.textContent = 'en vivo'; };
  es.onerror = function () { dot.classList.remove('on'); estado.textContent = 'desconectado'; };
  es.onmessage = function (m) {
    let ev;
    try { ev = JSON.parse(m.data); } catch (e) { return; }
    if (ev.kind === 'OUT') {
      append(ev.text, '');
    } else if (ev.kind === 'META') {
      append(ev.text, 'meta');
    } else if (ev.kind === 'ERR') {
      append(ev.text, 'err');
    }
  };

  function append(text, cls) {
    const div = document.createElement('div');
    if (cls) div.className = cls;
    div.textContent = text;
    term.appendChild(div);
    term.scrollTop = term.scrollHeight;
  }
</script>
</body>
</html>
""";
}