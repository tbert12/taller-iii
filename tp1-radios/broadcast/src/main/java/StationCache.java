import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;

public class StationCache {
    private static final Logger LOGGER = Logger.getLogger(StationCache.class);
    private static final Settings SETTINGS = Settings.from("broadcast.properties");
    private static final int RADIO_CACHE_EXPIRATION_SECONDS = SETTINGS.get("CACHE_EXPIRATION_SECONDS", 60);

    private class Cache {
        private final String radio;
        private List<String> users;
        private long timestamp;
        Cache(String radio, List<String> users) {
            this.radio = radio;
            this.timestamp = System.currentTimeMillis() / 1000;
            this.users = users;
        }

        boolean isExpired() {
            long now = System.currentTimeMillis() / 1000;
            return (now - this.timestamp > RADIO_CACHE_EXPIRATION_SECONDS);
        }

        void update(List<String> users) {
            this.timestamp = System.currentTimeMillis() / 1000;
            this.users = users;
        }

        List<String> getUsers() {
            return this.users;
        }
    }

    private final DB database;
    private HashMap<String, Cache> radioCache;

    public StationCache(DB database) {
        this.database = database;
        this.radioCache = new HashMap<>();
    }


    List<String> getUsers(String radio) {
        if (radioCache.containsKey(radio) && !radioCache.get(radio).isExpired()) {
            LOGGER.debug(String.format("Return users in %s from cache", radio));
            return radioCache.get(radio).getUsers();
        }
        List<String> users = database.getUsersInStation(radio);
        if (radioCache.containsKey(radio)) {
            LOGGER.debug(String.format("Updated users cache in radio '%s'", radio));
            radioCache.get(radio).update(users);
        } else {
            LOGGER.debug(String.format("Created users cache in radio '%s'", radio));
            radioCache.put(radio, new Cache(radio, users));
        }
        return users;
    }
}
