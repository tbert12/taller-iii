import Wrapper.ApacheLogEntry;

import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

public class Reader extends ThreadActivity {
    private final ArrayBlockingQueue<ApacheLogEntry> parserQueue;
    private final ArrayBlockingQueue<ApacheLogEntry> dumperQueue;
    private final Scanner stdinScanner;
    Reader(ArrayBlockingQueue<ApacheLogEntry> parserQueue, ArrayBlockingQueue<ApacheLogEntry> dumperQueue,
           Settings settings, WorkExecutor workExecutor) {
        super(settings,workExecutor);
        this.parserQueue = parserQueue;
        this.dumperQueue = dumperQueue;
        this.stdinScanner = new Scanner(settings.getInputReader());
    }

    @Override
    boolean cycle() throws InterruptedException {
        /*
        1. Read STDIN
        1.2 detect if EOF -> gracefull quit
        2. Detect if is Apache Log
            2.1. Send to Parser
            2.2. Send to Logger clon
        */
        if (!stdinScanner.hasNextLine()) {
            super.getLogger().info("EOF Detected. Closing...");
            super.getWorkExecutor().end();
            return false;
        }
        String log = stdinScanner.nextLine();
        ApacheLogEntry logEntry = ApacheLogEntry.from(log);
        if (logEntry != null) {
            parserQueue.put(logEntry);
            dumperQueue.put(new ApacheLogEntry(logEntry));
        } else {
            super.getLogger().info("Ignoring line\n'" + log + "'");
        }
        return true;
    }

    @Override
    void onStop() {
        stdinScanner.close();
    }
}
