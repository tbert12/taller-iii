import java.util.Set;
import org.apache.log4j.*;

public class StatViewer extends ThreadActivity {
    private final Stats stats;
    private final int pollSeconds;
    private final int topResources;
    StatViewer(Settings settings, Stats stats, WorkExecutor workExecutor) {
        super(settings, workExecutor);
        this.stats = stats;
        pollSeconds = settings.statsDumperFrequency();
        topResources = settings.statsTopMostRequestResources();
    }

    @Override
    boolean cycle() throws InterruptedException {
        /*
         * 1. Read Shared memory who has stats
         * 2. Show info in stdout
         * 3. Sleep 60 seconds.
         */
        StatsSummary statsSummary = stats.getSummary(topResources);
        stats.reset();

        StringBuilder msg = new StringBuilder()
                .append("\n\t- Request per seconds ").append((float) statsSummary.requests / (float) pollSeconds)
                .append("\n\t- Request per client ").append(statsSummary.requestPerClient)
                .append("\n\t- Errors ").append(statsSummary.errors)
                .append("\n\t- Most request resource");
        statsSummary.topResource.forEach(s -> msg.append("\n\t\t").append(s));
        logger.info("STATS SUMMARY:" + msg.toString());

        Thread.sleep(1000 * pollSeconds);
        return true;
    }

    @Override
    void onStop() {
    }
}
