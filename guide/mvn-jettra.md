# mvn-jettra CLI

`mvn-jettra` es la herramienta de línea de comandos integrada en el ecosistema **Jettra** para administrar la creación, instalación, desinstalación, sincronización de roles e integración de plugins autónomos.

Este script es autogenerado dinámicamente por `JettraServer` en el directorio raíz de tu proyecto al iniciar el servidor por primera vez, inyectándole permisos de ejecución (`chmod +x`).

---

## Tabla de Comandos

| Comando | Descripción |
| :--- | :--- |
| `generate-plugin` | Genera la estructura autónoma de un plugin a partir de un proyecto Jettra. |
| `install-plugin` | Instala un plugin en el proyecto, actualiza `pom.xml`, `TemplatePage.java` y `plugin-config.json`. |
| `sync-plugin-roles` | Sincroniza los roles definidos en `plugin-config.json` creando Enums y clases de roles en Java. |
| `remove-plugin` | Remueve la configuración y dependencia de un plugin instalado en el proyecto. |
| `list-plugin` | Lista los plugins disponibles en el repositorio central JettraHub. |
| `get-plugin` | Descarga la especificación de un plugin desde JettraHub y actualiza el `pom.xml`. |
| `help` | Muestra el menú de ayuda con la sintaxis de todos los comandos. |

---

## 1. Comando: `generate-plugin`

Genera un módulo de plugin autónomo aislando su código, recursos, propiedades multilenguaje y definiendo sus descriptores de seguridad y navegación.

### Sintaxis

```bash
./mvn-jettra generate-plugin -path <directorio_destino> -name <NombrePlugin> [opciones]
```

### Ejemplo de Uso

```bash
./mvn-jettra generate-plugin -path /home/usuario/proyectos -name MiNuevoPlugin exclude-package com.ejemplo.general exclude-class ClaseIgnorada.java includes-test yes
```

### Opciones y Parámetros

- **`-path <directorio>`**: Ruta del directorio donde se creará la carpeta del plugin.
- **`-name <Nombre>`**: Nombre en CamelCase del plugin (ej. `MiNuevoPlugin`).
- **`exclude-plugin`**: Lista separada por comas de nombres de plugins a excluir.
- **`exclude-package`**: Paquetes de clases Java a excluir de la migración.
- **`exclude-class`**: Clases Java específicas a omitir.
- **`includes-test`**: Copia también las clases y recursos de pruebas (`yes` | `no`).

### Transformaciones Automáticas realizadas por `generate-plugin`

1. **Empaquetado y Aislamiento**:
   - Asigna el `groupId` del pom como `io.jettraflux.<nombre-plugin-minuscula>`.
   - Crea el paquete base para clases auxiliares en `pjc.<nombre-plugin-minuscula>`.
2. **Transformación de Roles del Sistema (`systemRole`)**:
   - Reemplaza las referencias `@RolesAllowed({systemRole.ADMIN})` por `@RolesAllowed({pjc.<nombre-plugin-minuscula>.SystemRole.ADMIN})`.
   - Transforma `import jcf.systemRole;` en `import pjc.<nombre-plugin-minuscula>.SystemRole;`.
   - Genera automáticamente las clases `SystemRole.java` y `<NombrePlugin>SystemRole.java` dentro del paquete `pjc.<nombre-plugin-minuscula>`.
3. **Prefijo en Rutas REST (`@Path`)**:
   - En las anotaciones `@Path` a nivel de clase de los controladores REST, antepone automáticamente `/<nombre-plugin-minuscula>` (ej. `@Path("/mydata")` se transforma en `@Path("/minuevoplugin/mydata")`).
   - Las anotaciones `@Path` a nivel de método (ej. `@Path("/{id}")`) se mantienen intactas.
4. **Rutas de Páginas UI (`@Page`)**:
   - Las anotaciones `@Page(path = "...")` incorporan el prefijo `/<nombre-plugin-minuscula>/...`.
