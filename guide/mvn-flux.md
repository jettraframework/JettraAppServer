# mvn-flux CLI

`mvn-flux` es una herramienta de línea de comandos integrada en el ecosistema Jettra que facilita la generación de código y utilidades específicas para el framework **JettraFlux**. 

Mientras que `mvn-jettra` se enfoca en la administración y gestión del ciclo de vida de los plugins, `mvn-flux` está diseñado para agilizar el desarrollo de aplicaciones web escribiendo código repetitivo por ti.

Este script es autogenerado por JettraAppServer en la raíz de tu proyecto cuando inicias el servidor por primera vez, al igual que `mvn-jettra`.

## Comando: `-initialize-front-end`

El comando `-initialize-front-end` permite inicializar automáticamente la estructura frontend de un proyecto nuevo generado con Maven Archetype (por ejemplo, `maven-archetype-quickstart`).

### Flujo de Uso

1. Crear un proyecto nuevo con Maven:
```bash
mvn archetype:generate \
    -DgroupId=com.example.web \
    -DartifactId=MiExample \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false
```

2. Añadir la dependencia de `JettraAppServer` en el `pom.xml`.

3. Ejecutar el comando de inicialización:
```bash
./mvn-flux -initialize-front-end
```

### ¿Qué realiza este comando?

- **Configuración de `pom.xml`**: Actualiza el archivo `pom.xml` configurando las propiedades Java 25, dependencias de Jettra (`JettraAppServer`, `JettraFlux`, `JettraJSON`, `JettraRules`, `JettraJWT`, `JettraRest`, `JettraAnnotation`, `JettraTest`), plugins de compilación/shade y el repositorio `jitpack.io`.
- **Generación de propiedades**: Crea en `src/main/resources/` los archivos:
  - `jettra-config.properties` tomando la información de `<groupId>`, `<artifactId>`, `<version>`, `<name>` del `pom.xml`.
  - `messages.properties`, `messages_es.properties` y `messages_en.properties`.
- **Clase Principal (`App.java`)**: Crea en el paquete principal la clase `App.java` con toda la configuración del servidor web empotrado, OpenAPI y enrutamiento.
- **Estructura de Paquetes y Clases Autogeneradas**:
  - `login/`: Genera `LoginPage.java` y `ForgotPasswordPage.java`.
  - `template/`: Genera `TemplatePage.java`.
  - `dashboard/`: Genera `DashboardPage.java`.
  - `entity/`: Genera `Person.java`.
  - `model/`: Genera `PersonModel.java`.
  - `page/`: Genera `PersonPage.java`.

## Comando: `-create-code`

El comando principal de `mvn-flux` es `-create-code`, que permite la generación automática de múltiples capas de la arquitectura (Modelos, Servicios, Controladores, Repositorios, Vistas y Pruebas) a partir de tus entidades (`records`). Esto acelera significativamente el desarrollo al reducir el código repetitivo.

### Sintaxis y Opciones

Puedes generar el código para un record específico o para un paquete completo:

**Por un Record específico:**
```bash
./mvn-flux -create-code -source-record <Paquete.Record> -model [Opciones]
```

**Por todo un paquete de Records:**
```bash
./mvn-flux -create-code -source-package-record <Paquete> -model [Opciones]
```

#### Parámetros Principales
- **`-source-record <FQN>`** (o `-from-record`): Ruta absoluta de un `record` (ej. `com.example.entity.Person`).
- **`-source-package-record <Paquete>`** (o `-from-package-record`): Paquete que contiene múltiples `records` para generación masiva (ej. `com.example.entity`).
- **`-model`**: **[Requerido]** Genera la clase `ViewModel` base para la entidad.

#### Opciones de Generación (Flags)
Añade los siguientes flags al comando para generar las capas adicionales que necesites:

- **`-properties`**: Actualiza los archivos de propiedades (ej. `messages_es.properties`) con las etiquetas de los atributos del record.
- **`-converter`**: Genera la clase conversora bidireccional entre el `Record` y su `ViewModel`.
- **`-repository`**: Genera la capa de acceso a datos (Interfaz e Implementación del repositorio).
- **`-services`**: Crea la clase de servicio lógico con la inyección de dependencias necesaria.
- **`-rest`**: Construye la interfaz cliente REST para la comunicación con APIs externas.
- **`-controller`**: Genera el controlador REST para exponer la entidad como un endpoint.
- **`-page`**: Crea una página UI en blanco conectada al ViewModel.
- **`-page-crud`**: Genera una página UI completa con funcionalidad CRUD (Tablas, Paginación, Formularios, Modales).
- **`-test-rest` / `-test-service` / `-test-page`**: Genera las estructuras base para pruebas unitarias y de integración de cada capa respectiva.

