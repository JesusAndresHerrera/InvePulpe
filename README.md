# InvePulpe

Aplicación Android en Kotlin para administrar el inventario de una pulpería.
Se implementó sobre el proyecto existente, conservando su paquete, Gradle Kotlin DSL y wrapper.

## Funciones

- Crear, consultar, buscar, editar y eliminar productos.
- Precio en lempiras, código de barra manual opcional y único, existencias y mínimo por producto.
- Registrar entradas y salidas con fecha automática; rechazar salidas sin stock suficiente.
- Consultar, corregir y eliminar movimientos, revirtiendo su efecto en las existencias.
- Mostrar alertas cuando `stock <= min_stock`, etiquetas en el listado y productos por reponer en el resumen.
- Calcular el valor total y los cinco productos con mayor `price_cents * stock`.
- Guardar los datos localmente en SQLite. No requiere servidor, cuenta ni acceso a Internet para funcionar.
- Interfaz XML en español con colores para modo claro y oscuro.

## Diseño Cuaderno

La presentación sigue la propuesta Cuaderno elegida: pestañas superiores subrayadas,
fondo cálido, títulos con serif y filas separadas por líneas. El listado alinea producto,
stock y precio, y muestra el valor total al pie. El filtro **Bajo stock** actúa sobre los
productos ya cargados en pantalla; no requiere consultas adicionales.

Se reutilizan las mismas Activities y adaptadores. No se añadieron clases de producción,
dependencias ni tablas. Los modelos, controladores, DAO, servicio y helper de SQLite
conservan su implementación anterior; solamente cambió la presentación y su estado visual.

## Uso

1. En **Productos**, pulsa **Nuevo producto** y completa los datos.
2. Si ingresas existencias iniciales, la aplicación crea su movimiento de entrada.
3. Toca un producto del listado para editar sus datos o eliminarlo.
4. En **Movimientos**, selecciona producto, tipo y cantidad y pulsa **Registrar movimiento**.
5. Toca un registro del historial para corregir el tipo o cantidad, o eliminarlo.
6. En **Resumen**, consulta valor total, top 5 y productos por reponer.

El stock no se modifica directamente al editar un producto: utiliza movimientos.
Para conservar el historial, no se permite eliminar un producto que tenga movimientos.
Las correcciones conservan el producto y la fecha original y no pueden producir saldos negativos
ni superar el límite de existencias en ningún punto del historial.

## Arquitectura MVC + DAO

```text
Vista XML / Activity → Controlador → Servicio de inventario → DAO → SQLite
```

El modelo de MVC comprende las entidades, las reglas y el acceso a datos. Las Activities
presentan las vistas y delegan las acciones. Los controladores no ejecutan SQL y los DAO
no muestran diálogos ni conocen las pantallas.

```text
com.example.invepulpe
├── MainActivity                         Entrada a la lista de productos
├── model
│   ├── Product
│   ├── Movement
│   ├── MovementType
│   ├── ProductValuation                 Resultado calculado; no es tabla
│   └── InventorySummary                 Resultado calculado; no es tabla
├── data
│   ├── database
│   │   ├── DatabaseContract             Tablas, columnas y versión
│   │   └── InventoryDatabaseHelper      Creación de SQLite y conexión compartida
│   └── dao
│       ├── ProductDao
│       ├── MovementDao
│       └── MovementTypeDao
├── service
│   ├── InventoryRules                   Validaciones puras, comprobables con JUnit
│   └── InventoryService                 Reglas de negocio y transacciones
├── controller
│   ├── ProductController
│   ├── MovementController
│   └── SummaryController
└── view
    ├── BaseInventoryActivity            Trabajo en segundo plano y navegación común
    ├── DisplayFormat                    Presentación de dinero y fechas
    ├── product
    │   ├── ProductListActivity
    │   ├── ProductFormActivity
    │   └── ProductAdapter
    ├── movement
    │   ├── MovementActivity
    │   └── MovementAdapter
    └── summary
        └── SummaryActivity
```

Hay **24 componentes Kotlin de producción**, contando clases y objetos. Son los 20
componentes propuestos más `MainActivity` para conservar la entrada del proyecto,
`InventoryRules`, `BaseInventoryActivity` y `DisplayFormat` para separar y reutilizar responsabilidades.
Los XML y las clases de prueba no forman parte de ese conteo.

## Base de datos

Archivo: `invepulpe.db`, creado automáticamente al primer acceso.
En Android Studio: **App Inspection → Database Inspector**, con la app ejecutándose.

| Tabla | Columnas |
|---|---|
| products | id PK, name, barcode UNIQUE opcional, price_cents, stock, min_stock |
| movement_types | id PK, name UNIQUE |
| movements | id PK, product_id FK, type_id FK, qty, date |

Relaciones: `products 1 → 0..N movements` y `movement_types 1 → 0..N movements`.
Los tipos se insertan al crear la base: `1 = ENTRADA`, `2 = SALIDA`.

