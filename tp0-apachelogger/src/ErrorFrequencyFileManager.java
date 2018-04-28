import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;

import org.apache.log4j.*;

public class ErrorFrequencyFileManager {
    private static final Logger LOGGER = Logger.getLogger(ErrorFrequencyFileManager.class);
    private volatile ArrayList<Object> filesMutex;
    private final String workingDir;
    ErrorFrequencyFileManager(String workingDir, int maxFiles) throws IOException {
        Logger logger = Logger.getLogger(ErrorFrequencyFileManager.class);
        this.workingDir = workingDir;
        if (createPath()) {
            logger.info("Created working directory (" + workingDir + ")");
        } else {
            logger.fatal("Cannot craeate working directory (" + workingDir + ").");
            throw new IOException("Cannot create directory");
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
                    LOGGER.debug("Work returned `False`");
                }
            } catch (Exception e) {
                LOGGER.warn("Exception when working over file",e);
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
