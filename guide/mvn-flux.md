# mvn-flux CLI

`mvn-flux` es una herramienta de línea de comandos integrada en el ecosistema Jettra que facilita la generación de código y utilidades específicas para el framework **JettraFlux**. 

Mientras que `mvn-jettra` se enfoca en la administración y gestión del ciclo de vida de los plugins, `mvn-flux` está diseñado para agilizar el desarrollo de aplicaciones web escribiendo código repetitivo por ti.

Este script es autogenerado por JettraAppServer en la raíz de tu proyecto cuando inicias el servidor por primera vez, al igual que `mvn-jettra`.

## Comando: `-create-code`

El comando principal de `mvn-flux` es `-create-code`, que te permite generar clases `ViewModel` complejas de forma completamente automática a partir de tus entidades (`records`).

### Sintaxis

**Por un Record específico:**
```bash
./mvn-flux -create-code -source-record <Paquete.Record> -model [-properties] [-converter] [-rest] [-services] [-page] [-page-crud] [-test-rest] [-test-service] [-test-page]
```

**Por todo un paquete de Records:**
```bash
./mvn-flux -create-code -source-package-record <Paquete> -model [-properties] [-converter] [-rest] [-services] [-page] [-page-crud] [-test-rest] [-test-service] [-test-page]
```

### Opciones de Origen (`-source-record` / `-source-package-record`)

- **`-source-record <Paquete.Record>`** (o `-from-record`): Recibe la ruta absoluta (Fully Qualified Name) de una clase `record` específica.
- **`-source-package-record <Paquete>`** (o `-from-package-record`): Recibe el paquete de trabajo (ej. `com.example.entity`). Escanea y toma todos los `records` presentes en ese paquete para aplicar masivamente la generación según los parámetros indicados.

### Ejemplo de Uso

**1. Generación por un Record individual:**
Supongamos que tienes una entidad (record) `Person` en el paquete `com.miempresa.proyecto.entity`:

```bash
./mvn-flux -create-code -source-record com.miempresa.proyecto.entity.Person -model -properties -converter -rest -services -page-crud -test-rest -test-service -test-page
```

**2. Generación masiva por Paquete de Records:**
Para procesar automáticamente todos los `records` dentro del paquete `com.miempresa.proyecto.entity`:

```bash
./mvn-flux -create-code -source-package-record com.miempresa.proyecto.entity -model -properties -converter -rest -services -page-crud -test-rest -test-service -test-page
```

Esto analizará las entidades del paquete e implementará sus correspondientes `ViewModel` (e.g. `PersonModel.java`) en el paquete `com.miempresa.proyecto.model`. Además, si se incluye `-properties`, escaneará todos los archivos `messages*.properties` (multilenguaje) en la carpeta `src/main/resources/` y añadirá automáticamente las etiquetas correspondientes a los atributos de cada récord.

Por ejemplo, si `Person` tiene un `UUID id` y un `String name`, se añadirá automáticamente a tus archivos properties:
```properties
person.id = Id
person.name = Name
```

### ¿Qué hace internamente?

1. **Inferencia de Paquetes**: Asume de manera inteligente que si tu récord está en un subpaquete `.entity`, el ViewModel debe residir en `.model`. De igual manera, asume que los servicios residen en `.services`, clientes REST en `.restclient`, páginas en `.pages`.
2. **Generación de Selectores Visuales**: 
   - Transforma atributos básicos (como `String`, `Integer`) en campos simples con sus respectivas anotaciones `@PropertiesInRecord`, `@PropertiesLabel` y validaciones (`@NotNull`).
   - Identifica atributos complejos (relaciones con otras clases) y genera selectores de vista única (`@ViewSelectOne`).
   - Identifica colecciones (ej. `List<Department>`) y genera selectores de vista múltiple (`@ViewSelectMany`) referenciando directamente a los servicios.
3. **Conversor Bidireccional (Opcional)**: Si añades el flag `-converter`, el CLI generará explícitamente la clase `PersonModelConverter.java` en el paquete `.converter`. Esto es útil para evitar errores de IDE al inyectar esta clase en otras. Si no pasas este flag, podrás usar anotaciones (como `@FluxModelToRecordConversor`) o crearlo manualmente si prefieres el modo clásico.
4. **Cliente REST (Opcional)**: Si añades el flag `-rest`, generará automáticamente una interfaz `@RestClient` (`PersonRestClient.java`) en el paquete `.restclient` para conectarse a tus APIs, con métodos de CRUD básicos (`findAll`, `save`, `update`, `delete`) y consultas dinámicas `findBy<NombreAtributo>` por cada campo del record.
5. **Servicio Lógico (Opcional)**: Si añades el flag `-services`, generará una clase de servicio (`PersonService.java`) en el paquete `.services` configurada con `@Inject` inyectando tu `PersonRestClient` lista para ser utilizada.
6. **Vistas / Páginas (Opcional)**: Si añades el flag `-page`, generará una página en blanco adaptada al record. Si en cambio añades el flag `-page-crud`, se construirá un CRUD visual completo (`PersonCrudPage.java`) con DataTable, paginación, modales, etc.
7. **Pruebas Unitarias/Integración (Opcional)**: Con los flags `-test-rest`, `-test-service`, y `-test-page`, se generarán las estructuras de prueba en `src/test/java` para las correspondientes capas de tu aplicación.

## Comando: `-help`

Para consultar el menú de ayuda con la explicación detallada de todos los comandos, parámetros y ejemplos desde la consola, ejecuta:

```bash
./mvn-flux -help
```

*(También puedes utilizar `help`, `--help` o `-h`).*

---

*Nota: Para que la generación de código funcione correctamente, asegúrate de invocar `mvn-flux` en la raíz del proyecto que contiene los archivos fuente de tu entidad, ya que el CLI utiliza el classpath y las rutas locales del proyecto para ubicar y escribir los archivos de Java.*
