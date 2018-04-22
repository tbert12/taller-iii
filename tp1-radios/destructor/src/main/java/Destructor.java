import org.apache.log4j.Logger;

import java.io.IOException;

public class Destructor {
    private static final Logger LOGGER = Logger.getLogger(Destructor.class);
    private static final Settings SETTINGS = Settings.from("destructor.properties");
    public static void main(String[] args) {
        LOGGER.info("Desruct all Queues and Databases");
        CommunicationWrapper comm = CommunicationWrapper.getConnection(
                SETTINGS.get("RABBITMQ_HOST","localhost"),
                SETTINGS.get("RABBITMQ_PORT",5672)
        );
        if (comm == null) {
            LOGGER.fatal("Cannot connect");
            return;
        }

        comm.deleteQueue(SETTINGS.get("ADMIN_REQUEST_QUEUE","ADMIN_REQUEST"));

        comm.deleteQueue(SETTINGS.get("ADMIN_RESPONSE_QUEUE","ADMIN_RESPONSE"));

        comm.deleteQueue(SETTINGS.get("RADIO_QUEUE","RADIO"));

        comm.deleteQueue(SETTINGS.get("CLIENT_QUEUE","CLIENT"));

        if (SETTINGS.get("CLEAN_DATABASES",false)) {
            LOGGER.info("DBs cleaned");
            try {
                DB database = new BlockDatabase();
                database.cleanDatabases();
            } catch (IOException e) {
                LOGGER.warn("Cannot clean databases");
            }
        }

        comm.close();
        LOGGER.info("OK. End destructor");
    }
}
