import Message.*;

import org.apache.log4j.*;
import sun.misc.Signal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RadioListener {
    private static final Logger LOGGER = Logger.getLogger(RadioListener.class);
    private static final Settings SETTINGS = Settings.from("radio-listener.properties");

    private static final String RABBITMQ_HOST   = SETTINGS.get("RABBITMQ_HOST","localhost");
    private static final int RABBITMQ_PORT      = SETTINGS.get("RABBITMQ_PORT",5672);
    private static final String CLIENT_QUEUE    = SETTINGS.get("CLIENT_QUEUE","CLIENT");
    private static final int TIMEOUT_SECONDS    = SETTINGS.get("TIMEOUT_SECONDS",10);
    private static final int KEEP_ALIVE_POLL_SECONDS  = SETTINGS.get("KEEP_ALIVE_POLL_SECONDS",60);
    private static final int POOL_SIZE = SETTINGS.get("POOL_SIZE",10);


    private final String user;
    private final String radio;
    private File streamFile;

    private CommunicationWrapper comm;
    private String consumerTag;
    private String listenQueue;
    private AtomicBoolean isConnected;

    private ScheduledExecutorService keepAliveScheduler;
    private ScheduledExecutorService connectedScheduler;


    private void initCommunication() throws IOException {
        comm = CommunicationWrapper.getConnection(RABBITMQ_HOST, RABBITMQ_PORT);
        if (comm == null) {
            LOGGER.fatal("Cannot open connection. Server is down");
            throw new IOException("Cannot open connection. Server is up?");
        }

        listenQueue = comm.queueDeclare();
        if (listenQueue == null) {
            throw new IOException("Cannot declare queue to receive response");
        }
        isConnected = new AtomicBoolean(false);
    }

    private RadioListener() throws IOException {
        initCommunication();
        this.user = null;
        this.radio = null;
        this.keepAliveScheduler = null;
        this.connectedScheduler = null;
        this.streamFile = null;
    }

    private RadioListener(String user, String radio) throws IOException {
        initCommunication();
        this.user = user;
        this.radio = radio;
        this.keepAliveScheduler = Executors.newScheduledThreadPool(POOL_SIZE);

    }

    private File createFile(String extension) throws IOException {
        String fileName = this.user + "-" + this.radio + "." + extension;
        File fileStream = new File(fileName);
        int i = 1;
        while (fileStream.exists()) {
            fileName = this.user + "-" + this.radio + "-" + i + "." + extension;
            fileStream = new File(fileName);
            i++;
        }
        if (!fileStream.createNewFile()) {
            if (!fileStream.exists()) {
                throw new IOException("Failed on create file to write");
            }
        }
        LOGGER.info("Created file " + fileName + " to store radio packages");
        this.streamFile = fileStream;
        return fileStream;
    }

    private void handleRadioPackage(Message message) {
        FileOutputStream out;
        try {
            File fileStream = this.streamFile == null ? createFile(message.getContentType()) : this.streamFile;
            out = new FileOutputStream(fileStream, true);
            byte[] bytes = message.getPayload();
            LOGGER.debug("Write " + bytes.length + " in " + fileStream.getName());
            out.write(bytes);
            out.close();
        } catch (IOException e) {
            LOGGER.warn("Cannot write radio package. Ignoring it");
            LOGGER.debug(e);
        }
    }

    private void handleResponse(Message res) {
        if (res.getType() == MessageType.CONNECTION_ACCEPTED) {
            LOGGER.info("Receive Connection accepted");
            isConnected.set(true);
            startSchedulerToSendKeepAlive();
            System.out.println("Connected to radio '" + radio + "'");
        }
        if (res.getType() == MessageType.CONNECTION_DENIED) {
            LOGGER.info("Connection denied");
            System.out.println("Cannot connect with radio. Error: \"" + res.getError() + "\"");
        }
        if (res.getType() == MessageType.RADIO_PACKAGE) {
            LOGGER.info("Receive radio package");
            handleRadioPackage(res);
        }
        if (res.getType() == MessageType.END_CONNECTION) {
            LOGGER.info("Receive end connection");
            stop();
        }
    }

    private void startSchedulerToSendKeepAlive() {
        keepAliveScheduler.scheduleAtFixedRate(() -> {
            LOGGER.info("Send keep alive to server");
            Message message = new MessageBuilder()
                    .setType(MessageType.KEEP_ALIVE)
                    .setUser(user)
                    .build();
            comm.put(CLIENT_QUEUE, message);
        }, KEEP_ALIVE_POLL_SECONDS, KEEP_ALIVE_POLL_SECONDS, TimeUnit.SECONDS);
    }

    private void disconnect() {
        comm.put(CLIENT_QUEUE, new MessageBuilder()
                .setUser(user)
                .setRadio(radio)
                .setClientQueue(listenQueue)
                .setType(MessageType.END_CONNECTION).build()
        );
    }

    private synchronized void stop() {
        if (isConnected.compareAndSet(true, false)) {
            disconnect();
        }
        if (keepAliveScheduler != null) {
            keepAliveScheduler.shutdownNow();
            keepAliveScheduler = null;
        }
        if (connectedScheduler != null) {
            connectedScheduler.shutdown();
            connectedScheduler = null;
        }
        if (!comm.detach(consumerTag)) {
            LOGGER.warn("Cannot detach");
        }
        if (listenQueue != null) {
            comm.deleteQueue(listenQueue);
        }
        if (comm != null) {
            comm.close();
            comm = null;
        }
        LOGGER.info("Exit");
    }

    private void waitResponseWithTimeout(Consumer<Message> handler) {
        connectedScheduler = Executors.newScheduledThreadPool(POOL_SIZE);
        connectedScheduler.schedule(() -> {}, Long.MAX_VALUE, TimeUnit.DAYS);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(POOL_SIZE);
        LOGGER.info("Start timeout to wait response. TIMEOUT=" + TIMEOUT_SECONDS);
        scheduler.schedule(() -> {
                    LOGGER.info("TIMEOUT: Wake-up. Close connection. The servers are not working");
                    connectedScheduler.shutdownNow();
                }, TIMEOUT_SECONDS, TimeUnit.SECONDS
        );

        // Wait responses
        LOGGER.info("Waiting response");
        consumerTag = comm.append(listenQueue, res -> {
            handler.accept(res);
            scheduler.shutdownNow();
        });

        scheduler.shutdown();
        connectedScheduler.shutdown();
        try {
            scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            connectedScheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ignored) {
            LOGGER.info("Interrupted Exception");
        }
    }

    private void startListener() {

        Message request = new MessageBuilder().setType(MessageType.REQUEST_CONNECTION)
                .setClientQueue(listenQueue)
                .setRadio(radio)
                .setUser(user)
                .build();
        comm.put(CLIENT_QUEUE, request, TIMEOUT_SECONDS);

        waitResponseWithTimeout(this::handleResponse);

        if (!isConnected.get()) {
            stop();
        }
    }

    private void listRadios() {

        // Send request
        Message request = new MessageBuilder().
                setType(MessageType.REQUEST_RADIOS)
                .setClientQueue(listenQueue)
                .build();
        comm.put(CLIENT_QUEUE, request, TIMEOUT_SECONDS);


        waitResponseWithTimeout(res -> {
            LOGGER.info("Received response from server");
            if (res.getType() == MessageType.RESPONSE_RADIOS) {
                System.out.println("Radios:\n" + res.getInfo());
            }
            if (this.connectedScheduler != null) {
                this.connectedScheduler.shutdownNow();
            }
        });

        stop();

    }

    private void start() {
        if (this.user == null) {
            listRadios();
        } else {
            startListener();
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(" - Listen Radio: ./radio-listener <<user>> <<radio>>");
            System.out.println(" - List Radios: ./radio-listener list");
            return;
        }

        boolean listRadios = (args.length == 1 && args[0].toLowerCase().equals("list"));
        try {
            RadioListener radioListener = (listRadios) ? new RadioListener() : new RadioListener(args[0], args[1]);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("SIGINT detected. Closing connection");
                if (radioListener != null) {
                    radioListener.stop();
                }
                LOGGER.info("Connection closed");
            }));
            radioListener.start();
            LOGGER.info("Goodbye");
        } catch (IOException e) {
            LOGGER.fatal("Cannot start radio-listener");
            LOGGER.debug(e);
        }

    }


}
