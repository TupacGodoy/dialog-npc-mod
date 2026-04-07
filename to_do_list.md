# To-Do List

## Implementado
- [x] Comandos básicos de NPC (create, settitle, settext, addoption, remove, info)
- [x] Personalización de colores del diálogo
- [x] Personalización de altura de opciones
- [x] Soporte para texturas personalizadas (URL, base64, player skin)
- [x] Comportamiento de NPC (headTracking, bodyRotation, canMove, canRotate)
- [x] Comandos para configurar comportamiento (setheadtracking, setbodyrotation, setcanmove, setcanrotate)
- [x] Comandos para texturas personalizadas (settexturetype, setcustomtexture)
- [x] Soporte para sonidos en opciones de diálogo
- [x] Soporte para partículas en opciones de diálogo
- [x] Comando addoption mejorado con sonido y partículas
- [x] Sistema de traducciones para textos, títulos, nombres y opciones

## Pendiente

## Optimizaciones Realizadas (2026-04-07)
- [x] Crear clase utilitaria `TextureLoader` para cargar texturas de forma centralizada
- [x] Eliminar logging excesivo en `DialogNpcRenderer` (System.out.println → LOGGER)
- [x] Consolidar lógica duplicada de carga de texturas entre `DialogScreen` y `DialogNpcRenderer`
- [x] Simplificar validación de sonidos y partículas en `DialogCommand` con métodos helper
- [x] Eliminar variables innecesarias en `DialogScreen`
- [x] Reducir código boilerplate en `DialogNpcRenderer` (de ~230 líneas a ~40)
