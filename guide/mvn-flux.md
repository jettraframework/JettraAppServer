# mvn-flux CLI

`mvn-flux` es una herramienta de línea de comandos integrada en el ecosistema Jettra que facilita la generación de código y utilidades específicas para el framework **JettraFlux**. 

Mientras que `mvn-jettra` se enfoca en la administración y gestión del ciclo de vida de los plugins, `mvn-flux` está diseñado para agilizar el desarrollo de aplicaciones web escribiendo código repetitivo por ti.

Este script es autogenerado por JettraAppServer en la raíz de tu proyecto cuando inicias el servidor por primera vez, al igual que `mvn-jettra`.

## Comando: `-create-code`

El comando principal de `mvn-flux` es `-create-code`, que te permite generar clases `ViewModel` complejas de forma completamente automática a partir de tus entidades (`records`).

### Sintaxis

```bash
./mvn-flux -create-code -source-record <Paquete.Record> -model [-properties]
```

### Ejemplo de Uso

Supongamos que tienes una entidad (record) `Person` en el paquete `com.miempresa.proyecto.entity`:

```bash
./mvn-flux -create-code -source-record com.miempresa.proyecto.entity.Person -model -properties
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
3. **Conversor Bidireccional**: Agrega la anotación `@FluxModelToRecordConversor(goal = Person.class)` para que tu ViewModel pueda transformarse de ida y vuelta a la entidad original sin esfuerzo durante el flujo web.

---

*Nota: Para que la generación de código funcione correctamente, asegúrate de invocar `mvn-flux` en la raíz del proyecto que contiene los archivos fuente de tu entidad, ya que el CLI utiliza el classpath y las rutas locales del proyecto para ubicar y escribir los archivos de Java.*
