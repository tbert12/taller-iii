package Entities;

public class UserListenCount {
    private static final String SEPARATOR = "===";

    private final String name;
    protected int count;

    public UserListenCount(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public static UserListenCount from(String row) {
        String[] cols = row.split(SEPARATOR);
        if (cols.length != 2) {
            return null;
        }
        return new UserListenCount(cols[0], Integer.parseInt(cols[1]));
    }

    public boolean is(String userName) {
        return userName.equalsIgnoreCase(name);
    }

    public void addListen() {
        this.count += 1;
    }

    public void removeListen() {
        this.count -= 1;
        if (this.count < 0) {
            this.count = 0;
        }
    }

    public String toString() {
        return name + SEPARATOR + count;
    }

    public int getCount() {
        return this.count;
    }
}
