# Inicialización de Proyectos Backend: `./mvn-flux -initialize-backend`

El comando `-initialize-backend` de la herramienta de línea de comandos `mvn-flux` automatiza la configuración completa e inicialización de proyectos **backend** y **microservicios** dentro del ecosistema Jettra.

A diferencia del comando `-initialize-front-end`, este comando está optimizado exclusivamente para el desarrollo de APIs REST, repositorios desacoplados, servicios y microservicios, omitiendo capas gráficas o vistas de usuario (`login`, `page`, `template`, `model`).

---

## 🚀 Flujo Rápido de Uso

### 1. Generar un proyecto Maven básico
Puedes utilizar el arquetipo estándar `quickstart` de Maven:

```bash
mvn archetype:generate \
    -DgroupId=com.example.server \
    -DartifactId=MiServicio \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false
```

### 2. Acceder al proyecto e inicializar el backend
Coloca la herramienta `mvn-flux` (o añade la dependencia `JettraAppServer` en tu `pom.xml`) y ejecuta:

```bash
./mvn-flux -initialize-backend
```

*Alias soportados:*
- `./mvn-flux -initialize-backend`
- `./mvn-flux --initialize-backend`
- `./mvn-flux initialize-backend`

---

## 📦 Estructura y Artefactos Generados

Al ejecutar el comando, se crea la siguiente arquitectura lista para compilar y ejecutar:

```
MiServicio/
├── pom.xml                                      # Configuración con Java 25, dependencias Jettra y plugins
├── Dockerfile                                   # Imagen optimizada BellSoft Liberica JRE con AppCDS (AOT)
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   ├── jettra-config.properties         # Configuración del servidor backend, puertos, JWT y roles
│   │   │   ├── messages.properties              # Mensajes base
│   │   │   ├── messages_es.properties           # Soporte en español
│   │   │   └── messages_en.properties           # Soporte en inglés
│   │   └── java/
│   │       ├── jcf/
│   │       │   ├── AppRole.java                 # Enum de roles del sistema (ADMIN, MANAGER, USER)
│   │       │   └── systemRole.java              # Constantes para anotaciones (@RolesAllowed)
│   │       └── com/example/server/
│   │           ├── App.java                     # Servidor empotrado, OpenAPI y Swagger UI
│   │           ├── entity/
│   │           │   └── Person.java              # Entidad Record con validaciones (@NotNull, @Email, etc.)
│   │           ├── repository/
│   │           │   ├── PersonRepository.java    # Interfaz del repositorio
│   │           │   └── PersonRepositoryImpl.java# Implementación del repositorio en memoria
│   │           └── controller/
│   │               └── PersonController.java    # Controlador REST protegido con @Secured y @RolesAllowed
│   └── test/
│       └── java/
│           └── com/example/server/
│               ├── AppTest.java                 # Prueba de integración de autenticación JWT
│               ├── TestLauncher.java            # Lanzador del servidor de pruebas (@JettraTestLauncher)
│               └── controller/
│                   └── PersonControllerTest.java# Pruebas unitarias e integración de endpoints REST
```

---

## ⚙️ Componentes Clave

### 1. `jettra-config.properties`
Configurado con `server.typebackend=true`, puerto `9050`, contexto raíz `/`, secret y expiración de JWT:

```properties
app.title=MiServicio
app.shorttitle=MS
server.port=9050
server.contextpath=/
server.compactheader=true
server.session.timeout=0
app.language=es
app.theme=sai
app.animated=false
server.hotreload=true
baseUri=http://localhost:9050/
server.typebackend=true
#JWT Security
server.JWT_SECRET = default_secret_key_jettra_rest_2026
server.JWT_EXPIRATION=3600000
server.consoleshowregisterpage=false
security.roles=ADMIN,MANAGER,DEMO
app.roles=ADMIN,MANAGER, USER, SXRM
server.auth.exclude=/autentification,/plugin
```

### 2. Paquete `jcf` (Roles y Seguridad)
- **`AppRole.java`**:
  ```java
  package jcf;

  public enum AppRole {
      ADMIN,
      MANAGER,
      USER;

      public String getValue() {
          return name();
      }
  }
  ```
- **`systemRole.java`**:
  ```java
  package jcf;

  public class systemRole {
      public static final String ADMIN = "ADMIN";
      public static final String MANAGER = "MANAGER";
      public static final String DEMO = "DEMO";
  }
  ```

### 3. Controlador Protegido (`PersonController.java`)
El controlador utiliza inyección de dependencias (`@Inject`), autodescubrimiento (`@Discovered`), especificación OpenAPI y protección con JWT (`@Secured` y `@RolesAllowed({systemRole.ADMIN})`):

