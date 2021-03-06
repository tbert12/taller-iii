import org.apache.log4j.*;

public abstract class ThreadActivity implements Runnable {
    private Settings settings;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private final WorkExecutor workExecutor;
    ThreadActivity(Settings settings, WorkExecutor workExecutor) {
        this.workExecutor = workExecutor;
        this.settings = settings;
    }

    protected Settings getSettings() {
        return settings;
    }

    protected WorkExecutor getWorkExecutor() {
        return workExecutor;
    }

    protected Logger getLogger() {
        return logger;
    }

    abstract boolean cycle() throws InterruptedException;

    abstract void onStop();

    @Override
    public void run() {
        logger = Logger.getLogger(this.getClass().getName() + "-" + Thread.currentThread().getId());
        Boolean active = true;
        while (active) {
            try {
                active = cycle() && workExecutor.isActive();
            } catch (InterruptedException e) {
                logger.info("InterruptSignal. Stopping work");
                active = false;
            }
        }
        onStop();
        logger.info("OK. Stopped");
    }

}

