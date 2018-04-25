import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class Settings {
    private static final Logger LOGGER = Logger.getLogger(Settings.class);
    private final Properties properties;
    private Settings() {
        properties = new Properties();
    }

    public static Settings fromProperties(String propertiesFile) {
        Settings settings = new Settings();
        try (FileInputStream input = new FileInputStream(propertiesFile)){
            settings.properties.load(input);
        } catch (IOException e) {
            LOGGER.warn("[ERROR] Cannot load " + propertiesFile + " using default properties", e);
        }
        return settings;
    }

    public int statsDumperFrequency() {
        return Integer.parseInt(properties.getProperty("STATS_FREQUENCY_SEC", "20"));
    }

    public int statsTopMostRequestResources() {
        return Integer.parseInt(properties.getProperty("STATS_MOST_REQUEST_RESOURCE_TOP", "10"));
    }

    public String errorFrequencyWorkDir() {
        return properties.getProperty("ERROR_HANDLER_WORK_DIR", "err/");
    }

    public int errorFrequencyPoll() {
        return Integer.parseInt(properties.getProperty("ERROR_COLLECTOR_FREQUENCY_SEC", "20"));
    }

    public String errorFrequencyFile() {
        return properties.getProperty("ERROR_FREQUENCY_FILE", "error_frequency.log");
    }

    public int queueSize() {
        return Integer.parseInt(properties.getProperty("QUEUE_SIZE", "1024"));
    }

    public String errorLogFile() {
        return properties.getProperty("ERROR_LOG", "apache_error.log");
    }

    public String dumperLogFile() {
        return properties.getProperty("DUMPER_LOG", "apache_dump.log");
    }

    public int errorFrequencyMaxFiles() {
        return Integer.parseInt(properties.getProperty("ERROR_FREQUENCY_MAX_FILES", "500"));
    }

    public InputStream getInputReader() {
        String inputStream = properties.getProperty("READER_INPUT","STDIN");
        if (inputStream.equals("STDIN")) {
            return System.in;
        }
        try {
            return new FileInputStream(new File(inputStream));
        } catch (FileNotFoundException e) {
            Logger logger = Logger.getLogger(Settings.class);
            logger.warn("Cannot open file " + inputStream);
            logger.debug(e);
            logger.info("Using STDIN for Reader input");
            return System.in;
        }
    }

    public int numberParserWorkers() {
        return Integer.parseInt( properties.getProperty("PARSER_WORKERS","1") );
    }

    public int numberErrorFrequencyWorkers() {
        return Integer.parseInt( properties.getProperty("ERROR_FREQUENCY_WORKERS","1") );
    }

    public int numberStatsRegisterWorkers() {
        return Integer.parseInt( properties.getProperty("STATS_REGISTER_WORKERS","1") );
    }
}
