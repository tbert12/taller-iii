

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {
    private static String printThread() {
        return Thread.currentThread().getName();
    }

    private static void testExecutors() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        Object mutex = new Object();

        Callable<Integer> task = () -> {
            synchronized (mutex) {
                System.out.println(printThread() + " Tomo mutex");
                TimeUnit.SECONDS.sleep(3);
                System.out.println(printThread() + " Libero mutex");
            }
            return 1;
        };

        List<Callable<Integer>> runnables = new ArrayList<>();
        while (runnables.size() < 10) {
            runnables.add(task);
        }

        executor.invokeAll(runnables);
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        System.out.println(printThread() + " Hilo principal!");
    }


    private static void testFileLock() {
        try {
            FileCellBlock file = new FileCellBlock("dummydb", 100);
            file.insert("1");
            file.insert("2");
            file.insert("3");
            file.iterFile(System.out::println);
            file.delete("1", String::compareTo);
            System.out.println("-----");
            file.iterFile(System.out::println);
            System.out.println("-----");
            file.insert("4");
            file.iterFile(System.out::println);
            System.out.println(file.find(s -> true).size());
            file.clean();
        } catch (IOException | IndexOutOfBoundsException e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) throws InterruptedException {
        //testExecutors();
        testFileLock();
        System.exit(0);
    }
}
