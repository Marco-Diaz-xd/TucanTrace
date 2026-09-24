import re

with open('/home/marco/TucanTrace/build/tucantrace-diagram.svg', 'r') as f:
    svg = f.read()

# Enhance SVG with highlight classes and css
css_injection = """
<style>
  .uml-class { transition: all 0.3s ease; cursor: pointer; }
  .highlight-active rect { fill: #f9e2af !important; stroke: #fab387 !important; stroke-width: 2.5px !important; filter: drop-shadow(0 0 10px #f9e2af); }
  .highlight-active text { fill: #11111b !important; font-weight: bold !important; }
</style>
"""
svg_with_style = svg.replace('<defs/>', '<defs/>' + css_injection)

# Add id="class_ClassName" to every entity group
svg_enhanced = re.sub(r'<g class="entity" data-qualified-name="([^"]+)" id="([^"]+)"', r'<g class="entity uml-class" data-qualified-name="\1" id="class_\1"', svg_with_style)

html_content = f"""<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>TucanTrace - Visor en Vivo de TucanGo</title>
  <style>
    body {{ background: #181825; color: #cdd6f4; font-family: 'Segoe UI', system-ui, sans-serif; margin: 0; padding: 20px; }}
    h1 {{ color: #89b4fa; margin-bottom: 5px; }}
    .subtitle {{ color: #a6adc8; margin-bottom: 20px; }}
    .container {{ display: flex; gap: 20px; }}
    .diagram-card {{ flex: 3; background: #1e1e2e; border: 1px solid #313244; border-radius: 12px; padding: 15px; overflow: auto; box-shadow: 0 4px 20px rgba(0,0,0,0.5); }}
    .sidebar {{ flex: 1; background: #1e1e2e; border: 1px solid #313244; border-radius: 12px; padding: 15px; }}
    .log-box {{ background: #11111b; border: 1px solid #45475a; border-radius: 8px; padding: 10px; height: 350px; overflow-y: auto; font-family: monospace; font-size: 12px; color: #a6e3a1; }}
    .btn {{ background: #89b4fa; color: #11111b; border: none; padding: 10px 15px; border-radius: 6px; font-weight: bold; cursor: pointer; margin-top: 10px; width: 100%; }}
    .btn:hover {{ background: #b4befe; }}
  </style>
</head>
<body>
  <h1>🔍 TucanTrace & TucanGo v5.0 — Visor UML en Vivo</h1>
  <div class="subtitle">Análisis Estático AST + Resaltado Reactivo SVG para Sustentación</div>
  <div class="container">
    <div class="diagram-card">
      <div id="svgContainer">{svg_enhanced}</div>
    </div>
    <div class="sidebar">
      <h3>⚡ Consola de Eventos JDI</h3>
      <div class="log-box" id="logBox">
        [TucanTrace Runtime Listener conectado en :5005]<br>
        [AST cargado]: 12 clases de TucanGo listas.<br>
      </div>
      <button class="btn" onclick="simularPaso()">Simular Solicitud de Viaje (JDI)</button>
      <button class="btn" style="background:#f38ba8; margin-top:5px;" onclick="limpiar()">Limpiar Resaltado</button>
    </div>
  </div>

  <script>
    const secuencia = [
      {{ target: 'class_Estudiante', msg: '-> Estudiante.solicitarViaje("Campus", "Centro")' }},
      {{ target: 'class_Viaje', msg: '-> new Viaje("V-102", origen, destino)' }},
      {{ target: 'class_Motorista', msg: '-> Motorista.aceptarViaje(viaje)' }},
      {{ target: 'class_Moto', msg: '-> Moto.validarSoat()' }},
      {{ target: 'class_Pago', msg: '-> Pago.reportarPagoEstudiante(NEQUI)' }},
      {{ target: 'class_Calificacion', msg: '-> Calificacion.registrarCalificacion(5.0, ...)' }}
    ];
    let paso = 0;

    function simularPaso() {{
      if (paso >= secuencia.length) paso = 0;
      const step = secuencia[paso];
      
      document.querySelectorAll('.highlight-active').forEach(el => el.classList.remove('highlight-active'));
      
      const el = document.getElementById(step.target);
      if (el) {{
        el.classList.add('highlight-active');
      }}
      
      const log = document.getElementById('logBox');
      log.innerHTML += `<span style="color:#f9e2af;">[JDI MethodEntry]</span> ${{step.msg}}<br>`;
      log.scrollTop = log.scrollHeight;
      paso++;
    }}

    function limpiar() {{
      document.querySelectorAll('.highlight-active').forEach(el => el.classList.remove('highlight-active'));
      document.getElementById('logBox').innerHTML += '<span style="color:#a6adc8;">[Visor] Resaltado reiniciado</span><br>';
      paso = 0;
    }}
  </script>
</body>
</html>
"""

with open('/home/marco/TucanTrace/demo_visual/tucango_live.html', 'w') as f:
    f.write(html_content)

print("Generated /home/marco/TucanTrace/demo_visual/tucango_live.html")
