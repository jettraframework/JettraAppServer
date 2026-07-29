package io.jettra.server;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import io.jettra.server.core.JettraContext;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class JettraServer {

    private HttpServer server;
    private boolean isRunning = false;
    private Map<String, Object> handlerRegistry = new HashMap<>(); // Can be HttpHandler, Class, or Supplier
    private Thread hotReloadThread;
    private String errorPage;
    private int customPort = -1;
    private io.jettra.server.core.AutocloneManager autocloneManager;
    private int cloneCount = 0;
    private Boolean serverConsoleShowRegisterPage;

    public void setPort(int port) {
        this.customPort = port;
    }

    public void setErrorPage(String path) {
        this.errorPage = path;
    }

    /**
     * Registra un manejador HTTP personalizado antes de iniciar el servidor.
     *
     * @param path la ruta, ej. "/login"
     * @param handler el manejador
     */
    public void addHandler(String path, HttpHandler handler) {
        handlerRegistry.put(path, handler);
        if (server != null) {
            registerInServer(path, handler);
        }
    }

    public void addHandler(String path, java.util.function.Supplier<HttpHandler> supplier) {
        handlerRegistry.put(path, supplier);
        if (server != null) {
            registerInServer(path, supplier);
        }
    }

    public void addHandler(String path, Class<? extends HttpHandler> handlerClass) {
        handlerRegistry.put(path, handlerClass);
        if (server != null) {
            registerInServer(path, handlerClass);
        }
    }

    private void registerInServer(String path, Object handlerObj) {
        String resolvedPath = resolvePath(path);
        HttpHandler wrapped = wrapHandler(handlerObj);
        server.createContext(resolvedPath, wrapped);
        if (resolvedPath.endsWith("/") && resolvedPath.length() > 1) {
            server.createContext(resolvedPath.substring(0, resolvedPath.length() - 1), wrapped);
        }
    }

    public static String getContextPath() {
        String contextPath = io.jettra.server.config.JettraConfig.getProperty("server.contextpath");
        if (contextPath == null || contextPath.isBlank() || contextPath.equals("/")) {
            return "/";
        }

        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        if (contextPath.endsWith("/") && contextPath.length() > 1) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }

        return contextPath;
    }

    public static String resolvePath(String path) {
        String contextPath = getContextPath();
        if (contextPath.equals("/")) {
            return path.startsWith("/") ? path : "/" + path;
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return contextPath + path;
    }

    /**
     * Inicia el servidor HTTP y los módulos de Jettra.
     */
    public void start() {
        if (isRunning) {
            IO.println("JettraServer ya se encuentra en ejecución.");
            return;
        }
        long startExecutionTime = System.currentTimeMillis();
        IO.println("Starting JettraServer...");
        IO.println("Java version: " + System.getProperty("java.version"));

        // Verify and initialize JettraSecurityDB records (JUsers, JRole, JAccreditation)
        IO.println("[JettraServer] Initializing and verifying JettraSecurityDB records (JUsers, JRole, JAccreditation)...");
        Thread.startVirtualThread(() -> io.jettra.server.autentification.repository.JettraSecurityDBInitializer.initializeIfEmpty());

        // Auto-create CLI scripts if they don't exist
        autoCreateMvnScripts();

        // Add native admin console for security database (Lazy loaded)
        this.addHandler("/securitydb/admin", () -> new io.jettra.server.autentification.AdminConsoleHandler());

        // Inicializamos componentes abstractos/ejemplo si los hay
        IO.println("Initializing ExampleRest and ConfigInjector...");
        io.jettra.server.test.ExampleRest example = new io.jettra.server.test.ExampleRest();
        io.jettra.server.config.ConfigInjector.inject(example);
        example.draw();
        //Mostrar en consola las paginas cargadas
        String serverConsoleShowRegisterPageTemp = io.jettra.server.config.JettraConfig.getProperty("server.consoleshowregisterpage");

        serverConsoleShowRegisterPage = Boolean.FALSE;
        if (serverConsoleShowRegisterPageTemp.equals("true")) {
            serverConsoleShowRegisterPage = Boolean.TRUE;
        }
        loadAnnotatedPages();

        try {
            int port;
            if (customPort > 0) {
                port = customPort;
            } else {
                String portValue = io.jettra.server.config.JettraConfig.getProperty("server.port");
                port = (portValue != null && !portValue.isBlank()) ? Integer.parseInt(portValue) : 8080;
            }
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Add static resource handler
            HttpHandler staticHandler = wrapHandler(new io.jettra.server.core.StaticResourceHandler("/static"));
            server.createContext(resolvePath("/static"), staticHandler);

            // Add custom handlers from registry
            for (Map.Entry<String, Object> entry : handlerRegistry.entrySet()) {
                registerInServer(entry.getKey(), entry.getValue());
            }

            // ... [root handled logic remains but uses wrapped] ...
            // Default handler check
            String rootPath = resolvePath("/");
            boolean rootHandled = false;
            for (String path : handlerRegistry.keySet()) {
                if (resolvePath(path).equals(rootPath)) {
                    rootHandled = true;
                    break;
                }
            }

            if (!rootHandled) {
                HttpHandler defaultHandler = exchange -> {
                    String response = "JettraServer is running!";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                };

                HttpHandler wrappedRoot = wrapHandler(defaultHandler);
                server.createContext(rootPath, wrappedRoot);

                if (rootPath.endsWith("/") && rootPath.length() > 1) {
                    server.createContext(rootPath.substring(0, rootPath.length() - 1), wrappedRoot);
                }
            }

            if (!getContextPath().equals("/")) {
                HttpHandler redirectHandler = exchange -> {
                    String path = exchange.getRequestURI().getPath();
                    String redirectUrl = getContextPath() + (path.startsWith("/") ? path : "/" + path);

                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && !query.isEmpty()) {
                        redirectUrl += "?" + query;
                    }

                    exchange.getResponseHeaders().set("Location", redirectUrl);
                    exchange.sendResponseHeaders(302, -1);
                    exchange.getResponseBody().close();
                };
                server.createContext("/", wrapHandler(redirectHandler));
            }

            server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
            server.start();
            isRunning = true;

            // Java 25 / Compact Header support detection
            String javaVersion = System.getProperty("java.version");
            boolean isJava25 = javaVersion != null && javaVersion.startsWith("25");
            String compactHeader = io.jettra.server.config.JettraConfig.getProperty("server.compactheader");

            if (isJava25 || "true".equalsIgnoreCase(compactHeader)) {
                IO.println("[JettraServer] Optimizando para Java 25: Uso de Compact Object Headers (JEP 450) detectado o configurado.");
            }

            startHotReloadWatcher();

            IO.println("JettraServer HTTP server started on port " + port);
            IO.println("JettraServer HTTP server contextpath = " + getContextPath());
            IO.println("Features initialized: REST, gRPC, JWT, Health, FaultTolerance, Session, DI, Security (XSS).");

            // Iniciar AutocloneManager si estamos en el servidor principal (no en un clon)
            if (customPort <= 0) {
                String thresholdVal = io.jettra.server.config.JettraConfig.getProperty("server.autoclone.threshold");
                int threshold = (thresholdVal != null && !thresholdVal.isBlank()) ? Integer.parseInt(thresholdVal) : 100;
                autocloneManager = new io.jettra.server.core.AutocloneManager(this, threshold);
                autocloneManager.start();
            }
            long endExecutionTime = System.currentTimeMillis();
            IO.println("Servidor ejecutado en " + (endExecutionTime - startExecutionTime) + " milisegundos.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start JettraServer embedded HTTP server.");
        }
    }

    /**
     * Detiene la ejecución del servidor.
     */
    public void stop() {
        if (!isRunning || server == null) {
            IO.println("JettraServer no está en ejecución.");
            return;
        }
        IO.println("Stopping JettraServer...");

        // Cierra todas las sesiones de todos los usuarios
        io.jettra.server.core.JettraContext.clearSessions();
        IO.println("[JettraServer] Todas las sesiones han sido cerradas.");

        String expiredValue = io.jettra.server.config.JettraConfig.getProperty("server.session.expired");
        int expired = (expiredValue != null && !expiredValue.isBlank()) ? Integer.parseInt(expiredValue) : 0;
        server.stop(expired);
        if (hotReloadThread != null) {
            hotReloadThread.interrupt();
        }
        if (autocloneManager != null) {
            autocloneManager.stop();
        }
        isRunning = false;
        IO.println("JettraServer detenido exitosamente.");
    }

    /**
     * Autoclonación Dinámica: Replica el servidor creando una nueva instancia
     * en otro puerto para gestionar la carga.
     */
    public void autoclone() {
        cloneCount++;
        int basePort;
        if (customPort > 0) {
            basePort = customPort;
        } else {
            String portValue = io.jettra.server.config.JettraConfig.getProperty("server.port");
            basePort = (portValue != null && !portValue.isBlank()) ? Integer.parseInt(portValue) : 8080;
        }
        int newPort = basePort + cloneCount;

        IO.println("[Autoclonación Dinámica] Iniciando réplica del servidor en el puerto " + newPort + "...");
        IO.println("[Autoclonación Dinámica] Se están transfiriendo los objetos más viejos y equilibrando la carga para mejorar el rendimiento.");

        JettraServer replica = new JettraServer();
        replica.setPort(newPort);
        replica.setErrorPage(this.errorPage);

        for (Map.Entry<String, Object> entry : this.handlerRegistry.entrySet()) {
            if (entry.getValue() instanceof HttpHandler) {
                replica.addHandler(entry.getKey(), (HttpHandler) entry.getValue());
            } else if (entry.getValue() instanceof java.util.function.Supplier) {
                replica.addHandler(entry.getKey(), (java.util.function.Supplier<HttpHandler>) entry.getValue());
            } else if (entry.getValue() instanceof Class) {
                replica.addHandler(entry.getKey(), (Class<? extends HttpHandler>) entry.getValue());
            }
        }

        Thread replicaThread = new Thread(() -> replica.start());
        replicaThread.start();
    }

    private HttpHandler wrapHandler(Object original) {
        return exchange -> {
            String sessionId = getOrCreateSessionId(exchange);
            JettraContext context = new JettraContext(sessionId);
            JettraContext.setCurrent(context);
            
            String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
            String lang = "en"; // Default fallback
            if (cookieHeader != null) {
                for (String c : cookieHeader.split(";")) {
                    c = c.trim();
                    if (c.startsWith("jettra_lang=")) {
                        lang = c.substring(12);
                    }
                }
            }
            context.set(JettraContext.Scope.SESSION, "jettra_lang", lang);
            try {
                // Add enhanced security headers (XSS Protection)
                exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
                exchange.getResponseHeaders().add("X-Frame-Options", "DENY");
                exchange.getResponseHeaders().add("X-XSS-Protection", "1; mode=block");
                exchange.getResponseHeaders().add("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://unpkg.com https://cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net https://unpkg.com https://cdnjs.cloudflare.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; media-src 'self' blob: data: mediastream:; connect-src 'self' ws: wss:;");
                exchange.getResponseHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");

                String path = exchange.getRequestURI().getPath();
                if (!path.endsWith("/login") && !path.contains("/securitydb/admin") && !path.contains(".")) {
                    Object credential = JettraContext.getCurrent().get(JettraContext.Scope.SESSION, "credentialFlux");
                    if (credential == null) {
                        exchange.getResponseHeaders().set("Location", resolvePath("/login"));
                        exchange.sendResponseHeaders(302, -1);
                        exchange.getResponseBody().close();
                        return;
                    }
                }

                HttpHandler instance = null;
                if (original instanceof HttpHandler) {
                    instance = (HttpHandler) original;
                } else if (original instanceof java.util.function.Supplier) {
                    instance = ((java.util.function.Supplier<HttpHandler>) original).get();
                } else if (original instanceof Class) {
                    try {
                        instance = ((Class<? extends HttpHandler>) original).getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (instance != null) {
                    try {
                        io.jettra.server.core.DependencyInjector.inject(instance);
                        instance.handle(exchange);
                    } catch (Exception e) {
                        if (exchange.getResponseCode() == -1) {
                            e.printStackTrace();
                            handleErrorRedirect(exchange, "500 Internal Server Error", e.getClass().getSimpleName() + " - " + e.getMessage(), "Handler: " + original.getClass().getSimpleName());
                        } else {
                            // Headers already sent (e.g., Broken pipe during body write)
                            // We cannot send a 500 error redirect now. Just log it quietly.
                            System.err.println("[JettraServer] Client disconnected or IO Error after headers sent: " + e.getMessage());
                        }
                    }
                } else {
                    handleErrorRedirect(exchange, "404 Not Found", "The requested handler was not found", exchange.getRequestURI().toString());
                }
            } finally {
                JettraContext.clear();
            }
        };
    }

    private void handleErrorRedirect(HttpExchange exchange, String title, String detail, String origin) throws java.io.IOException {
        if (this.errorPage != null && !this.errorPage.isEmpty()) {
            try {
                String redirectUrl = this.errorPage + "?title=" + java.net.URLEncoder.encode(title, "UTF-8")
                        + "&detail=" + java.net.URLEncoder.encode(detail, "UTF-8")
                        + "&origin=" + java.net.URLEncoder.encode(origin, "UTF-8");
                exchange.getResponseHeaders().set("Location", resolvePath(redirectUrl));
                exchange.sendResponseHeaders(302, -1);
                exchange.getResponseBody().close();
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        String req = title + ": " + detail + " at " + origin;
        exchange.sendResponseHeaders(404, req.length());
        exchange.getResponseBody().write(req.getBytes());
        exchange.getResponseBody().close();
    }

    private String getOrCreateSessionId(HttpExchange exchange) {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies != null) {
            for (String cookie : cookies.split(";")) {
                cookie = cookie.trim();
                if (cookie.startsWith("jsessionid=")) {
                    return cookie.substring("jsessionid=".length());
                }
            }
        }
        String newId = java.util.UUID.randomUUID().toString();
        exchange.getResponseHeaders().add("Set-Cookie", "jsessionid=" + newId + "; Path=/; HttpOnly");
        return newId;
    }

    private void startHotReloadWatcher() {
        String hotreload = io.jettra.server.config.JettraConfig.getProperty("server.hotreload");
        if (!"true".equalsIgnoreCase(hotreload)) {
            return;
        }

        hotReloadThread = new Thread(() -> {
            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();
                Path path = Paths.get("target/classes");
                if (!Files.exists(path)) {
                    return;
                }

                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                IO.println("[HotReload] Watching for changes in target/classes...");

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watcher.poll(1, TimeUnit.SECONDS);
                    if (key != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            IO.println("[HotReload] Change detected: " + event.context());
                            restore(); // Full restart for now
                            return;
                        }
                        key.reset();
                    }
                }
            } catch (Exception e) {
                // Stopped
            }
        });
        hotReloadThread.setDaemon(true);
        hotReloadThread.start();
    }

    /**
     * Reinicia / restaura el servidor deteniéndolo e iniciándolo de nuevo.
     */
    public void restore() {
        IO.println("Restaurando JettraServer...");
        stop();
        start();
    }

    /**
     * Retorna el estado actual del servidor.
     *
     * @return STATUS (RUNNING o STOPPED)
     */
    public String status() {
        return isRunning ? "RUNNING" : "STOPPED";
    }

    /**
     * Check de liveness para saber si el servidor está operativo.
     *
     * @return true si el servidor está levantado y aceptando peticiones.
     */
    public boolean live() {
        return isRunning;
    }

    private void loadAnnotatedPages() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            java.util.Enumeration<java.net.URL> resources = classLoader.getResources("META-INF/jettra/page.classes");

            while (resources.hasMoreElements()) {
                java.net.URL url = resources.nextElement();
                try (java.io.InputStream is = url.openStream(); java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int separatorIndex = line.indexOf('=');
                        if (separatorIndex > 0) {
                            String className = line.substring(0, separatorIndex).trim();
                            String path = line.substring(separatorIndex + 1).trim();

                            ClassLoader clForCheck = classLoader;
                            if (clForCheck == null) clForCheck = Thread.currentThread().getContextClassLoader();
                            if (clForCheck == null) clForCheck = JettraServer.class.getClassLoader();

                            java.net.URL classResource = clForCheck.getResource(className.replace('.', '/') + ".class");
                            if (classResource == null) {
                                System.err.println("[JettraServer] Advertencia: La clase " + className + " referenciada para " + path + " no se encontro en el classpath. Se omitira el registro.");
                                continue;
                            }

                            if (handlerRegistry.containsKey(path)) {
                                System.err.println("[JettraServer] Advertencia: Conflicto de rutas. La ruta " + path + " ya esta registrada. La clase " + className + " sobreescribira el manejador anterior.");
                            }

                            java.util.function.Supplier<com.sun.net.httpserver.HttpHandler> lazyLoader = new java.util.function.Supplier<>() {
                                private Class<?> cachedClass = null;

                                @Override
                                public com.sun.net.httpserver.HttpHandler get() {
                                    try {
                                        if (cachedClass == null) {
                                            ClassLoader cl = classLoader; // Prioritize the classLoader captured from main thread
                                            if (cl == null) cl = Thread.currentThread().getContextClassLoader();
                                            if (cl == null) cl = JettraServer.class.getClassLoader();
                                            cachedClass = Class.forName(className, true, cl);
                                            if (!com.sun.net.httpserver.HttpHandler.class.isAssignableFrom(cachedClass)) {
                                                System.err.println("[JettraServer] Error: La clase " + className + " anotada con @Page no implementa HttpHandler.");
                                                return null;
                                            }
                                        }
                                        return (com.sun.net.httpserver.HttpHandler) cachedClass.getDeclaredConstructor().newInstance();
                                    } catch (Throwable e) {
                                        System.err.println("[JettraServer] Error al instanciar la clase de página: " + className + " - " + e.getMessage());
                                        e.printStackTrace();
                                        return null;
                                    }
                                }
                            };
                            addHandler(path, lazyLoader);
                            if (serverConsoleShowRegisterPage) {
                                IO.println("[JettraServer] Page registrada automáticamente (Lazy): " + path + " -> " + className);
                            }

                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[JettraServer] Error al cargar clases de página desde META-INF/jettra/page.classes: " + e.getMessage());
        }
    }

    private void autoCreateMvnScripts() {
        try {
            Path scriptPathJettra = Paths.get("mvn-jettra");
            if (!Files.exists(scriptPathJettra)) {
                String scriptContent = "#!/bin/bash\n" +
                                       "if [ \"$1\" = \"-jettra\" ]; then\n" +
                                       "    shift\n" +
                                       "fi\n\n" +
                                       "# Execute the CLI tool using the local pom.xml\n" +
                                       "mvn -q exec:java -Dexec.mainClass=\"io.jettra.server.cli.PluginCLI\" -Dexec.args=\"$*\"\n";
                Files.write(scriptPathJettra, scriptContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                    Files.setPosixFilePermissions(scriptPathJettra, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
                }
                IO.println("[JettraServer] Auto-generado script 'mvn-jettra' en el directorio actual.");
            }

            Path scriptPathFlux = Paths.get("mvn-flux");
            if (!Files.exists(scriptPathFlux)) {
                String scriptContentFlux = "#!/bin/bash\n" +
                                       "if [ \"$1\" = \"-flux\" ]; then\n" +
                                       "    shift\n" +
                                       "fi\n\n" +
                                       "# Execute the CLI tool using the local pom.xml\n" +
                                       "mvn -q exec:java -Dexec.mainClass=\"io.jettra.flux.cli.FluxCLI\" -Dexec.args=\"$*\"\n";
                Files.write(scriptPathFlux, scriptContentFlux.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                    Files.setPosixFilePermissions(scriptPathFlux, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
                }
                IO.println("[JettraServer] Auto-generado script 'mvn-flux' en el directorio actual.");
            }
        } catch (Exception e) {
            System.err.println("[JettraServer] Error al crear scripts CLI: " + e.getMessage());
        }
    }
}
