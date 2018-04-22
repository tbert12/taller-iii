import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.log4j.*;

public class ErrorFrequencyCollector extends ThreadActivity {
    private final ErrorFrequencyFileManager fileManager;
    private final String workingDir;
    private final String outputLog;
    private final int pollSeconds;
    ErrorFrequencyCollector(ErrorFrequencyFileManager fileManager, Settings settings, WorkExecutor workExecutor) {
        super(settings, workExecutor);
        this.fileManager = fileManager;
        this.workingDir = settings.errorFrequencyWorkDir();
        this.outputLog = settings.errorFrequencyFile();
        this.pollSeconds = settings.errorFrequencyPoll();
    }

    @Override
    boolean cycle() throws InterruptedException {
        /*
            1. Sleep 3 minutes
            2. Collect all errors in hashmap
            3. Store errors in file 'error_frequency.log'
         */
        ArrayList<String> errorFrequencyDump = new ArrayList<>();
        collectErrors().entrySet().stream()
                .sorted((e1,e2) -> -e1.getValue().compareTo(e2.getValue()))
                .forEach(e -> errorFrequencyDump.add(e.getValue() + " | " + e.getKey()));
        generateTopErrorFile(errorFrequencyDump);
        Thread.sleep(1000 * pollSeconds);
        return true;
    }

    private void generateTopErrorFile(List<String> errors) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(outputLog));
            for (String line : errors) {
                writer.write(line + "\n");
            }
            writer.close();
            logger.info("Generated " + outputLog);
        } catch (IOException e) {

            logger.warn("Cannot write in file [" + outputLog + "]");
            logger.debug(e);
        }
    }

    private List<String> readFile(String path) {
        try {
            if (new File(path).exists()) {
                return Files.readAllLines(Paths.get(path));
            }
        } catch (IOException e) {
            logger.warn("Cannot read file [" + path + "] to collect frequencies");
        }
        return new ArrayList<>();
    }

    private HashMap<String,Integer> collectErrors() {
        HashMap<String, Integer> errorCount = new HashMap<>();
        fileManager.workOverFiles( fileName -> {
            logger.debug("Take lock on " + fileName);
            readFile(fileName).forEach(s -> {
                String[] parts = s.split("==", 2);
                errorCount.put(parts[1], Integer.parseInt(parts[0].trim()));
            });
            logger.debug("Free " + fileName);
            return true;
        });
        return errorCount;

    }

    @Override
    void onStop() {
    }
}
