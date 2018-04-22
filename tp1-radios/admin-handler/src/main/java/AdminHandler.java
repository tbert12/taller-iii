import Message.*;
import org.apache.log4j.Logger;
import sun.misc.Signal;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class AdminHandler {
    private static final Logger LOGGER = Logger.getLogger(AdminHandler.class);
    private static final Settings SETTINGS = Settings.from("admin-handler.properties");


    private static final String RABBITMQ_HOST       = SETTINGS.get("RABBITMQ_HOST","localhost");
    private static final int RABBITMQ_PORT          = SETTINGS.get("RABBITMQ_PORT",5672);
    private static final String ADMIN_REQ_QUEUE     = SETTINGS.get("ADMIN_REQUEST_QUEUE","ADMIN_REQUEST");
    private static final String ADMIN_RES_QUEUE     = SETTINGS.get("ADMIN_RESPONSE_QUEUE","ADMIN_RESPONSE");
    private static final int COUNT_TOP_USERS        = SETTINGS.get("COUNT_TOP_USERS",10);

    private final CommunicationWrapper communication;

    private DB db;

    AdminHandler() throws Exception {
        communication = CommunicationWrapper.getConnection(RABBITMQ_HOST,RABBITMQ_PORT);
        if (communication == null) {
            LOGGER.fatal("Cannot open communication");
            throw new Exception("Cannot open communication");
        }

        db = new BlockDatabase();
    }

    private String generatePrintableStats() {
        List<String> topUsers = db.getTopUsers(COUNT_TOP_USERS);
        List<String> usersPerStations = db.getCountUserPerStation();
        StringBuilder stats = new StringBuilder("Number of users per station");
        for (String userCount : usersPerStations) {
            stats.append("\n\t-").append(userCount);
        }
        stats.append("\n\nTop users (minutes)");
        for (String user : topUsers) {
            stats.append("\n\t-").append(user);
        }
        return stats.toString();

    }

    void start() {
        LOGGER.info("Waiting admin request");

        String consumerTag = communication.append(ADMIN_REQ_QUEUE,req -> {
            Message stats = new MessageBuilder()
                    .setType(MessageType.ADMIN_RESPONSE_STATS)
                    .setInfo(generatePrintableStats())
                    .build();
            communication.put(ADMIN_RES_QUEUE, stats);
        });


        Semaphore semaphore = new Semaphore(0);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("SIGINT detected. Closing Admin-Handler");
                communication.detach(consumerTag);
                communication.close();
                LOGGER.info("Admin-Handler closed");
                semaphore.release();
                if (LOGGER.isDebugEnabled()) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignored) {}
                }
            })
        );
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupt signal");
            LOGGER.debug(e);
        }
        LOGGER.info("Exiting");
    }

    public static void main(String[] argv) {
        try {
            AdminHandler adminHandler = new AdminHandler();
            adminHandler.start();
        } catch (Exception e) {
            LOGGER.info("Cannot start Admin Handler");
            LOGGER.debug(e);
        }
    }
}
