package Wrapper;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ApacheLogEntry {
    private HashMap<String, String> lineParts;

    private static String getAccessLogRegex() {
        String rClientAddr = "^([\\d.\\S]+)"; // Client address
        String rSep1 = " (\\S+)"; // -
        String rSep2 = " (\\S+)"; // -
        String rDate = " \\[([\\w:/]+\\s[+\\-]\\d{4})\\]"; // Date
        String rMethodAndURL = " \"([A-Z]+) ([^ ]+?) (.+?)\""; // request method, url, http version
        String rHTTPCODE = " (\\d{3})"; // HTTP code
        String rBytes = " (\\d+|(.+?))"; // Number of bytes
        String rReferer = "( \"([^\"]+|(.+?))\")?"; // Referer
        String rAgent = "( \"([^\"]+|(.+?))\")?"; // Agent

        return rClientAddr + rSep1 + rSep2 + rDate + rMethodAndURL + rHTTPCODE + rBytes + rReferer + rAgent;
    }

    private static String getErrorLogRegex() {
        String rDate = "^(\\[[^\\]]+\\])"; // Date
        String rType = "( \\[(\\S+)\\])?"; // Type
        String rClient = "( \\[(client (\\S+))\\])?"; //Client
        String rErrorMessage = " (.*)?$"; //Error message
        return rDate + rType + rClient + rErrorMessage;
    }

    private static final Pattern accessLogPattern = Pattern.compile(
            getAccessLogRegex(),Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern errorLogPattern = Pattern.compile(
            getErrorLogRegex(),Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private ApacheLogEntry(String line) {
        lineParts = new HashMap<>();
        lineParts.put("_line",line);
        parseLine();
    }

    public ApacheLogEntry(ApacheLogEntry apacheLogEntry) {
        this.lineParts = new HashMap<>(apacheLogEntry.lineParts);
    }

    public static ApacheLogEntry from(String line) {
        return isApacheLogEntry(line) ? new ApacheLogEntry(line) : null;
    }

    private static boolean isApacheLogEntry(String entry) {
        return entry.matches(getAccessLogRegex()) || entry.matches(getErrorLogRegex());
    }

    private void parseLine() {
        String logLine = lineParts.get("_line");
        Matcher accessLogMatcher = accessLogPattern.matcher(logLine);
        if (accessLogMatcher.matches()) {
            parseAccessLogLine(accessLogMatcher);
        } else  {
            Matcher errorLogMatcher = errorLogPattern.matcher(logLine);
            if (errorLogMatcher.matches()) {
                parseErrorLogLine(errorLogMatcher);
            }
        }
    }

    private void parseErrorLogLine(Matcher errorLogMatcher) {
        if (errorLogMatcher.matches()) {
            lineParts.put("date", errorLogMatcher.group(1));
            lineParts.put("error_type", errorLogMatcher.group(3));
            lineParts.put("error_client", errorLogMatcher.group(6));
            lineParts.put("error_msg", errorLogMatcher.group(7));
        }
    }

    private void parseAccessLogLine(Matcher accessLogMatcher) {
        if (accessLogMatcher.matches()) {
            lineParts.put("client", accessLogMatcher.group(1));
            lineParts.put("date", accessLogMatcher.group(4));
            lineParts.put("http_method", accessLogMatcher.group(5));
            lineParts.put("resource", accessLogMatcher.group(6));
            lineParts.put("http_version", accessLogMatcher.group(7));
            lineParts.put("http_code", accessLogMatcher.group(8));
            lineParts.put("bytes", accessLogMatcher.group(9));
            lineParts.put("referer", accessLogMatcher.group(15));
        }
    }

    public boolean isError() {
        return lineParts.containsKey("error_type")
                && lineParts.get("error_type") != null
                && lineParts.get("error_type").equals("error");
    }

    public String getError() {
        return lineParts.get("error_msg");
    }

    public boolean hasClient() {
        return lineParts.containsKey("client") && lineParts.get("client") != null;
    }

    public String getClient() {
        return lineParts.get("client");
    }

    public boolean hasResource() {
        return lineParts.containsKey("resource") && lineParts.get("resource") != null;
    }

    public String getResource() {
        return lineParts.get("resource");
    }

    public String getRawLine() {
        return lineParts.get("_line");
    }
}
