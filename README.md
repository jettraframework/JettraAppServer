# 🚀 Bienvenido a JettraAppServer

**JettraServer** es un servidor Java ultra-ligero y altamente optimizado. Construido sobre los estándares más recientes orientados a **Java 25**, saca provecho del modelo de ejecución concurrente unificado (Virtual Threads) y las directrices de Project Leyden para ofrecer tiempos de respuesta y de arranque (AOT) drásticamente bajos.

Concebido como una alternativa minimalista para ambientes con recursos moderados, JettraServer proporciona una plataforma base ágil para manejar comunicación REST y gRPC. Adicionalmente, cuenta con estrecha compatibilidad con la librería de cliente ligero **JettraUI**.

---

## ⚙️ Configuración del Puerto del Servidor

Por defecto, JettraServer escuchará tráfico en el puerto `8080`. Sin embargo, es altamente configurable y puedes establecer el puerto deseado usando el archivo principal de propiedades.

Simplemente edita o agrega la siguiente línea al archivo `src/main/resources/jettra-config.properties`:

```properties
server.port=9090
```
*Si no especificas un puerto, el sistema utilizará el puerto 8080 como "fallback".*

---

## 🛠️ Compilación y Ejecución (Java)

JettraServer utiliza Maven para su ciclo de vida y manejo de dependencias. Para compilar una versión de distribución:

**1. Compilar y empaquetar el ejecutable (JAR)**
Abre tu terminal en la raíz del proyecto y ejecuta:
```bash
mvn clean install
```
*(Este comando construirá el proyecto y generará el empaquetado final en la carpeta `target`)*

**2. Iniciar el servidor (Modo Standalone)**
Levanta el servidor invocando directamente el archivo JAR optimizado:
```bash
java -jar target/JettraAppServer-1.0.0-SNAPSHOT.jar
```
*La terminal mostrará un aviso de "Started" indicando el puerto final en el que está escuchando.*

**3. Generación de Scripts CLI (mvn-flux y mvn-jettra)**
Puedes instruir al servidor a generar de manera manual los scripts CLI (necesarios para ejecutar comandos automáticos) pasándole el argumento `-generate-flux-jettra-sh` al ejecutar la aplicación. Esto creará los archivos `mvn-flux` y `mvn-jettra` en el directorio actual, que debe coincidir con el lugar donde se encuentra el `pom.xml` de tu proyecto. 

Por ejemplo:
```bash
java -jar target/MyApplication.jar -generate-flux-jettra-sh
```
*Si tienes integrado JettraAppServer correctamente en la clase `main` de tu aplicación, este comando generará los archivos inmediatamente y finalizará la ejecución.*

---

## 📦 Uso como Dependencia (Librería)

JettraServer está pensado primordialmente para incrustarse en otras aplicaciones. Una vez instalado en tu repositorio local mediante `mvn clean install`, puedes incluirlo en cualquier otro proyecto Maven agregando el siguiente bloque a su archivo `pom.xml`:

```xml
<dependency>
    <groupId>com.jettra</groupId>
    <artifactId>JettraAppServer</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Con esto, el proyecto principal heredará el motor de inyección de configuración (`@JettraConfigProperty`), las dependencias subyacentes, y la capacidad de instanciar y arrancar el servidor embebido desde su propia clase `main()`.

---

## 🐳 Contenedorización con Docker

Si prefieres ejecutar o desplegar tu infraestructura de manera aislada y replicable, JettraServer soporta empaquetado de contenedores natural.

**1. Construir la imagen de Docker local**
Una vez empaquetado el jar en el paso anterior, usa el `Dockerfile` incluido para crear la imagen de tu aplicación:
```bash
docker build -t jettraserver:latest .
```

**2. Levantar servidor contenedorizado**
Puedes iniciar un nuevo contenedor mapeando el puerto designado al puerto real externo de tu máquina (ej. `8080` a `8080` u otro que hayas configurado en properties).

```bash
docker run -d -p 8080:8080 --name servidor-backend jettraappserver:latest
```
*(Si decidiste configurar tu aplicación con `server.port=9090`, asegúrate de mapear `docker run -d -p 9090:9090` consecuentemente).*
