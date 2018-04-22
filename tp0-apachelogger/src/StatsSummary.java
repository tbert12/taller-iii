import java.util.List;

public class StatsSummary {
    protected final int requests;
    protected final int errors;
    protected final float requestPerClient;
    protected final List<String> topResource;
    StatsSummary(int request, int errors, float requestPerClient, List<String> topResource) {
        this.requests = request;
        this.errors = errors;
        this.requestPerClient = requestPerClient;
        this.topResource = topResource;
    };
}
