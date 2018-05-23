import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

public class BlockDatabase implements DB {
    private static final Logger LOGGER = Logger.getLogger(BlockDatabase.class);
    private static final Settings SETTINGS = Settings.from("../database.properties");

    private static final String WORKING_DIR     = SETTINGS.get("WORKING_DIR","../.database/");
    private static final String STATIONS_DIR     = SETTINGS.get("STATION_DIR", "stations/");

    private final StationDB stationDB;
    private final UserDB userDB;


    BlockDatabase() throws IOException {
        createDir(WORKING_DIR);
        createDir(WORKING_DIR + STATIONS_DIR);
        stationDB = new StationDB();
        userDB = new UserDB();
    }


    private void createDir(String path) throws IOException {
        File workingDir = new File(path);
        if (!workingDir.exists()) {
            if (!workingDir.mkdir()) {
                if (!workingDir.exists()) {
                    LOGGER.fatal("Cannot create working dir: " + path);
                    throw new IOException("Cannot create working dir: " + path);
                }
            }
            LOGGER.debug("Created working dir for DB: " + path);
        }
    }


    public void cleanDatabases() {
        stationDB.clean();
        userDB.clean();
    }

    public void addUserInRadio(String userName, String userQueue, String radio) {
        stationDB.addUser(userName, userQueue, radio);
        userDB.addRadio(userName);
    }

    public void deleteUserFromRadio(String userName, String userQueue, String radio) {
        stationDB.removeUser(userName, userQueue, radio);
        userDB.removeRadio(userName);
    }

    public boolean existStation(String radio) {
        return stationDB.exist(radio);
    }

    public boolean userCanHearRadio(String userName, String radio) {
        return userDB.canConnectToRadio(userName, radio);
    }

    public void updateUserActivity(String userName) {
        userDB.updateActivity(userName);
    }

    public List<String> getStations() {
        return stationDB.getAll();
    }

    public List<String> getUsersInStation(String station) {
        return stationDB.getUsers(station);
    }

    public List<String> getTopUsers(int count) {
        return userDB.getTopUsers(count);
    }

    public void addStation(String station) {
        stationDB.add(station);
    }

    public void deleteStation(String station) {
        stationDB.remove(station);
    }

    public List<String> getCountUserPerStation() {
        return stationDB.getUserCountPerStation();
    }
}
