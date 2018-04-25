import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.log4j.*;

public class WorkExecutor {
    private volatile ArrayList<Thread> workers;
    private volatile boolean active = false;
    WorkExecutor() {
        workers = new ArrayList<>();
    }

    public synchronized boolean isActive() {
        return active;
    }

    public synchronized void startWork() {
        active = true;
        workers.forEach(Thread::start);
    }

    public synchronized void end() {
        active = false;
        workers.forEach(Thread::interrupt);
    }

    public void addWorker(Runnable runnableWorker) {
        workers.add(new Thread(runnableWorker));
    }

    public void addWorker(Callable<Runnable> runneableCreator, int clons) throws Exception {
        for (int i = 0; i < clons; i++) {
            Runnable runnable = runneableCreator.call();
            addWorker(runnable);
        }
    }

    public void join() {
        workers.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                Logger.getLogger(WorkExecutor.class).warn("InterruptedException on join " + t.getName());
            }
        });
    }
}
