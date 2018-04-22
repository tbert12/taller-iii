import Wrapper.ApacheLogEntry;
import sun.misc.Signal;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.*;


public class Main {
    public static void main(String[] args)  {
        Settings settings = Settings.fromProperties("config.properties");
        WorkExecutor workExecutor = new WorkExecutor();

        PropertyConfigurator.configure("log4j.properties");
        Logger logger = Logger.getLogger(Main.class);

        // 0. Signal handler to detect CTRL-C and do graceful quit
        Signal.handle(new Signal("INT"), sig -> {
            logger.info("CTRL+C Detected. Closing...");
            workExecutor.end();
        });

        // 1. Create Queues
        int queueCapacity = settings.queueSize();
        ArrayBlockingQueue<ApacheLogEntry> parserQueue           = new ArrayBlockingQueue<>(queueCapacity);
        ArrayBlockingQueue<ApacheLogEntry> dumperQueue           = new ArrayBlockingQueue<>(queueCapacity);
        ArrayBlockingQueue<ApacheLogEntry> errorHandlerQueue     = new ArrayBlockingQueue<>(queueCapacity);
        ArrayBlockingQueue<ApacheLogEntry> statsQueue            = new ArrayBlockingQueue<>(queueCapacity);
        ArrayBlockingQueue<ApacheLogEntry> errorFreqQueue       = new ArrayBlockingQueue<>(queueCapacity);

        // 2. Create workers in threads
        workExecutor.addWorker(
                new Reader(parserQueue, dumperQueue, settings, workExecutor)
        );
        workExecutor.addWorker(
                () -> new Parser(parserQueue, statsQueue, errorHandlerQueue, settings, workExecutor),
                settings.numberParserWorkers()
        );
        try {
            workExecutor.addWorker(
                    new Dumper(dumperQueue, settings, workExecutor)
            );
            workExecutor.addWorker(
                    new ErrorHandler(errorHandlerQueue, errorFreqQueue, settings, workExecutor)
            );
        } catch (IOException e) {
            logger.fatal("Error on create ErrorHandler and Dumper file. Aborting");
            System.exit(1);
        }
        ErrorFrequencyFileManager fileManager = new ErrorFrequencyFileManager(
                settings.errorFrequencyWorkDir(), settings.errorFrequencyMaxFiles()
        );
        workExecutor.addWorker(
                () -> new ErrorFrequency(errorFreqQueue, fileManager, settings, workExecutor),
                settings.numberErrorFrequencyWorkers()
        );
        workExecutor.addWorker(
                new ErrorFrequencyCollector(fileManager, settings, workExecutor)
        );
        Stats stats = new Stats();
        workExecutor.addWorker(
                () -> new StatsRegister(statsQueue, settings, stats, workExecutor),
                settings.numberStatsRegisterWorkers()
        );
        workExecutor.addWorker(
                new StatViewer(settings, stats, workExecutor)
        );

        // 3. Run Threads
        workExecutor.startWork();

        // 4. Join Threads
        workExecutor.join();
        logger.info("Bye :)");
    }
}
