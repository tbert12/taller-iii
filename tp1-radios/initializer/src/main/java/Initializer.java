import org.apache.log4j.Logger;

public class Initializer {
    private static final Logger LOGGER = Logger.getLogger(Initializer.class);
    private static final Settings SETTINGS = Settings.from("initializer.properties");
    public static void main(String[] args) {
        LOGGER.info("Initialize all queues to use");
        CommunicationWrapper comm = CommunicationWrapper.getConnection(
                SETTINGS.get("RABBITMQ_HOST","localhost"),
                SETTINGS.get("RABBITMQ_PORT",5672)
        );
        if (comm == null) {
            LOGGER.fatal("Cannot connect. Abort initializer");
            return;
        }

        comm.queueDeclare(SETTINGS.get("ADMIN_REQUEST_QUEUE","ADMIN_REQUEST"));

        comm.queueDeclare(SETTINGS.get("ADMIN_RESPONSE_QUEUE","ADMIN_RESPONSE"));

        comm.queueDeclare(SETTINGS.get("RADIO_QUEUE","RADIO"));

        comm.queueDeclare(SETTINGS.get("CLIENT_QUEUE","CLIENT"));

        comm.close();
        LOGGER.info("OK. Initializer");

    }
}
