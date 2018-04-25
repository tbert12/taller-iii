import Wrapper.ApacheLogEntry;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;

public class ErrorHandler extends ThreadActivity {
    private final ArrayBlockingQueue<ApacheLogEntry> parserQueue;
    private final ArrayBlockingQueue<ApacheLogEntry> errorFrequencyQueue;
    private final PrintWriter errorWriter;

    ErrorHandler(ArrayBlockingQueue<ApacheLogEntry> parserQueue, ArrayBlockingQueue<ApacheLogEntry> errorFreqQueue,
                 Settings settings, WorkExecutor workExecutor) throws IOException {
        super(settings, workExecutor);
        this.parserQueue = parserQueue;
        this.errorFrequencyQueue = errorFreqQueue;
        errorWriter = new PrintWriter(new FileWriter(super.getSettings().errorLogFile()));
    }

    private void writeError(String errorLineLog) {
        errorWriter.write(errorLineLog);
        errorWriter.write("\n");
        if (errorWriter.checkError()) {
            super.getLogger().warn("Error on write line in apache_error.log");
        }
    }

    @Override
    boolean cycle() throws InterruptedException {
        /*
            1. Read Parser Queue
            2. Write error in error_log
            3. Send Error to ErrorFrequency
        */
        ApacheLogEntry errorLineLog = parserQueue.take();
        writeError(errorLineLog.getRawLine());
        errorFrequencyQueue.put(errorLineLog);
        return true;
    }

    @Override
    void onStop() {
        errorWriter.close();
    }
}
