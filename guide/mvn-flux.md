# mvn-flux CLI

`mvn-flux` es una herramienta de línea de comandos integrada en el ecosistema Jettra que facilita la generación de código y utilidades específicas para el framework **JettraFlux**. 

Mientras que `mvn-jettra` se enfoca en la administración y gestión del ciclo de vida de los plugins, `mvn-flux` está diseñado para agilizar el desarrollo de aplicaciones web escribiendo código repetitivo por ti.

Este script es autogenerado por JettraAppServer en la raíz de tu proyecto cuando inicias el servidor por primera vez, al igual que `mvn-jettra`.

## Comando: `-create-code`

El comando principal de `mvn-flux` es `-create-code`, que te permite generar clases `ViewModel` complejas de forma completamente automática a partir de tus entidades (`records`).

### Sintaxis

```bash
./mvn-flux -create-code -source-record <Paquete.Record> -model [-properties] [-converter] [-rest] [-services]
```

### Ejemplo de Uso

Supongamos que tienes una entidad (record) `Person` en el paquete `com.miempresa.proyecto.entity`:

```bash
./mvn-flux -create-code -source-record com.miempresa.proyecto.entity.Person -model -properties -converter -rest -services
```

Esto analizará tu archivo `Person.java` e implementará un `PersonModel.java` en el paquete `com.miempresa.proyecto.model`. Además, gracias al flag `-properties`, escaneará todos los archivos `messages*.properties` (multilenguaje) en tu carpeta `src/main/resources/` y añadirá automáticamente las etiquetas correspondientes a los atributos.

Por ejemplo, si `Person` tiene un `UUID id` y un `String name`, se añadirá automáticamente a tus archivos properties:
```properties
person.id = Id
person.name = Name
```

### ¿Qué hace internamente?

1. **Inferencia de Paquetes**: Asume de manera inteligente que si tu récord está en un subpaquete `.entity`, el ViewModel debe residir en `.model`. De igual manera, asume que los servicios residen en `.services`.
2. **Generación de Selectores Visuales**: 
   - Transforma atributos básicos (como `String`, `Integer`) en campos simples con sus respectivas anotaciones `@PropertiesInRecord`, `@PropertiesLabel` y validaciones (`@NotNull`).
   - Identifica atributos complejos (relaciones con otras clases) y genera selectores de vista única (`@ViewSelectOne`).
   - Identifica colecciones (ej. `List<Department>`) y genera selectores de vista múltiple (`@ViewSelectMany`) referenciando directamente a los servicios.
3. **Conversor Bidireccional (Opcional)**: Si añades el flag `-converter`, el CLI generará explícitamente la clase `PersonModelConversor.java` en el paquete `.converter`. Esto es útil para evitar errores de IDE al inyectar esta clase en otras. Si no pasas este flag, podrás usar anotaciones (como `@FluxModelToRecordConversor`) o crearlo manualmente si prefieres el modo clásico.
4. **Cliente REST (Opcional)**: Si añades el flag `-rest`, generará automáticamente una interfaz `@RestClient` (`PersonRestClient.java`) en el paquete `.restclient` para conectarse a tus APIs, con métodos de CRUD básicos (`findAll`, `save`, `update`, `delete`) y consultas dinámicas `findBy<NombreAtributo>` por cada campo del record.
5. **Servicio Lógico (Opcional)**: Si añades el flag `-services`, generará una clase de servicio (`PersonService.java`) en el paquete `.services` configurada con `@Inject` inyectando tu `PersonRestClient` lista para ser utilizada.

---

*Nota: Para que la generación de código funcione correctamente, asegúrate de invocar `mvn-flux` en la raíz del proyecto que contiene los archivos fuente de tu entidad, ya que el CLI utiliza el classpath y las rutas locales del proyecto para ubicar y escribir los archivos de Java.*
