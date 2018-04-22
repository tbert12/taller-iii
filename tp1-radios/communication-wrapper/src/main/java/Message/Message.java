package Message;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Message {
    private static final JSONParser PARSER = new JSONParser();

    private String raw;
    private JSONObject json;

    public Message(String rawJSON) throws MessageException {
        try {
            json = (JSONObject) PARSER.parse(rawJSON);
            raw = rawJSON;
        } catch (ParseException e) {
            throw new MessageException("Invalid JSON string", e);
        }
    }

    public Message(byte[] bytes) throws MessageException {
        try {
            raw = new String(bytes, UTF_8);
            json = (JSONObject) PARSER.parse(raw);
        } catch (ParseException e) {
            throw new MessageException("Invalid Byte data", e);
        }
    }

    public Message(JSONObject json) {
        this.json = json;
        this.raw = json.toJSONString();
    }


    public MessageType getType() {
        Long type = (Long) json.get("type");
        return MessageType.from(type.intValue());
    }

    public byte[] getPayload() {
        String encoded = json.get("payload").toString();
        return Base64.getDecoder().decode(encoded);
    }

    public String getStringPayload() {
        return new String(getPayload(), UTF_8);
    }

    public String getContentType() {
        return json.get("content_type").toString();
    }

    public byte[] toBytes() {
        return raw.getBytes();
    }

    public String toString() {
        return raw;
    }


    public String getRadio() {
        return json.get("radio").toString();
    }

    public String getError() {
        return json.get("error").toString();
    }

    public String getUserQueue() {
        return json.get("user_queue").toString();
    }

    public String getUser() {
        return json.get("user").toString();
    }

    public String getInfo() {
        return json.get("info").toString();
    }
}

