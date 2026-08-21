# JettraAppServer - Comprehensive Guide & Architecture Manual

## 1. Overview & Architecture
`JettraAppServer` is an ultra-lightweight, modular application server engineered on **Java 25** with Virtual Threads (Project Loom). It serves as the enterprise microkernel and runtime host for Jettra plugins, web endpoints, background workers, security contexts, and JettraFlux applications.

```
                    JettraAppServer Runtime
                               │
       ┌───────────────────────┼───────────────────────┐
       ▼                       ▼                       ▼
Microkernel Core       Plugin Lifecycle         Virtual Threads
- ClassLoader Root     - Hot-plugging JARs      - Zero-overhead I/O
- CDI / Dependency     - Scoped Contexts        - Embedded HTTP/2
```

---

## 2. Key Features
- **Pure Java 25 Microkernel**: Starts in milliseconds with minimal heap consumption.
- **Dynamic Plugin Subsystem**: Isolated classloaders for hot-loading and managing business plugins at runtime.
- **Embedded HTTP & Web Services**: High-throughput non-blocking request routing.
- **Integrated Security & Scoped Contexts**: Native integration with `JettraJWT` and role-based access control.
- **CLI & Orchestration**: Automated code scaffolding and initialization scripts (`mvn-flux`, `mvn-jettra`).

---

## 3. Configuration & Bootstrapping

`jettra-config.properties`:
```properties
server.port=8080
server.host=0.0.0.0
server.threads.virtual=true
plugin.directory=./plugins
security.jwt.secret=your-secret-key-32-chars-minimum
```

### Launching Server
```bash
mvn clean compile exec:java
```

---

## 4. Plugin Architecture & Usage

### 4.1 Creating a Jettra Plugin
```java
package com.example.plugin;

import io.jettra.server.plugin.JettraPlugin;
import io.jettra.server.plugin.PluginContext;

public class InventoryPlugin implements JettraPlugin {
    @Override
    public void onStart(PluginContext ctx) {
        System.out.println("InventoryPlugin started on port: " + ctx.getPort());
    }

    @Override
    public void onStop() {
        System.out.println("InventoryPlugin stopped cleanly.");
    }
}
```

### 4.2 Handling Requests with Scoped Contexts
```java
package com.example.plugin;

import io.jettra.server.annotation.Route;
import io.jettra.server.http.HttpRequest;
import io.jettra.server.http.HttpResponse;

public class ProductController {
    
    @Route(path = "/api/products", method = "GET")
    public HttpResponse listProducts(HttpRequest req) {
        return HttpResponse.ok("{\"products\": [\"item1\", \"item2\"]}");
    }
}
```
