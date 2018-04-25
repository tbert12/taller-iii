import Wrapper.ApacheLogEntry;
import org.apache.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;

class Parser extends ThreadActivity {
    private final ArrayBlockingQueue<ApacheLogEntry> readerQueue;
    private final ArrayBlockingQueue<ApacheLogEntry> statsQueue;
    private final ArrayBlockingQueue<ApacheLogEntry> errorHandlerQueue;

    Parser(ArrayBlockingQueue<ApacheLogEntry> readerQueue, ArrayBlockingQueue<ApacheLogEntry> statsQueue,
           ArrayBlockingQueue<ApacheLogEntry> errorHandlerQueue, Settings settings, WorkExecutor workExecutor) {
        super(settings, workExecutor);
        this.readerQueue = readerQueue;
        this.statsQueue = statsQueue;
        this.errorHandlerQueue = errorHandlerQueue;
    }

    @Override
    boolean cycle() throws InterruptedException {
        ApacheLogEntry apacheLogEntry = new ApacheLogEntry(readerQueue.take());
        statsQueue.put(apacheLogEntry);
        if (apacheLogEntry.isError()) {
            errorHandlerQueue.put(apacheLogEntry);
        }
        return true;
    }

    @Override
    void onStop() {
    }
}
