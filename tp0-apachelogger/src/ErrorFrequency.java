import Wrapper.ApacheLogEntry;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class ErrorFrequency extends ThreadActivity {
    private final ArrayBlockingQueue<ApacheLogEntry> errorHandlerQueue;
    private final ErrorFrequencyFileManager fileManager;
    ErrorFrequency(ArrayBlockingQueue<ApacheLogEntry> errorHandlerQueue, ErrorFrequencyFileManager fileManager,
                   Settings settings, WorkExecutor workExecutor) {
        super(settings, workExecutor);
        this.fileManager = fileManager;
        this.errorHandlerQueue = errorHandlerQueue;
    }

    @Override
    boolean cycle() throws InterruptedException {
        /*
            1. Read Error handler Queue
            2. Write error file (name is hashed)
                ej. File: co.err (al errors who start with co)
                2.1. If file not exist. Create
                2.2. If file exist update (if exist in file) or append error.
        */
        registerLog(errorHandlerQueue.take());
        return true;
    }

    private HashMap<String,Integer> readFileError(String fileName) {
        HashMap<String,Integer> errors = new HashMap<>();
        try {
            Files.readAllLines(Paths.get(fileName)).forEach(s -> {
                String[] parts = s.split("==", 2);
                errors.put(parts[1].trim(), Integer.parseInt(parts[0].trim()));
            });
        } catch (IOException e) {
            logger.debug(e);
            logger.warn(fileName + " not exist. It will be created");
        }
        return errors;
    }

    private boolean writeErrorCountInFile(HashMap<String, Integer> errorCount, String fileName) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(fileName));
            errorCount.forEach((key, value) -> writer.write(value + "==" + key + "\n"));
            writer.close();
            logger.debug("Updated errors in " + fileName);
        } catch (IOException e) {
            logger.warn("Cannot write in file [" + fileName + "]. Ignoring error log entry");
            logger.debug(e);
            return false;
        }
        return true;
    }

    private void registerLog(ApacheLogEntry apacheLog) {
        String error = apacheLog.getError().trim();
        HashMap<String,Integer> errorCount = new HashMap<>();
        logger.debug("Before take lock");
        fileManager.workOverFile(error, f -> {
            logger.debug("Take lock " + f);
            errorCount.putAll(readFileError(f));
            Integer actualErrorCount = errorCount.get(error);
            errorCount.put(error, actualErrorCount == null ? 1 : actualErrorCount + 1);
            boolean result = writeErrorCountInFile(errorCount, f);
            logger.debug("Free " + f);
            return result;
        });
        logger.debug("After work over the file (end error register)");
    }

    @Override
    void onStop() {
    }
}
