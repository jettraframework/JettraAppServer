package io.jettra.server.config;

import java.io.InputStream;
import java.util.Properties;

public class JettraConfig {
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = JettraConfig.class.getClassLoader().getResourceAsStream("jettra-config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        AppRoleGenerator.generateFromProperties(properties);
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public static Properties getProperties() {
        return properties;
    }
}
