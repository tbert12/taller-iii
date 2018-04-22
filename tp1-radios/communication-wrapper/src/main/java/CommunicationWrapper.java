import Message.*;
import com.rabbitmq.client.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class CommunicationWrapper {
    private static final Logger LOGGER = Logger.getLogger(CommunicationWrapper.class);

    static CommunicationWrapper getConnection(String host, int port) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        Connection connection = null;
        Channel channel = null;
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            return new CommunicationWrapper(channel);
        } catch (IOException | TimeoutException e) {
            LOGGER.error("Error: " + e.getMessage());
        }
        return null;
    }

    private final Channel channel;

    CommunicationWrapper(Channel channel) {
        this.channel = channel;
    }

    void close() {
        try {
            Connection connection = channel.getConnection();
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            LOGGER.warn("Cannot close connection " + e.getMessage());
        }
    }

    boolean queueDeclare(String name) {
        try {
            AMQP.Queue.DeclareOk result = channel.queueDeclare(name, true, false, false, null);
            LOGGER.info("Created queue " + result.getQueue());
            return true;
        } catch (IOException e) {
            LOGGER.warn("Cannot declare queue " + name + ". " + e.getMessage());
            return false;
        }
    }

    public String queueDeclare() {
        try {
            AMQP.Queue.DeclareOk result = channel.queueDeclare();
            LOGGER.info("Created queue " + result.getQueue());
            return result.getQueue();
        } catch (IOException e) {
            LOGGER.warn("Cannot declare queue. " + e.getMessage());
            return null;
        }
    }

    private boolean put(String queue, Message message, AMQP.BasicProperties props) {
        try {
            channel.basicPublish("", queue, null, message.toBytes());
            LOGGER.debug("Send message [" + message.toString().hashCode() + "] on queue " + queue);
        } catch (IOException e) {
            LOGGER.warn("Cannot put message in " + queue + ". " + e.getMessage());
            return false;
        }
        return true;
    }

    boolean put(String queue, Message message, int expiration_seconds) {
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .expiration(String.valueOf(expiration_seconds * 1000))
                .build();
        return put(queue, message, props);
    }

    boolean put(String queue, Message message) {
        return put(queue, message, Integer.MAX_VALUE);
    }

    String append(String queue, Consumer<Message> handlerFunction) {
        try {
            return channel.basicConsume(queue, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope env, AMQP.BasicProperties props, byte[] body) {
                    try {
                        Message message = new Message(body);
                        LOGGER.debug("Receive message [" + message.toString().hashCode() + "] from queue " + queue);
                        handlerFunction.accept(message);
                        channel.basicAck(env.getDeliveryTag(), false);
                    } catch (IOException  e) {
                        LOGGER.debug(e);
                    }
                }
            });
        } catch (IOException e) {
            LOGGER.error("Error on append in " + queue);
            LOGGER.debug(e);
            return null;
        }
    }

    boolean detach(String consumerTag) {
        try {
            channel.basicCancel(consumerTag);
            return true;
        } catch (IOException e) {
            LOGGER.warn("Cannot detach consumerTag: " + consumerTag);
            LOGGER.debug(e);
            return false;
        }
    }

    void deleteQueue(String queueName) {
        try {
            channel.queueDeleteNoWait(queueName, false, false);
            LOGGER.info("Deleted queue " + queueName);
        } catch (IOException e) {
            LOGGER.warn("Cannot delete queue: " + queueName);
            LOGGER.debug(e);
        }
    }



}
