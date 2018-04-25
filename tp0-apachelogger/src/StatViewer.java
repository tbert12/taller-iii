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
                .append("\n\t- Request per seconds ").append((float) statsSummary.getRequests() / (float) pollSeconds)
                .append("\n\t- Request per client ").append(statsSummary.getRequestPerClient())
                .append("\n\t- Errors ").append(statsSummary.getErrors())
                .append("\n\t- Most request resource");
        statsSummary.getTopResource().forEach(s -> msg.append("\n\t\t").append(s));
        super.getLogger().info("STATS SUMMARY:" + msg.toString());

        Thread.sleep(1000 * pollSeconds);
        return true;
    }

    @Override
    void onStop() {
    }
}
