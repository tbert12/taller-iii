package Message;

import java.io.IOException;

public class MessageException extends IOException {
    public MessageException() { super(); }
    public MessageException(String message) { super(message); }
    public MessageException(String message, Throwable cause) { super(message, cause); }
    public MessageException(Throwable cause) { super(cause); }
}
