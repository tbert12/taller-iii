import Wrapper.ApacheLogEntry;
import org.apache.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;

public class StatsRegister extends ThreadActivity {
    private final ArrayBlockingQueue<ApacheLogEntry> parserQueue;
    private final Stats stats;
    StatsRegister(ArrayBlockingQueue<ApacheLogEntry> parserQueue, Settings settings,
                  Stats stats, WorkExecutor workExecutor) {
        super(settings, workExecutor);
        this.parserQueue = parserQueue;
        this.stats = stats;
    }

    @Override
    boolean cycle() throws InterruptedException {
        /*
            * 1. Read Parser queue
            * 2. Store stat in shared memory
         */
        ApacheLogEntry logLine = parserQueue.take();
        stats.add(logLine);
        return true;
    }

    @Override
    void onStop() {
    }
}