### Ejemplos de Uso

**1. Generación Full-Stack para una Entidad:**
Generar todas las capas (Modelo, Repositorio, Controlador, Servicio, CRUD y Pruebas) para la entidad `Person`:

```bash
./mvn-flux -create-code -source-record com.miempresa.proyecto.entity.Person -model -properties -converter -repository -controller -services -rest -page-crud -test-rest -test-service -test-page
```

**2. Generación Masiva para un Paquete:**
Procesar todos los `records` del paquete `entity` de una sola vez:

```bash
./mvn-flux -create-code -source-package-record com.miempresa.proyecto.entity -model -properties -converter -repository -controller -services -rest -page-crud
```

### ¿Qué hace internamente?

1. **Inferencia Inteligente de Paquetes**: Organiza automáticamente el código generado en subpaquetes correspondientes (`.model`, `.services`, `.restclient`, `.pages`, `.repository`, `.controller`) basándose en la ubicación original del record.
2. **Componentes Visuales Dinámicos**: 
   - Mapea atributos simples a campos de texto.
   - Detecta relaciones complejas y colecciones, generando selectores avanzados (`@ViewSelectOne`, `@ViewSelectMany`).
3. **Gestión de Etiquetas Automática**: Con `-properties`, extrae los nombres de las variables y alimenta los archivos de internacionalización (i18n) para la UI.
4. **Integración Completa**: Inyecta y conecta las distintas capas (ej. inyecta el `RestClient` o `Repository` en el `Service`, y el `Service` en el `Page` o `Controller`) para que el código generado sea funcional casi de inmediato.

## Comando: `-help`

Para consultar el menú de ayuda con la explicación detallada de todos los comandos, parámetros y ejemplos desde la consola, ejecuta:

```bash
./mvn-flux -help
```

*(También puedes utilizar `help`, `--help` o `-h`).*

---

*Nota: Para que la generación de código funcione correctamente, asegúrate de invocar `mvn-flux` en la raíz del proyecto que contiene los archivos fuente de tu entidad, ya que el CLI utiliza el classpath y las rutas locales del proyecto para ubicar y escribir los archivos de Java.*


El comando `mvn-flux` es la herramienta de línea de comandos integrada en el ecosistema Jettra (ejecutada mediante `io.jettra.server.cli.FluxCLI`). Permite automatizar la creación de código, inicialización de estructuras front-end y, más recientemente, la generación de plugins de temas.

## Nuevo Comando: `-generate-theme-project`

Para facilitar la creación de temas dinámicos que JettraFlux detectará automáticamente a través de la arquitectura de plugins (`theme.json`), puedes utilizar el comando `-generate-theme-project`.

### Sintaxis

```bash
./mvn-flux -generate-theme-project <nombre-proyecto-plugin> -path <path-donde-se-creara el proyecto>  -url-source <url-template-example>
```

### Parámetros

- `<nombre-proyecto-plugin>`: El nombre de tu nuevo proyecto (ej. `SkyRed`). Esto creará una carpeta con el mismo nombre en tu espacio de trabajo.
- `-url-source`: (Opcional) Una URL de referencia que sirvió de inspiración para el diseño (ej. `https://primeui.store/templates/angular/freya`).

### Ejemplo de Uso

```bash
./mvn-flux -generate-theme-project SkyRed -path ~/Descargas -url-source https://primeui.store/templates/angular/freya 
```

Al finalizar la ejecución, este comando creará un proyecto Maven independiente, empaquetado como `jar`, y con la carpeta `src/main/resources/META-INF/` conteniendo el archivo descriptor base **`theme.json`**. 

Luego, solo tendrás que entrar a la carpeta, modificar el `theme.json` para definir tus estilos, y compilar:

```bash
cd SkyRed
mvn clean install
```

Para más detalles sobre la estructura del descriptor de temas, consulta [createplugin.md](createplugin.md).
