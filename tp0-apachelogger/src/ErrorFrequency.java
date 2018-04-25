import Wrapper.ApacheLogEntry;

import java.io.File;
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

    private HashMap<String,Integer> readFileErrorIfExist(String fileName) {
        HashMap<String,Integer> errors = new HashMap<>();
        try {
            Files.readAllLines(Paths.get(fileName)).forEach(s -> {
                String[] parts = s.split("==", 2);
                errors.put(parts[1].trim(), Integer.parseInt(parts[0].trim()));
            });
        } catch (IOException e) {
            super.getLogger().debug(e);
            super.getLogger().warn("Cannot read " + fileName + ". " + e.getMessage());
            File file = new File(fileName);
            if (file.exists()) { // Files.readAlLines create empty file
                super.getLogger().info(fileName + " has been created");
            }
        }
        return errors;
    }

    private boolean writeErrorCountInFile(HashMap<String, Integer> errorCount, String fileName) {
        try ( PrintWriter writer = new PrintWriter(new FileWriter(fileName)) ) {
            errorCount.forEach((key, value) -> writer.write(value + "==" + key + "\n"));
            super.getLogger().debug("Updated errors in " + fileName);
        } catch (IOException e) {
            super.getLogger().fatal("Cannot write in file [" + fileName + "]. Ignoring error log entry", e);
            super.getWorkExecutor().end();
            return false;
        }
        return true;
    }

    private void registerLog(ApacheLogEntry apacheLog) {
        String error = apacheLog.getError().trim();
        HashMap<String,Integer> errorCount = new HashMap<>();
        super.getLogger().debug("Before take lock");
        fileManager.workOverFile(error, f -> {
            super.getLogger().debug("Take lock " + f);
            errorCount.putAll(readFileErrorIfExist(f));
            Integer actualErrorCount = errorCount.get(error);
            errorCount.put(error, actualErrorCount == null ? 1 : actualErrorCount + 1);
            boolean result = writeErrorCountInFile(errorCount, f);
            super.getLogger().debug("Free " + f);
            return result;
        });
        super.getLogger().debug("After work over the file (end error register)");
    }

    @Override
    void onStop() {
    }
}
