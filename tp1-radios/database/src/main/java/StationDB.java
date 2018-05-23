import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StationDB {
    private static final Logger LOGGER = Logger.getLogger(BlockDatabase.class);
    private static final Settings SETTINGS = Settings.from("../database.properties");

    private static final String WORKING_DIR     = SETTINGS.get("WORKING_DIR","../.database/");
    private static final String STATIONS_DIR     = SETTINGS.get("STATION_DIR", "stations/");

    private static final int BLOCK_SIZE         = SETTINGS.get("BLOCK_SIZE", 100);

    private static final String STATION_PREFIX = "station.";

    private static final String STATIONS_WORKING_DIR = WORKING_DIR + STATIONS_DIR;

    private HashMap<String,FileCellBlock> Stations;

    StationDB() throws IOException {
        checkDir(WORKING_DIR);
        checkDir(STATIONS_WORKING_DIR);
        Stations = new HashMap<>();
        loadStations();
    }

    private void checkDir(String dirName) throws IOException {
        File workingDir = new File(dirName);
        if (!workingDir.exists()) {
            throw new IOException(String.format("Working directory '%s' not exist", dirName));
        }
    }

    private void loadStations() throws IOException {
        File file = new File(STATIONS_WORKING_DIR);
        for (File stationFile : Objects.requireNonNull(file.listFiles())) {
            if (stationFile.isFile() && !Stations.containsKey(stationFile.getName())) {
                Stations.put(stationFile.getName(), new FileCellBlock(stationFile.getPath(), BLOCK_SIZE));
                LOGGER.debug("Load database for radio" + stationFile.getName());
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


    private String stationKey(String radio) {
        return STATION_PREFIX + radio;
    }

    private String stationName(String key) {
        return key.replace(STATION_PREFIX, "");
    }

    private FileCellBlock getStationDB(String name) throws IOException {
        String key = stationKey(name);
        String newFilePath = WORKING_DIR + STATIONS_DIR + key;
        return Stations.getOrDefault(key, new FileCellBlock(newFilePath, BLOCK_SIZE));
    }

    void addUser(String userName, String userQueue, String radio) {
        try {
            getStationDB(radio).insert(userQueue);
        } catch (IOException e) {
            LOGGER.error(String.format("Cannot add user %s in radio %s",userName, radio));
            LOGGER.debug(e);
        }
    }

    void removeUser(String userName, String userQueue, String radio) {
        try {
            getStationDB(radio).delete(row -> row.equalsIgnoreCase(userQueue));
        } catch (IOException e) {
            LOGGER.error(String.format("Cannot remove user %s in radio %s",userName, radio));
            LOGGER.debug(e);
        }
    }

    void add(String station) {
        try {
            getStationDB(station);
        } catch (IOException e) {
            LOGGER.warn("Cannot add station " + station);
            LOGGER.debug(e);
        }
    }

    boolean exist(String radio) {
        File radioDB = new File(WORKING_DIR + STATIONS_DIR + stationKey(radio));
        return radioDB.exists();
    }

    void remove(String station) {
        reloadStations();
        if (Stations.containsKey( stationKey(station) )) {
            try {
                File file = getStationDB(station).getFile();
                if (file.delete()) {
                    Stations.remove( stationKey(station) );
                    LOGGER.debug("Deleted radio " + station);
                    return;
                }
            } catch (IOException e) {
                LOGGER.debug(e);
            }
            LOGGER.warn("Cannot delete station " + station);
        }
    }

    List<String> getUsers(String radio) {
        reloadStations();
        String key = stationKey(radio);
        return Stations.containsKey(key) ? Stations.get(key).find(s -> true) : new ArrayList<>();
    }

    List<String> getAll() {
        reloadStations();
        List<String> stations = new ArrayList<>();
        Stations.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> stations.add(stationName(e.getKey())));
        return stations;
    }


    List<String> getUserCountPerStation() {
        reloadStations();

        HashMap<String, Integer> stationUserCount = new HashMap<>();
        Stations.forEach((station, db) -> stationUserCount.put(station, db.find(s -> true).size()));

        List<String> userCountPerRadio = new ArrayList<>();
        stationUserCount.entrySet().stream()
                .sorted((e1, e2) -> -e1.getValue().compareTo(e2.getValue()))
                .forEach(e -> userCountPerRadio.add(String.format("%s (%d)",e.getKey(),e.getValue())));

        return userCountPerRadio;
    }

    public synchronized void clean() {
        Set<Map.Entry<String, FileCellBlock>> stations = Stations.entrySet();
        stations.forEach(e -> {
            FileCellBlock stationFile = e.getValue();
            if (stationFile.getFile().delete()) {
                LOGGER.info("Deleted station key: " + e.getKey());
            } else {
                LOGGER.warn("Cannot delete station key: " + e.getKey());
            }
        });
        Stations.clear();
    }
}
