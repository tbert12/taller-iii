import Message.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Station {
    private static final Logger LOGGER = Logger.getLogger(Station.class);
    private static final Settings SETTINGS = Settings.from("station.properties");

    private static final String RADIO_QUEUE = SETTINGS.get("RADIO_QUEUE","RADIO");
    private static final String RABBITMQ_HOST = SETTINGS.get("RABBITMQ_HOST","localhost");
    private static final int RABBITMQ_PORT = SETTINGS.get("RABBITMQ_PORT",5672);
    private static final int MESSAGE_EXPIRATION_TIME_SECONDS = SETTINGS.get("MESSAGE_EXPIRATION_TIME_SECONDS",60);
    private static final int PACKAGE_BYTE_SIZE = SETTINGS.get("PACKAGE_BYTE_SIZE",192000);
    private static final int TIME_PER_PACKAGE_SECONDS = SETTINGS.get("TIME_PER_PACKAGE_SECONDS",2);

    private final CommunicationWrapper communication;
    private final String name;
    private AtomicBoolean transmissionStarted;

    private Station(String name) throws Exception {
        communication = CommunicationWrapper.getConnection(RABBITMQ_HOST, RABBITMQ_PORT);
        if (communication == null) {
            LOGGER.warn("Cannot establish communication");
            throw new Exception("Cannot establish communication");

        }

        if (!communication.queueDeclare(RADIO_QUEUE)) {
            LOGGER.warn("Cannot declare Queue " + RADIO_QUEUE);
            throw new Exception("Cannot declare Queue " + RADIO_QUEUE);
        }

        this.name = name;
        this.transmissionStarted = new AtomicBoolean(false);

    }

    private void startTransmission(String filePath) throws InterruptedException {
        File file = new File(filePath);
        try (FileInputStream fis = new FileInputStream(file)) {
            String fileExtension = filePath.substring(filePath.lastIndexOf('.') + 1);
            int totalBytes = fis.available();
            LOGGER.info("Attempt to send " + file.getName() + ". Content type " + fileExtension);
            LOGGER.info("Total bytes to send " + totalBytes);
            int byteCountRead = 0;
            int byteCount;
            byte[] bytes = new byte[PACKAGE_BYTE_SIZE];
            transmissionStarted.set(true);
            while ((byteCount = fis.read(bytes)) != -1) {
                byteCountRead+= byteCount;
                Message message = new MessageBuilder()
                        .setType(MessageType.RADIO_PACKAGE)
                        .setRadio(name)
                        .setContentType(fileExtension)
                        .setPayload(Arrays.copyOfRange(bytes,0,byteCount))
                        .build();
                communication.put(RADIO_QUEUE, message,MESSAGE_EXPIRATION_TIME_SECONDS);
                LOGGER.debug("ByteCount " + byteCount + ". Left: " + (totalBytes - byteCountRead));
                LOGGER.info("Sent " + byteCount + " bytes. " + (byteCountRead*100)/totalBytes + "%");

                TimeUnit.SECONDS.sleep(TIME_PER_PACKAGE_SECONDS);
            }
            LOGGER.info("End stream");
        } catch (IOException e) {
            LOGGER.debug(e);
            LOGGER.warn("Cannot read file stream " + filePath);
        }
    }

    private void stopTransmission() {
        if (transmissionStarted.compareAndSet(true, false)) {
            LOGGER.info("Closing connection (send end transmission message");
            Message endMessage = new MessageBuilder()
                    .setType(MessageType.END_TRANSMISSION)
                    .setRadio(name)
                    .build();
            communication.put(RADIO_QUEUE, endMessage,MESSAGE_EXPIRATION_TIME_SECONDS);
            communication.close();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Use: ./station <<name>> <<file>>");
            LOGGER.fatal("Invalid parameters");
            return;
        }

        try {
            Station station = new Station(args[0]);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        LOGGER.info("SIGINT detected. Closing Station");
                        station.stopTransmission();
                        if (LOGGER.isDebugEnabled()) {
                            try {
                                TimeUnit.SECONDS.sleep(1);
                            } catch (InterruptedException ignored) {}
                        }
                    })
            );
            station.startTransmission(args[1]);
            station.stopTransmission();
        } catch (Exception e) {
            LOGGER.debug(e);
            LOGGER.warn("Cannot create station");
        }
    }
}