Se activan claves foráneas, se utiliza `ON DELETE RESTRICT` y existe un índice por producto
y fecha para el historial. Las búsquedas por identificador y código emplean parámetros.
Se cierran los cursores con `use` y se comparte un helper con contexto de aplicación.

### Normalización hasta 5FN

- **1FN:** valores atómicos y filas identificadas por una clave primaria.
- **2FN:** los atributos dependen de la clave completa; las claves primarias son simples.
- **3FN y BCNF:** según las reglas del ejercicio, las dependencias funcionales no triviales
  tienen como determinante una clave candidata. No se repiten nombres del producto ni del
  tipo dentro de movimientos. El código de barra identifica un producto cuando está presente.
- **4FN:** no existen conjuntos de atributos multivaluados independientes dentro de una tabla.
- **5FN:** para estas reglas no se han identificado dependencias de reunión no triviales
  que requieran descomponer las tablas. Un movimiento es un evento con identidad propia;
  separar sus relaciones podría inventar combinaciones entre eventos.

Esta conclusión depende de las reglas declaradas; dibujar claves primarias y foráneas por sí
solo no demuestra 5FN. Al ampliar el negocio deben revisarse sus dependencias.

`stock` conserva el saldo actual solicitado por el enunciado. Es un dato agregado también
reconstruible desde movimientos. Esa redundancia se controla escribiendo saldo e historial
en la misma transacción; no se confunde normalización con ausencia absoluta de datos calculados.

## Integridad y decisiones

- Precio como entero en centavos (`Long` en Kotlin), sin cálculos monetarios con `Double`.
- Precio permitido entre L 0.00 y L 1,000,000.00; existencias, mínimo y cantidad hasta 1,000,000.
- Cantidades de movimiento estrictamente positivas y unidades enteras; no se contemplan ventas por peso fraccionario.
- Código como texto para conservar ceros iniciales; blanco se convierte a `NULL`.
- Fechas UTC ISO 8601 con milisegundos; se muestran en la zona horaria del dispositivo.
- Transacciones para alta con stock inicial, entradas, salidas, correcciones y eliminaciones.
- Las salidas simultáneas se serializan mediante las transacciones de SQLite.
- El top 5 se calcula a partir del precio actual y el stock actual; no representa ingresos por ventas.
- Cálculos de resumen sobre una consulta de productos: no hay consultas SQL en las Activities.
- Operaciones SQLite fuera del hilo principal con un ejecutor; los resultados visuales se publican en el hilo principal.
- El esquema inicial usa versión 1. Futuras versiones deben implementar migraciones explícitas sin borrar inventario.
- No se usan interfaces o capas adicionales sin una necesidad concreta. Clean Code orienta la separación de responsabilidades;
  no exige una cantidad fija de clases ni garantiza por sí solo una arquitectura correcta.

## Compilación y pruebas

Desde la raíz del proyecto en PowerShell, con el JDK de Android Studio:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest --no-configuration-cache
```

La segunda orden requiere un dispositivo o emulador conectado.
El APK de depuración queda en `app/build/outputs/apk/debug/app-debug.apk`.

Pruebas propias:

- `InventoryRulesTest`: centavos, límites, cantidades inválidas, alerta e historial.
- `InventoryDatabaseTest`: SQLite real en memoria, claves foráneas, códigos, CRUD, top 5,
  transacciones con fallos provocados y ventas concurrentes.
- `InventoryScreenTest`: creación desde formulario, salida, alerta y consulta del resumen.

Los tests de ejemplo originales se conservaron. Las pruebas de base de datos usan almacenamiento
aislado; la prueba visual elimina únicamente los registros que ella misma crea.

La comprobación en emulador no sustituye la revisión final en el teléfono que se utilizará para la entrega.

## Verificación realizada el 24 de octubre de 2026

- Compilación Android de depuración: correcta.
- Pruebas locales: 13 ejecutadas, 0 fallidas.
- Pruebas en emulador Android API 37: 18 ejecutadas, 0 fallidas.
- Análisis Lint del diseño Cuaderno: 0 errores y 52 advertencias no bloqueantes.
- Recorrido visual automatizado: crear producto, registrar salida, mostrar alerta y consultar resumen.
- Persistencia comprobada cerrando y abriendo una base de datos de prueba.
- Inicio de la aplicación comprobado en el emulador.
- No se ha probado en un teléfono físico.

El recorrido de pantalla también comprueba el filtro de stock bajo antes y después de
una salida. Se ejecutaron 13 pruebas locales y 18 pruebas instrumentadas con el diseño
Cuaderno. Las pruebas instrumentadas de esta revisión se dirigieron expresamente al
emulador mediante ADB. Las capturas contienen datos de ejemplo solamente en el emulador
temporal; esos datos no se incluyen como inventario inicial del APK.

Se conservó la configuración de Gradle del proyecto. Para las comprobaciones desde este entorno
se utilizó un directorio temporal dentro de la carpeta de trabajo, resolviendo un error del JDK
al establecer la conexión local de Gradle. No se modificaron ajustes globales de Windows.
