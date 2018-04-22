import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.apache.log4j.*;

public class ErrorFrequencyFileManager {
    private volatile ArrayList<Object> filesMutex;
    private final String workingDir;
    ErrorFrequencyFileManager(String workingDir, int maxFiles) {
        Logger logger = Logger.getLogger(ErrorFrequencyFileManager.class);
        this.workingDir = workingDir;
        if (createPath()) {
            logger.info("Created working directory (" + workingDir + ")");
        } else {
            logger.fatal("Cannot craeate working directory (" + workingDir + ").");
            System.exit(1);
        }
        filesMutex = new ArrayList<>();
        for (int i = 0; i < maxFiles; i++) {
            filesMutex.add( new Object() );
        }
    }

    private boolean createPath() {
        File workingDirectory = new File(workingDir);
        return workingDirectory.exists() || workingDirectory.mkdir();
    }

    private synchronized Object getFileMutex(int fileId) {
        return filesMutex.get(fileId);
    }

    private synchronized int getNumberOfFiles() {
        return filesMutex.size();
    }

    private void workOverFile(int fileId, Function<String, Boolean> work) {
        String fileName = String.valueOf(fileId) + ".err";
        synchronized (getFileMutex(fileId)) {
            try {
                Boolean result = work.apply(workingDir + fileName);
                if (!result) {
                    Logger.getLogger(ErrorFrequencyFileManager.class).debug("Work returned `False`");
                }
            } catch (Exception e) {
                Logger.getLogger(ErrorFrequencyFileManager.class).warn("Exception when working over file",e);
            }
        }
    }

    public void workOverFile(String error, Function<String, Boolean> work) {
        int fileId = Math.abs(error.hashCode()) % getNumberOfFiles();
        workOverFile(fileId, work);
    }

    public void workOverFiles(Function<String, Boolean> work) {
        for (int i = 0; i < getNumberOfFiles(); i++) {
            workOverFile(i, work);
        }
    }
}