```java
package com.example.server.controller;

import com.example.server.entity.Person;
import com.example.server.repository.PersonRepository;
import io.jettra.core.inject.annotation.Inject;
import io.jettra.rest.annotations.Consumes;
import io.jettra.rest.annotations.DELETE;
import io.jettra.rest.annotations.GET;
import io.jettra.rest.annotations.POST;
import io.jettra.rest.annotations.PUT;
import io.jettra.rest.annotations.Path;
import io.jettra.rest.annotations.PathParam;
import io.jettra.rest.annotations.Produces;
import io.jettra.rest.annotations.Secured;
import io.jettra.rest.annotations.accreditation.RolesAllowed;
import io.jettra.rest.core.Response;
import io.jettra.server.discoverer.Discovered;
import io.jettra.server.openapi.annotations.OpenApi;
import io.jettra.server.openapi.annotations.Operation;
import java.util.List;
import jcf.systemRole;

@Secured
@Path("/plugin/demo/person")
@RolesAllowed({systemRole.ADMIN})
@Discovered
@OpenApi(title = "Person", version = "v1.0", description = "API for Person management")
public class PersonController {

    @Inject
    PersonRepository personRepository;

    @GET
    @Path("/")
    @Produces("application/json")
    @Operation(summary = "findAll", description = "Returns all records")
    public List<Person> findAll() {
        return personRepository.findAll();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Operation(summary = "save", description = "Saves a new Person")
    public Response save(Person person) {
        personRepository.save(person);
        return Response.ok("{\"message\": \"Saved successfully\"}").build();
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Operation(summary = "update", description = "Updates an existing Person")
    public Response update(Person person) {
        personRepository.save(person);
        return Response.ok("{\"message\": \"Updated successfully\"}").build();
    }

    @DELETE
    @Path("/{id}")
    @Produces("application/json")
    @Operation(summary = "delete", description = "Deletes a Person by id")
    public Response delete(@PathParam("id") String id) {
        personRepository.delete(id);
        return Response.ok("{\"message\": \"Deleted successfully\"}").build();
    }
}
```

### 4. Servidor Principal (`App.java`)
Configurado para iniciar el servidor de enrutamiento REST, exponer OpenAPI en `/openapi.json`, Swagger UI en `/swagger-ui` y registrar automáticamente todos los controladores marcados con `@Discovered`.

```java
package com.example.server;

import io.jettra.rest.server.JettraRestServer;
import io.jettra.server.JettraServer;
import io.jettra.server.config.ConfigInjector;
import io.jettra.server.config.JettraConfigProperty;
import io.jettra.server.discoverer.DiscoveredLoad;
import io.jettra.server.discoverer.DiscoveredRegistry;
import io.jettra.server.openapi.OpenApiHandler;
import io.jettra.server.openapi.SwaggerUIHandler;
import java.util.ArrayList;
import java.util.List;

@DiscoveredLoad
public class App {

    @JettraConfigProperty(name = "app.title")
    private String appTitle;
    @JettraConfigProperty(name = "server.port")
    private String port;
    @JettraConfigProperty(name = "server.contextpath")
    private String contextpath;
    public static JettraServer serverInstance;

    public void initUI() {
        ConfigInjector.inject(this);
        System.out.println("Iniciando aplicación Backend: " + appTitle);
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0 && args[0].equals("-console")) {
            io.jettra.server.autentification.SecurityCLI.main(args);
            return;
        }
        if (args != null && args.length > 0 && args[0].equals("-generate-flux-jettra-sh")) {
            io.jettra.server.JettraServer.generateMvnScripts();
            return;
        }

        App app = new App();
        app.initUI();
        io.jettra.flux.complex.ErrorPage.path = "http://localhost:" + app.port + app.contextpath;

        System.out.println("Levantando servidor de enrutamiento JettraServer empotrado...");
        JettraServer server = new JettraServer();
        server.setErrorPage("/error");
        server.addHandler("/error", io.jettra.flux.complex.ErrorPage.class);
        server.addHandler("/swagger-ui", io.jettra.flux.complex.SwaggerUIPage.class);

        List<Class<?>> controllers = new ArrayList<>(DiscoveredRegistry.getDiscoveredClasses(App.class));

        server.addHandler("/openapi.json", new OpenApiHandler(controllers));
        server.addHandler("/swagger-ui", new SwaggerUIHandler("/openapi.json"));

        JettraRestServer.registerDiscovered(server, App.class);

        server.start();
    }
}
```

### 5. Suite de Pruebas (`JettraTest`)
Se generan automáticamente:
- **`TestLauncher.java`**: Gestiona el ciclo de vida del servidor durante la fase `test` de Maven con `@JettraTestLauncher`.
- **`AppTest.java`**: Valida el login JWT contra `/auth/login` y el consumo de rutas protegidas mediante `JwtTestClient`.
- **`PersonControllerTest.java`**: Valida tanto la inyección directa del controlador como las peticiones HTTP autenticadas (GET, POST, PUT, DELETE).

---

## 🧪 Ejecutar Pruebas y Compilar

Para compilar el proyecto:
```bash
mvn clean compile
```

Para ejecutar las pruebas con el ejecutor nativo `JettraTest`:
```bash
mvn test
```

Para empaquetar el Fat JAR ejecutable:
```bash
mvn package
```
