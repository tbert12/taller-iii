import Message.*;
import org.apache.log4j.Logger;
import sun.misc.Signal;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Admin {
    private static final Logger LOGGER = Logger.getLogger(Admin.class);
    private static final Settings SETTINGS = Settings.from("admin.properties");

    private static final String RABBITMQ_HOST       = SETTINGS.get("RABBITMQ_HOST","localhost");
    private static final int RABBITMQ_PORT          = SETTINGS.get("RABBITMQ_PORT",5672);
    private static final String ADMIN_REQ_QUEUE     = SETTINGS.get("ADMIN_REQUEST_QUEUE","ADMIN_REQUEST");
    private static final String ADMIN_RES_QUEUE     = SETTINGS.get("ADMIN_RESPONSE_QUEUE","ADMIN_RESPONSE");
    private static final int REQUEST_POLL_SECONDS   = SETTINGS.get("REQUEST_POLL_SECONDS",10);
    private static final int POOL_SIZE              = SETTINGS.get("POOL_SIZE",5);

    private CommunicationWrapper communication;

    private AtomicBoolean isConnected;
    private String consumerTag;

    private Admin() throws IOException {
        communication = CommunicationWrapper.getConnection(RABBITMQ_HOST,RABBITMQ_PORT);
        if (communication == null) {
            LOGGER.error("Cannot get connection");
            throw new IOException("Cannot connect");
        }
        isConnected = new AtomicBoolean(false);
        consumerTag = null;
    }

    private Timer startScheduledRequests() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Message statsRequest = new MessageBuilder()
                        .setType(MessageType.ADMIN_REQUEST_STATS)
                        .build();
                communication.put(ADMIN_REQ_QUEUE, statsRequest, REQUEST_POLL_SECONDS);
                LOGGER.info("Sent stats request");
            }
        }, 0, (int)TimeUnit.SECONDS.toMillis(REQUEST_POLL_SECONDS));

        return timer;

    }

    private String startResponseListener() {
        return communication.append(ADMIN_RES_QUEUE, res -> {
            isConnected.set(true);
            LOGGER.info("Receive message from " + ADMIN_RES_QUEUE);
            if (res.getType() == MessageType.ADMIN_RESPONSE_STATS) {
                System.out.println(res.getInfo());
            } else {
                LOGGER.warn("Unhandled message type");
            }
        });
    }

    private void start() throws InterruptedException {
        LOGGER.info("Init admin-client");

        LOGGER.info("Starting admin-scheduler collector");

        Timer schedule = startScheduledRequests();

        consumerTag = startResponseListener();

        Semaphore semaphore = new Semaphore(0);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    LOGGER.info("SIGINT detected. Closing Admin-Handler");
                    schedule.cancel();
                    communication.detach(consumerTag);
                    communication.close();
                    LOGGER.info("Admin-Handler closed");
                    semaphore.release();
                    if (LOGGER.isDebugEnabled()) {
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException ignored) {}
                    }
                }
        ));
        semaphore.acquire();
        LOGGER.info("Exiting");
    }

    public static void main(String[] strings) {
        try {
            Admin admin = new Admin();
            admin.start();
        } catch (IOException | InterruptedException e) {
            LOGGER.fatal(e);
            LOGGER.warn("Error. Closed admin");
        }
    }
}
