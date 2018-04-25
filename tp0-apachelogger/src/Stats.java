import Wrapper.ApacheLogEntry;
import org.apache.log4j.Logger;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Stats {
    private int requests;
    private int errors;
    private HashSet<String> clients;
    private HashMap<String, Integer> resourceCounter;

    public Stats() {
        clients = new HashSet<>();
        resourceCounter = new HashMap<>();
    }

    synchronized void reset() {
        requests = 0;
        errors = 0;
        clients.clear();
        resourceCounter.clear();
    }

    private int getRequestCount() {
        return requests;
    }

    private float getRequestPerClient() {
        return (clients.size() > 0) ? (float)requests / (float)clients.size() : 0;
    }

    private int getErrorsCount() {
        return errors;
    }

    synchronized void add(ApacheLogEntry log) {
        if (log.hasClient()) {
            requests += 1;
            clients.add(log.getClient());
        }
        if (log.isError()) {
            Logger.getLogger(StatsRegister.class).debug("Register error");
            errors+=1;
        }
        if (log.hasResource()) {
            String resourcer = log.getResource();
            Integer previousValue = resourceCounter.get(resourcer);
            resourceCounter.put(resourcer, previousValue == null ? 1 : previousValue + 1);
        }
    }

    synchronized StatsSummary getSummary( int countTopResources ) {
        return new StatsSummary(
          getRequestCount(),
          getErrorsCount(),
          getRequestPerClient(),
          getMostRequestResource(countTopResources)
        );
    }

    private List<String> getMostRequestResource(int n) {
        ArrayList<String> topResource = new ArrayList<>();
        resourceCounter.entrySet().stream()
                .sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue()))
                .forEach(k -> topResource.add( "[" + k.getValue() + "] " + k.getKey()));
        return (topResource.size() <= n) ? topResource : topResource.subList(0, n);
    }


}
