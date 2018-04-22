import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public void addWorker(Callable<Runnable> runneableCreator, int clons) {
        for (int i = 0; i < clons; i++) {
            try {
                Runnable runnable = runneableCreator.call();
                addWorker(runnable);
            } catch (Exception e) {
                Logger.getLogger(WorkExecutor.class).warn("Cannot create Runnable to append in work executor. Ignoring it.");
            }

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
