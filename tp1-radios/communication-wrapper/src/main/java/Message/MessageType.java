package Message;

public enum MessageType {
    REQUEST_RADIOS(0),
    RESPONSE_RADIOS(1),
    REQUEST_CONNECTION(2),
    CONNECTION_ACCEPTED(3),
    CONNECTION_DENIED(4),
    RADIO_PACKAGE(5),
    KEEP_ALIVE(6),
    END_TRANSMISSION(7),
    END_CONNECTION(8),
    ADMIN_REQUEST_STATS(9),
    ADMIN_RESPONSE_STATS(10),
    INVALID(-1); //Default MessageType


    public static MessageType from(int x) {
        MessageType[] values = MessageType.values();
        if (x >= values.length) {
            return INVALID;
        }
        return values[x];
    }

    private final int value;
    MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
