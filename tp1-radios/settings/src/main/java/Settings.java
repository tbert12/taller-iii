import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class Settings {
    private static final Logger LOGGER = Logger.getLogger(Settings.class);
    private final Properties properties;
    private Settings() {
        properties = new Properties();
    }

    public static Settings from(String propertiesFile) {
        InputStream input = null;
        Settings settings = new Settings();
        try {
            input = new FileInputStream(propertiesFile);
            settings.properties.load(input);
            LOGGER.info(String.format("\"%s\" was loaded correctly", propertiesFile));
        } catch (IOException ex) {
            LOGGER.error(String.format("Cannot load \"%s\" using all default values",propertiesFile));
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.warn("IOException when attempt to close " + propertiesFile);
                    LOGGER.debug(e);
                }
            }
        }
        return settings;
    }

    public int get(String name, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(name, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid Int value " + properties.getProperty(name) + " of property: " + name + ". Return default");
            return defaultValue;
        }
    }

    public Boolean get(String name, boolean defaultValue) {
        return Boolean.valueOf(properties.getProperty(name, String.valueOf(defaultValue)));
    }

    public String get(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }
}