package io.jettra.server.discoverer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class DiscoveredRegistry {

    private static final String DISCOVERED_RESOURCE = "META-INF/jettra/discovered.classes";

    /**
     * Devuelve una lista de clases descubiertas que tienen @Discovered(automatic=true).
     * Solo retorna datos si el mainClass proporcionado está anotado con @DiscoveredLoad.
     *
     * @param mainClass La clase principal del proyecto que debe tener @DiscoveredLoad.
     * @return Lista de clases a cargar automáticamente.
     */
    public static List<Class<?>> getDiscoveredClasses(Class<?> mainClass) {
        List<Class<?>> classes = new ArrayList<>();
        
        if (mainClass == null || !mainClass.isAnnotationPresent(DiscoveredLoad.class)) {
            System.out.println("[JettraServer] " + (mainClass != null ? mainClass.getSimpleName() : "null") + " no posee @DiscoveredLoad. Descubrimiento automatico deshabilitado.");
            return classes;
        }

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(DISCOVERED_RESOURCE);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream is = url.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                     
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        // Formato esperado: com.example.MiClase=true
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String className = parts[0];
                            boolean automatic = Boolean.parseBoolean(parts[1]);
                            if (automatic) {
                                try {
                                    Class<?> clazz = Class.forName(className, true, classLoader);
                                    if (!classes.contains(clazz)) {
                                        classes.add(clazz);
                                    }
                                } catch (ClassNotFoundException e) {
                                    System.err.println("[JettraServer] Advertencia: No se pudo cargar la clase descubierta: " + className);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[JettraServer] Error al cargar clases descubiertas desde " + DISCOVERED_RESOURCE + ": " + e.getMessage());
        }

        return classes;
    }
}
