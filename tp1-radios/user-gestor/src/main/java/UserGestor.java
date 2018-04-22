import Message.*;
import org.apache.log4j.Logger;
import sun.misc.Signal;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class UserGestor {
    private static final Logger LOGGER = Logger.getLogger(UserGestor.class);

    private static final Logger CONNECTION_LOGGER = Logger.getLogger("connection");

    private static final Settings SETTINGS = Settings.from("user-gestor.properties");

    private static final String RABBITMQ_HOST   = SETTINGS.get("RABBITMQ_HOST","localhost");
    private static final int RABBITMQ_PORT      = SETTINGS.get("RABBITMQ_PORT",5672);
    private static final String CLIENT_QUEUE = SETTINGS.get("CLIENT_QUEUE","CLIENT");

    private String consumerClientTag;

    private final CommunicationWrapper communication;

    private DB db;

    UserGestor() throws Exception {
        communication = CommunicationWrapper.getConnection(RABBITMQ_HOST,RABBITMQ_PORT);
        if (communication == null) {
            LOGGER.fatal("Cannot open communication");
            throw new Exception("Cannot open communication");
        }

        if (!communication.queueDeclare(CLIENT_QUEUE)) {
            LOGGER.fatal("Cannot declare queue " + CLIENT_QUEUE);
            communication.close();
            throw new Exception("Cannot declare queue " + CLIENT_QUEUE);
        }

        db = new BlockDatabase();
    }

    private void registerSIGINT() throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    LOGGER.info("SIGINT detected. Closing connection");
                    communication.detach(consumerClientTag);
                    communication.close();
                    LOGGER.info("Connection closed");
                    semaphore.release();
                })
        );
        semaphore.acquire();
    }

    private Message handlerRequestConnection(Message request) {
        LOGGER.info("Request connection from " + request.getUser() + " in radio " + request.getRadio());
        MessageBuilder messageBuilder = new MessageBuilder();
        if (db.existStation(request.getRadio())) {
            if (db.userCanHearRadio(request.getUser(), request.getRadio())) {
                db.addUserInRadio(request.getUser(), request.getUserQueue(), request.getRadio());
                LOGGER.info("Accepted connection from " + request.getUser() + " to radio " + request.getRadio());
                CONNECTION_LOGGER.info(String.format("[CONNECTION] user '%s' to radio '%s'",request.getUser(),request.getRadio()));
                messageBuilder.setType(MessageType.CONNECTION_ACCEPTED);
            } else {
                messageBuilder
                        .setType(MessageType.CONNECTION_DENIED)
                        .setError("You can not listen to more radios with current user").build();
                LOGGER.info("Revoke request connection from " + request.getUser() + " in radio " + request.getRadio());
            }
        } else {
            messageBuilder
                    .setType(MessageType.CONNECTION_DENIED)
                    .setError("Radio no exist or not in transmission").build();
            LOGGER.info("Revoke request connection from " + request.getUser() + ". Radio " + request.getRadio() + " no exist");
        }
        return messageBuilder.build();
    }



    private void start() {
        LOGGER.info("Waiting client request");

        consumerClientTag = communication.append(CLIENT_QUEUE, message -> {
            if (message.getType() == MessageType.REQUEST_CONNECTION) {
                Message response = handlerRequestConnection(message);
                communication.put(message.getUserQueue(), response);
            } else if (message.getType() == MessageType.REQUEST_RADIOS) {
                StringBuilder stationsFlat = new StringBuilder();
                db.getStations().forEach(s -> stationsFlat.append("- ").append(s).append("\n"));
                Message response = new MessageBuilder()
                        .setType(MessageType.RESPONSE_RADIOS)
                        .setInfo(stationsFlat.toString())
                        .build();
                communication.put(message.getUserQueue(),response);
            } else if (message.getType() == MessageType.KEEP_ALIVE) {
                db.updateUserActivity(message.getUser());
                LOGGER.info("Updated activity for user " + message.getUser());
            } else if (message.getType() == MessageType.END_CONNECTION) {
                db.deleteUserFromRadio(message.getUser(), message.getUserQueue(), message.getRadio());
                LOGGER.info(String.format("Deleted user %s from radio %s", message.getUser(), message.getRadio()));
                CONNECTION_LOGGER.info(String.format("[DISCONNECTION] user '%s' to radio '%s'",message.getUser(),message.getRadio()));
            } else {
                LOGGER.warn("Unhandled Message with type " +  message.getType());
            }
        });

        try {
            registerSIGINT();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupt signal");
            LOGGER.debug(e);
        }
        LOGGER.info("Exiting");
    }

    public static void main(String[] argv) {
        UserGestor userGestor = null;
        try {
            userGestor = new UserGestor();
        } catch (Exception e) {
            LOGGER.info("Cannot start user gestor");
            LOGGER.debug(e);
            System.exit(1);
        }
        userGestor.start();
    }
}
