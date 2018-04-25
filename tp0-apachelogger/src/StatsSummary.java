import java.util.List;

public class StatsSummary {
    private final int requests;
    private final int errors;
    private final float requestPerClient;
    private final List<String> topResource;
    StatsSummary(int request, int errors, float requestPerClient, List<String> topResource) {
        this.requests = request;
        this.errors = errors;
        this.requestPerClient = requestPerClient;
        this.topResource = topResource;
    };

    public int getRequests() {
        return requests;
    }


    public int getErrors() {
        return errors;
    }

    public float getRequestPerClient() {
        return requestPerClient;
    }

    public List<String> getTopResource() {
        return topResource;
    }
}
