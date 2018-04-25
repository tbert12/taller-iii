import Wrapper.ApacheLogEntry;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;

public class Dumper extends ThreadActivity {
    private final ArrayBlockingQueue<ApacheLogEntry> readerQueue;
    private final PrintWriter dumperFile;

    Dumper(ArrayBlockingQueue<ApacheLogEntry> readerQueue, Settings settings, WorkExecutor workExecutor) throws IOException {
        super(settings, workExecutor);
        this.readerQueue = readerQueue;
        dumperFile = new PrintWriter(new FileWriter(super.getSettings().dumperLogFile()));
    }

    private void writeLine(String line) {
        dumperFile.write(line);
        dumperFile.write("\n");
        if (dumperFile.checkError()) {
            super.getLogger().warn("Error on write line");
        }
    }

    @Override
    boolean cycle() throws InterruptedException {
        writeLine(readerQueue.take().getRawLine());
        return true;
    }

    @Override
    void onStop() {
        dumperFile.close();
    }
}
