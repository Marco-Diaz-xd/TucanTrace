package tucantrace.ui;

/**
 * Construye la página HTML del visor UML en vivo.
 * <p>
 * La página incrusta el SVG generado por PlantUML y se conecta al stream de
 * eventos (SSE) para resaltar clases, métodos y atributos mientras el código
 * se ejecuta.
 * </p>
 */
public class LiveDiagramPage {

    /** Placeholder reemplazado por el SVG en tiempo de ejecución. */
    private static final String PLACEHOLDER_SVG = "__TUCANTRACE_SVG__";

    /**
     * Construye el HTML completo con el SVG incrustado.
     */
    public static String build(String svg) {
        return TEMPLATE.replace(PLACEHOLDER_SVG, svg);
    }

    private static final String TEMPLATE = """
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="utf-8">
<title>TucanTrace - UML en vivo</title>
<style>
  * { box-sizing: border-box; }
  body {
    margin: 0;
    font-family: 'Segoe UI', system-ui, sans-serif;
    background: #1e1e1e;
    color: #e0e0e0;
    display: flex;
    flex-direction: column;
    height: 100vh;
  }
  header {
    background: #252526;
    border-bottom: 1px solid #3c3c3c;
    padding: 10px 16px;
    display: flex;
    align-items: center;
    gap: 12px;
  }
  header h1 { font-size: 16px; margin: 0; font-weight: 600; }
  header .sub { font-size: 12px; color: #888; }
  #dot { width: 10px; height: 10px; border-radius: 50%; background: #f44336; }
  #dot.on { background: #4caf50; }
  #stats { margin-left: auto; font-size: 12px; color: #aaa; display: flex; gap: 14px; }
  #stats b { color: #4fc3f7; }
  main { flex: 1; display: flex; min-height: 0; }
  #diagram {
    flex: 1;
    overflow: auto;
    background: #ffffff;
    padding: 12px;
  }
  #diagram svg { max-width: 100%; height: auto; }
  aside {
    width: 420px;
    border-left: 1px solid #3c3c3c;
    display: flex;
    flex-direction: column;
    background: #1e1e1e;
  }
  aside h2 { font-size: 12px; text-transform: uppercase; color: #888; margin: 0; padding: 10px 12px; border-bottom: 1px solid #3c3c3c; }
  #log { flex: 1; overflow: auto; padding: 8px 12px; font-family: Consolas, monospace; font-size: 12px; line-height: 1.5; }
  .row { white-space: nowrap; }
  .tag { display: inline-block; width: 62px; font-weight: 700; }
  .tag.CLASS { color: #9e9e9e; }
  .tag.ENTER { color: #4caf50; }
  .tag.EXIT  { color: #607d8b; }
  .tag.FIELD { color: #ffb300; }
  .txt { color: #cfcfcf; }
  /* --- Resaltado de elementos del SVG --- */
  .class-active { fill: #ffe0b2 !important; stroke: #e65100 !important; stroke-width: 2.5px !important; }
  .member-active { fill: #d32f2f !important; font-weight: bold !important; }
</style>
</head>
<body>
<header>
  <span id="dot"></span>
  <h1>TucanTrace</h1>
  <span class="sub">UML en vivo - Universidad de la Amazonia</span>
  <span id="stats">
    <span>CLASES <b id="c-class">0</b></span>
    <span>ENTER <b id="c-enter">0</b></span>
    <span>EXIT <b id="c-exit">0</b></span>
    <span>CAMPO <b id="c-field">0</b></span>
  </span>
</header>
<main>
  <div id="diagram">
__TUCANTRACE_SVG__
  </div>
  <aside>
    <h2>Eventos en vivo</h2>
    <div id="log"></div>
  </aside>
</main>
<script>
  const dot = document.getElementById('dot');
  const log = document.getElementById('log');
  const counters = { class: 0, enter: 0, exit: 0, field: 0 };

  // --- Conexion SSE ---
  const es = new EventSource('/events');
  es.onopen = function () { dot.classList.add('on'); };
  es.onerror = function () { dot.classList.remove('on'); };
  es.onmessage = function (m) {
    try { handle(JSON.parse(m.data)); } catch (e) { console.error(e); }
  };

  function flash(el, cls, ms) {
    if (!el) return;
    el.classList.add(cls);
    setTimeout(function () { el.classList.remove(cls); }, ms || 900);
  }

  function highlightClass(name) {
    if (!name) return;
    flash(document.getElementById(name), 'class-active', 1100);
  }

  function highlightMember(member) {
    if (!member) return;
    const texts = document.querySelectorAll('#diagram text');
    for (let i = 0; i < texts.length; i++) {
      if (texts[i].textContent.indexOf(member) >= 0) {
        flash(texts[i], 'member-active', 900);
      }
    }
  }

  function bump(k) {
    counters[k] = (counters[k] || 0) + 1;
    const el = document.getElementById('c-' + k);
    if (el) el.textContent = counters[k];
  }

  function logLine(tag, text) {
    const row = document.createElement('div');
    row.className = 'row';
    const t = document.createElement('span');
    t.className = 'tag ' + tag;
    t.textContent = tag;
    const x = document.createElement('span');
    x.className = 'txt';
    x.textContent = text || '';
    row.appendChild(t);
    row.appendChild(x);
    log.appendChild(row);
    while (log.childNodes.length > 300) log.removeChild(log.firstChild);
    log.scrollTop = log.scrollHeight;
  }

  function shortName(full) {
    if (!full) return '';
    const i = full.lastIndexOf('.');
    return i >= 0 ? full.substring(i + 1) : full;
  }

  function handle(ev) {
    const cls = ev.className || '';
    const member = ev.member || '';
    if (ev.kind === 'CLASS') {
      bump('class');
      highlightClass(cls);
      logLine('CLASS', shortName(cls) + ' cargada');
    } else if (ev.kind === 'ENTER') {
      bump('enter');
      highlightClass(cls);
      highlightMember(member + '(');
      logLine('ENTER', shortName(cls) + '.' + member + '()');
    } else if (ev.kind === 'EXIT') {
      bump('exit');
      logLine('EXIT', shortName(cls) + '.' + member + '()');
    } else if (ev.kind === 'FIELD') {
      bump('field');
      highlightClass(cls);
      highlightMember(member);
      logLine('FIELD', shortName(cls) + '.' + member + ' = ' + (ev.value || ''));
    }
  }
</script>
</body>
</html>
""";
}