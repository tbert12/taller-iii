package Entities;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;


public class UserActivity {
    private static final String SEPARATOR = "===";
    private static final int INIT_TOTAL_SEC = 1;

    private final String name;
    private long lastTimestampMilliseconds;
    private int total;

    public UserActivity(String name, long lastTimestamp, int total) {
        this.name = name;
        this.lastTimestampMilliseconds = lastTimestamp;
        this.total = total;
    }

    public static UserActivity from(String row) {
        String[] cols = row.split(SEPARATOR);
        if (cols.length != 3) {
            return null;
        }
        return new UserActivity(cols[0], Long.parseLong(cols[1]), Integer.parseInt(cols[2]));
    }

    private static long getTimestamp() {
        return new Date().getTime();
    }

    public static UserActivity init(String userName) {
        return new UserActivity(userName, getTimestamp(), INIT_TOTAL_SEC);
    }

    public boolean is(String userName) {
        return name.equalsIgnoreCase(userName);
    }

    public void update(int offsetTimestampSeconds) {
        Long timestamp = getTimestamp();
        Long lastTimestamp = this.lastTimestampMilliseconds;
        Long diff = timestamp - lastTimestamp;
        this.lastTimestampMilliseconds = timestamp;
        Long add = (diff > offsetTimestampSeconds*1000) ? diff/6000 : INIT_TOTAL_SEC;
        this.total = this.total + add.intValue();
    }

    public String toString() {
        return name + SEPARATOR + lastTimestampMilliseconds + SEPARATOR + total;
    }

    public int getTotal() {
        return this.total;
    }

    public static int compareRow(String r1, String r2) {
        UserActivity userActivity1 = UserActivity.from(r1);
        UserActivity userActivity2 = UserActivity.from(r2);
        if (userActivity1 == null && userActivity2 == null) {
            return 0;
        }
        if (userActivity1 != null && userActivity2 == null) {
            return -1;
        }
        if (userActivity1 == null) {
            return 1;
        }
        return -Integer.compare(userActivity1.total, userActivity2.total);
    }

    public String getUserName() {
        return this.name;
    }
}
