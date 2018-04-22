package Message;

import org.json.simple.JSONObject;

import java.util.Base64;


@SuppressWarnings("unchecked")
public class MessageBuilder {
    private final JSONObject messageData;

    public MessageBuilder() {
        messageData = new JSONObject();
    }

    public MessageBuilder setType(MessageType type) {
        messageData.put("type", type.getValue());
        return this;
    }

    public MessageBuilder setPayload(String payload) {
        messageData.put("payload",payload);
        return this;
    }

    public MessageBuilder setPayload(byte[] bytes) {
        return setPayload(Base64.getEncoder().encodeToString(bytes));
    }


    public MessageBuilder setClientQueue(String clientQueue) {
        messageData.put("user_queue", clientQueue);
        return this;
    }

    public MessageBuilder setRadio(String radio) {
        messageData.put("radio", radio);
        return this;
    }

    public MessageBuilder setUser(String user) {
        messageData.put("user", user);
        return this;
    }

    public MessageBuilder setContentType(String contentType) {
        messageData.put("content_type", contentType);
        return this;
    }

    public MessageBuilder setError(String error) {
        messageData.put("error", error);
        return this;
    }

    public MessageBuilder setInfo(String info) {
        messageData.put("info", info);
        return this;
    }

    public Message build() {
        return new Message(messageData);
    }
}
