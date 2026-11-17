# Prompt de la Sesión

> "Actúa como un desarrollador Android con experiencia en Kotlin, Jetpack Compose y arquitectura MVVM + Repository. Soy estudiante y conozco lo básico; implementa los cambios paso a paso y explícame brevemente las decisiones importantes.
> Trabaja sobre el proyecto CampusGPS, en la rama `mejora-ia`. Primero revisa el código y respeta la estructura y los nombres que ya existen, especialmente `MapaViewModel`, `PantallaMapa`, `MapaUbicacion`, los repositorios y sus interfaces.
> El proyecto usa **MapLibre con OpenFreeMap** para el mapa. Conserva esa tecnología y el comportamiento actual de la ubicación. No reemplaces el mapa con Google Maps ni agregues una clave de API.
> Implementa estas tres mejoras de la Fase 2:
> 1. Filtrar por categoría
> 2. Recordar el último lugar visitado
> 3. Mapa en modo oscuro
> ..."

---

## Resumen de la Solución e Implementación

Se implementaron exitosamente las tres mejoras de la Fase 2 en la rama `mejora-ia` del proyecto `CampusGPS`, manteniendo estrictamente la arquitectura MVVM + Repository, respetando los nombres y componentes existentes, y utilizando MapLibre junto con OpenFreeMap.

---

## Cambios Realizados

1. **Dependencias (`gradle/libs.versions.toml` y `app/build.gradle.kts`)**:
   - Se añadió la dependencia de **Jetpack DataStore Preferences** (`androidx.datastore:datastore-preferences:1.3.0`) para almacenar de forma persistente las preferencias de usuario.

2. **Repositorio de Preferencias (`app/src/main/java/com/huanco/campusgps/data/PreferenciasRepository.kt`)**:
   - [NEW] Se creó la interfaz `PreferenciasRepository` y su implementación `PreferenciasRepositoryImpl` utilizando DataStore Preferences para guardar y leer el identificador del último lugar seleccionado (`ultimoLugarId`). El manejo de `Context` se mantiene aislado dentro del repositorio.

3. **ViewModel (`app/src/main/java/com/huanco/campusgps/viewmodel/MapaViewModel.kt`) y Factory**:
   - Se integró `PreferenciasRepository` en el ViewModel.
   - Se añadió el estado reactivo `categoriaSeleccionada` y la función `seleccionarCategoria(categoria: String?)`.
   - Se actualizó el flujo `lugares` para combinar los datos con el filtro de categoría activo, actualizando automáticamente la lista y los marcadores del mapa.
   - Se implementó la recuperación del último lugar seleccionado al iniciar la app (`init`), validando que el ID pertenezca a un lugar disponible para evitar fallos.
   - Se actualizó `CampusViewModelFactory` para inyectar el repositorio de preferencias.

4. **UI y Mapa (`app/src/main/java/com/huanco/campusgps/ui/PantallaMapa.kt` y `MapaUbicacion.kt`)**:
   - `PantallaMapa`: Se añadió una barra horizontal (`LazyRow`) con `FilterChip` para las categorías («Todas», «Estudio», «Comida», «Eventos», «Servicios»). Se detecta el tema del sistema (`isSystemInDarkTheme()`).
   - `MapaUbicacion`: Se configuró para alternar entre el estilo claro (`https://tiles.openfreemap.org/styles/liberty`) y oscuro (`https://tiles.openfreemap.org/styles/dark`) de OpenFreeMap según el tema actual, manejando errores de carga de estilo de forma robusta con un respaldo automático al estilo claro.

5. **Actividad (`app/src/main/java/com/huanco/campusgps/MainActivity.kt`)**:
   - Se instancia `PreferenciasRepositoryImpl(applicationContext)` y se pasa a `CampusViewModelFactory`.

---

## Decisiones Técnicas y Arquitecturales

- **MVVM + Repository**: Toda la lógica de filtrado de lugares, persistencia y control de estado permanece estrictamente en el `MapaViewModel` y los repositorios. Los Composables (`PantallaMapa`, `MapaUbicacion`) solo observan estados y comunican eventos del usuario.
- **Aislamiento de Context**: El `Context` requerido por DataStore se gestiona exclusivamente en `PreferenciasRepositoryImpl`, evitando pasar `Context` al `MapaViewModel`.
- **Compatibilidad con MapLibre y OpenFreeMap**: Se conservó MapLibre con los estilos vectoriales oficiales de OpenFreeMap (`liberty` para claro y `dark` para oscuro), sin requerir claves de API de Google Maps ni romper el funcionamiento de la ubicación y las distancias.
- **Robustez ante IDs Inválidos**: Si el ID recuperado de DataStore ya no existe (por cambios en la lista de lugares), la app inicia sin selección por defecto en lugar de colapsar.

## Revisión propia

En la primera versión, los bloques de lectura y escritura de preferencias capturaban `Exception` sin volver a lanzar `CancellationException`. Se corrigieron para relanzar la cancelación y mantener el comportamiento estructurado de las corrutinas; los errores ordinarios siguen registrándose en Logcat.

También ajusté el fallback del mapa oscuro: la primera versión dependía de que el mapa ya estuviera guardado en estado, por lo que no podía recuperar un fallo durante la carga inicial. Ahora solicita el mapa al `MapView` y aplica el estilo claro de respaldo. La lógica de categorías y persistencia permanece en ViewModel y Repository; la pantalla solo observa estado y envía eventos.

---

## Cómo Probar las Mejoras

1. **Filtrar por categoría**:
   - Toca cualquiera de los chips de categoría en la parte superior (Ej. "Comida", "Estudio").
   - Verifica que tanto la lista de lugares inferior como los marcadores en el mapa se actualicen instantáneamente para mostrar únicamente los lugares de esa categoría.
2. **Recordar el último lugar visitado**:
   - Selecciona un lugar de la lista o del mapa.
   - Cierra la aplicación por completo o fuerza su detención y vuelve a abrirla.
   - Comprueba que el lugar seleccionado previamente aparezca seleccionado automáticamente al iniciar.
3. **Mapa en modo oscuro**:
   - Cambia el tema de tu dispositivo Android (o emulador) entre Claro y Oscuro.
   - Abre la app y observa cómo el mapa de OpenFreeMap cambia dinámicamente al estilo oscuro o claro según corresponda.
