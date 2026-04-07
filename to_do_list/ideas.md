# Ideas de Implementacion y Optimizacion - Dialog NPC Mod

## Ideas de Mejoras

### Sistema de Dialogos Avanzado
- [ ] Soporte para dialogos de pagina multiple (navegacion con "Siguiente", "Anterior")
- [ ] Soporte para formato de texto (colores, negrita, italica usando &c&l etc.)
- [ ] Opcion para que las opciones cierren o mantengan abierto el dialogo
- [ ] Animaciones de entrada/salida para la pantalla de dialogo
- [ ] Sonidos personalizables al abrir/cerrar dialogo y al seleccionar opciones

### Sistema de Permisos
- [ ] Integracion con LuckPerms para permisos granulares (dialognpc.create, dialognpc.edit, etc.)
- [ ] Soporte para diferentes niveles de moderadores (solo editar ciertos NPCs)
- [ ] Proteccion de NPCs por region/worldguard (opcional)

### Mejoras en NPCs
- [ ] Diferentes tipos de modelos (no solo player, tambien villager, etc.)
- [ ] Animaciones para NPCs (idle, saludo al interactuar)
- [ ] NPCs que miran al jugador automaticamente
- [ ] Opcion para que NPCs sean invisibles (solo se ve la nametag)
- [ ] Tamaño escalable del NPC

### Sistema de Condiciones
- [ ] Opciones condicionales (ej: solo mostrar si tiene X permiso, o Y item)
- [ ] Sistema de variables en comandos (%player%, %world%, etc.)
- [ ] Cooldowns por opcion o por NPC global

### Herramientas de Moderador
- [ ] Comando `/npc list` para ver todos los NPCs y sus coordenadas
- [ ] Comando `/npc tp <id>` para teleportarse a un NPC
- [ ] Comando `/npc copy <npc origen> <npc destino>` para duplicar configuracion
- [ ] Edicion GUI con inventario virtual en vez de solo comandos
- [ ] Exportar/Importar configuracion de NPCs a JSON

### Persistencia y Datos
- [ ] Sistema de progreso por jugador (que dialogos ya vio, opciones seleccionadas)
- [ ] Base de datos opcional (SQLite) para datos complejos
- [ ] Sistema de quests/misiones simple ligado a NPCs

### Multi-idioma
- [x] Soporte completo de traduccion (lang files)
- [x] Posibilidad de tener dialogos en diferentes idiomas

### Optimizaciones
- [ ] Cache de texturas para evitar recargas
- [ ] Limitar numero de NPCs por chunk/configurable
- [ ] Optimizar paquetes de red (solo sincronizar cuando cambia)

## Completadas

- [x] Sistema base de dialogos con opciones
- [x] Comandos basicos para moderadores
- [x] Persistencia via NBT
- [x] Seguridad server-side para comandos
