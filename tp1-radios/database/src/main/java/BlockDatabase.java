import Entities.UserActivity;
import Entities.UserListenCount;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class BlockDatabase implements DB {
    private static final Logger LOGGER = Logger.getLogger(BlockDatabase.class);
    private static final Settings SETTINGS = Settings.from("../database.properties");

    private static final int MAX_RADIOS_PER_CLIENT = SETTINGS.get("MAX_RADIOS_PER_CLIENT",3);

    private static final String WORKING_DIR     = SETTINGS.get("WORKING_DIR","../.database/");
    private static final String STATIONS_DIR     = SETTINGS.get("STATION_DIR", "stations/");
    private static final String USERS_DB        = SETTINGS.get("USER_DB","user");
    private static final String CONNECTIONS_DB  = SETTINGS.get("CONNECTION_DB","connection");

    private static final int OFFSET_TIMESTAMP   = SETTINGS.get("OFFSET_TIMESTAMP", 2);

    private static final int BLOCK_SIZE         = SETTINGS.get("BLOCK_SIZE", 100);

    private static final String STATION_PREFIX = "station.";

    private final String stationsDir;
    private HashMap<String,FileCellBlock> DB;
    private HashMap<String,FileCellBlock> DBStation;

    BlockDatabase() throws IOException {
        createDir(WORKING_DIR);
        stationsDir = WORKING_DIR + STATIONS_DIR;
        createDir(WORKING_DIR + STATIONS_DIR);
        String[] files = {CONNECTIONS_DB, USERS_DB};
        DB = new HashMap<>();
        for (String file : files) {
            String path = WORKING_DIR + file;
            DB.put(file, new FileCellBlock(path, BLOCK_SIZE));
        }
        DBStation =  new HashMap<>();
        loadStations();
    }

    private void loadStations() throws IOException {
        File file = new File(stationsDir);
        for (File stationFile : Objects.requireNonNull(file.listFiles())) {
            if (stationFile.isFile() && !DBStation.containsKey(stationFile.getName())) {
                DBStation.put(stationFile.getName(), new FileCellBlock(stationFile.getPath(), BLOCK_SIZE));
                LOGGER.debug("Load database " + stationFile.getName());
            }
        }
    }

    private void reloadStations() {
        try {
            loadStations();
        } catch (IOException e) {
            LOGGER.warn("Cannot reload stations");
            LOGGER.debug(e);
        }
    }

    private void createDir(String path) throws IOException {
        File workingDir = new File(path);
        if (!workingDir.exists()) {
            if (!workingDir.mkdir()) {
                if (!workingDir.exists()) {
                    LOGGER.fatal("Cannot create working dir");
                    throw new IOException("Cannot create working dir");
                }
            }
            LOGGER.debug("Created working dir for DB: " + WORKING_DIR);
        }
    }

    public void cleanDatabases() {
        DB.forEach((key, value) -> value.clean());
        Set<Map.Entry<String, FileCellBlock>> stations = DBStation.entrySet();
        stations.forEach(e -> deleteStation(stationName(e.getKey())));
        DBStation.clear();
    }

    private String stationKey(String radio) {
        return STATION_PREFIX + radio;
    }

    private String stationName(String key) {
        return key.replace(STATION_PREFIX, "");
    }

    private FileCellBlock getStationDB(String name) throws IOException {
        String key = stationKey(name);
        String newFilePath = WORKING_DIR + STATIONS_DIR + key;
        return DBStation.getOrDefault(key, new FileCellBlock(newFilePath, BLOCK_SIZE));
    }

    public void addUserInRadio(String userName, String userQueue, String radio) {
        try {
            getStationDB(radio).insert(userQueue);
            UserListenCount defaultValue = new UserListenCount(userName, 1);
            DB.get(USERS_DB).update(defaultValue.toString(), row -> {
                UserListenCount userRow = UserListenCount.from(row);
                if (userRow != null && userRow.is(userName)) {
                    userRow.addListen();
                    return userRow.toString();
                }
                return row;
            });
        } catch (IOException e) {
            LOGGER.warn(String.format("Cannot add user '%s' in radio '%s'",userName,radio));
            LOGGER.debug(e);
        }
    }

    public void deleteUserFromRadio(String userName, String userQueue, String radio) {
        try {
            getStationDB(radio).delete(row -> row.equalsIgnoreCase(userQueue));
            UserListenCount defaultValue = new UserListenCount(userName, 0);
            DB.get(USERS_DB).update(defaultValue.toString(), row -> {
                UserListenCount userRow = UserListenCount.from(row);
                if (userRow != null && userRow.is(userName)) {
                    userRow.removeListen();
                    return userRow.toString();
                }
                return row;
            });
        } catch (IOException e) {
            LOGGER.warn(String.format("Cannot add user '%s' in radio '%s'",userName,radio));
            LOGGER.debug(e);
        }
    }

    public boolean existStation(String radio) {
        File radioDB = new File(WORKING_DIR + STATIONS_DIR + stationKey(radio));
        return radioDB.exists();
    }

    public boolean userCanHearRadio(String userName, String radio) {

        FileCellBlock users = DB.get(USERS_DB);

        List<String> result = users.find(r -> {
            UserListenCount user = UserListenCount.from(r);
            return  (user != null && user.is(userName));
        });
        if (result.isEmpty()) {
            return true;
        }
        UserListenCount user = UserListenCount.from(result.get(0));
        return user != null && user.getCount() < MAX_RADIOS_PER_CLIENT;

    }

    public void updateUserActivity(String userName) {
        UserActivity initialActivity = UserActivity.init(userName);
        DB.get(CONNECTIONS_DB).update(initialActivity.toString(), row -> {
            UserActivity userRow = UserActivity.from(row);
            if (userRow != null) {
                if (userRow.is(userName)) {
                    userRow.update(OFFSET_TIMESTAMP);
                    return userRow.toString();
                }
            }
            return row;
        });
    }

    public List<String> getStations() {
        reloadStations();
        List<String> stations = new ArrayList<>();
        DBStation.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> stations.add(stationName(e.getKey())));
        return stations;
    }

    public List<String> getUsersInStation(String station) {
        reloadStations();
        String key = stationKey(station);
        return DBStation.containsKey(key) ? DBStation.get(key).find(s -> true) : new ArrayList<>();
    }

    public List<String> getTopUsers(int count) {
        List<String> topUser = new ArrayList<>();
        DB.get(CONNECTIONS_DB).find(s-> true).stream()
                .sorted(UserActivity::compareRow)
                .limit(count)
                .forEach(row -> {
                    UserActivity activity = UserActivity.from(row);
                    if (activity != null) {
                        topUser.add(String.format("%s %d", activity.getUserName(),activity.getTotal()));
                    }
                });
        return topUser;
    }

    public void addStation(String station) {
        try {
            getStationDB(station);
        } catch (IOException e) {
            LOGGER.warn("Cannot add station " + station);
            LOGGER.debug(e);
        }
    }

    public void deleteStation(String station) {
        reloadStations();
        if (DBStation.containsKey( stationKey(station) )) {
            try {
                File file = getStationDB(station).getFile();
                if (file.delete()) {
                    DB.remove( stationKey(station) );
                    LOGGER.debug("Deleted station DB " + station);
                    return;
                }
            } catch (IOException e) {
                LOGGER.debug(e);
            }
            LOGGER.warn("Cannot delete station " + station);
        }
    }

    public List<String> getCountUserPerStation() {
        reloadStations();
        HashMap<String, Integer> stationUserCount = new HashMap<>();

        DBStation.forEach((station, file) -> stationUserCount.put(station, file.find(s -> true).size()));

        List<String> userCountPerRadio = new ArrayList<>();

        stationUserCount.entrySet().stream()
                .sorted((e1, e2) -> -e1.getValue().compareTo(e2.getValue()))
                .forEach(e -> userCountPerRadio.add(String.format("%s (%d)",e.getKey(),e.getValue())));
        return userCountPerRadio;
    }
}
