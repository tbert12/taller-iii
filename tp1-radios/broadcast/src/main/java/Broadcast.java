import Message.*;
import org.apache.log4j.Logger;
import sun.misc.Signal;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Broadcast {
    private static final Logger LOGGER = Logger.getLogger(Broadcast.class);
    private static final Settings SETTINGS = Settings.from("broadcast.properties");

    private static final String RABBITMQ_HOST   = SETTINGS.get("RABBITMQ_HOST","localhost");
    private static final int RABBITMQ_PORT      = SETTINGS.get("RABBITMQ_PORT",5672);
    private static final String RADIO_QUEUE     = SETTINGS.get("RADIO_QUEUE","RADIO");
    private static final int MESSAGE_EXPIRATION_SECONDS = SETTINGS.get("MESSAGE_EXPIRATION_SECONDS", 30);

    private String consumerRadioTag;

    private final CommunicationWrapper communication;

    private DB db;

    Broadcast() throws Exception {
        communication = CommunicationWrapper.getConnection(RABBITMQ_HOST,RABBITMQ_PORT);
        if (communication == null) {
            LOGGER.fatal("Cannot open communication");
            throw new Exception("Cannot open communication");
        }

        if (!communication.queueDeclare(RADIO_QUEUE)) {
            LOGGER.fatal("Cannot declare queue " + RADIO_QUEUE);
            communication.close();
            throw new Exception("Cannot declare queue " + RADIO_QUEUE);
        }

        db = new BlockDatabase();
    }

    private void registerSIGINT() throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("SIGINT detected. Closing Broadcast");
                communication.detach(consumerRadioTag);
                communication.close();
                LOGGER.info("Broadcast closed");
                semaphore.release();
                if (LOGGER.isDebugEnabled()) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignored) {}
                }
            })
        );
        semaphore.acquire();
    }

    void start() {
        LOGGER.info("Waiting Radio message");

        consumerRadioTag = communication.append(RADIO_QUEUE, message -> {
            if (message.getType() == MessageType.RADIO_PACKAGE) {
                db.addStation(message.getRadio());
                for (String userQueue : db.getUsersInStation(message.getRadio())) {
                    communication.put(userQueue, message, MESSAGE_EXPIRATION_SECONDS);
                }
            } else if (message.getType() == MessageType.END_TRANSMISSION) {
                Message messageEnd = new MessageBuilder()
                        .setType(MessageType.END_CONNECTION)
                        .build();
                for (String userQueue : db.getUsersInStation(message.getRadio())) {
                    LOGGER.info(String.format("Send end transmission (%s) to user(queue) %s", message.getRadio(), userQueue));
                    communication.put(userQueue, messageEnd, MESSAGE_EXPIRATION_SECONDS);
                }
                db.deleteStation(message.getRadio());
                LOGGER.info("Deleted station " + message.getRadio());
            } else {
                LOGGER.warn("Unhandled message with type: " + message.getType());
            }
        });


        try {
            registerSIGINT();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupt signal");
        }
        LOGGER.info("Exiting");
    }

    public static void main(String[] argv) {
        Broadcast broadcast = null;
        try {
            broadcast = new Broadcast();
        } catch (Exception e) {
            LOGGER.info("Cannot start broadcast");
            LOGGER.debug(e);
            System.exit(1);
        }
        broadcast.start();
    }
}