5. **Propiedades Multilenguaje Localizadas**:
   - Genera archivos de propiedades únicos (ej. `messages-MiNuevoPlugin_es.properties`) e inyecta `@InjectProperties(name = "messages-MiNuevoPlugin")`.
6. **Descriptor de Plugin (`plugin-descriptor.md`)**:
   - Genera el descriptor con las estructuras de `WidgetLet` y la sección `## SecurityRole` extrayendo los roles de `security.roles` definidos en `jettra-config.properties`.

---

## 2. Comando: `install-plugin`

Instala un plugin local o empaquetado en tu proyecto Jettra actual.

### Sintaxis

```bash
./mvn-jettra install-plugin <nombre-plugin | ruta_del_plugin>
```

### Ejemplo de Uso

```bash
./mvn-jettra install-plugin MiNuevoPlugin
./mvn-jettra install-plugin /ruta/absoluta/a/MiNuevoPlugin
```

### ¿Qué hace internamente?

1. **Compilación e Inyección de Dependencia**: Si se indica una ruta, ejecuta `mvn clean install` e inyecta la dependencia `<dependency>` en tu `pom.xml`.
2. **Inyección de Menús en `TemplatePage.java`**: Agrega los `WidgetLet` de navegación delimitados por marcadores de comentarios para evitar duplicaciones.
3. **Generación/Actualización de `plugin-config.json`**:
   Crea o actualiza el archivo `src/main/resources/plugin-config.json` definiendo las secciones de roles de aplicación y sinónimos de roles de seguridad del sistema:

   ```json
   [
     {
       "id": "MiNuevoPlugin",
       "roles": [
         {
           "plugin-role": "ADMIN",
           "applicative-role": "ADMIN"
         }
       ],
       "security-roles": [
         {
           "plugin-security-role": "ADMIN",
           "applicative-security-role": "ADMINISTRADOR"
         }
       ]
     }
   ]
   ```

---

## 3. Comando: `sync-plugin-roles`

Sincroniza la configuración de roles almacenada en `plugin-config.json` generando código Java listo para compilar.

### Sintaxis

```bash
./mvn-jettra sync-plugin-roles
```

### Resultado de Ejecución

Genera en el paquete `io.jettraflux.roles`:
- `<NombrePlugin>Roles.java`: Enum con los roles de interfaz gráfica (`plugin-role` / `applicative-role`).
- `<NombrePlugin>SystemRole.java`: Clase Java con los roles de seguridad del sistema (`plugin-security-role` / `applicative-security-role`).

---

## 4. Soporte de Sinónimos de Roles en Tiempo de Ejecución

Jettra integra un motor de resolución dinámica de sinónimos de roles:
- Si el plugin requiere el rol `ADMIN` en su anotación `@RolesAllowed({MiNuevoPluginSystemRole.ADMIN})` pero el token JWT o sesión del usuario autenticado contiene la etiqueta `ADMINISTRADOR`, el framework consulta `plugin-config.json`.
- Al encontrar la equivalencia en `"security-roles"` (`"plugin-security-role": "ADMIN"`, `"applicative-security-role": "ADMINISTRADOR"`), el framework convalida el acceso automáticamente tanto en peticiones HTTP/REST como en vistas UI.

---

## 5. Comando: `remove-plugin`

Desinstala la configuración de un plugin previamente integrado.

### Sintaxis

```bash
./mvn-jettra remove-plugin <nombre-plugin>
```

### Acciones

- Elimina la dependencia `<dependency>` del archivo `pom.xml`.
- Remueve los bloques de menú y variables en `TemplatePage.java`.

---

## 6. Comandos JettraHub: `list-plugin` y `get-plugin`

Permiten descubrir y descargar plugins desde el repositorio central JettraHub.

```bash
# Listar plugins disponibles en la nube
./mvn-jettra list-plugin

# Obtener e inyectar la dependencia de un plugin publicado
./mvn-jettra get-plugin <NombrePlugin>
```

---

## 7. Comando: `help`

Muestra el manual interactivo en consola con la sintaxis completa de todos los comandos:

```bash
./mvn-jettra help
```
