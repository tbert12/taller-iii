import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class JSONDatabase implements DB {
    private static final Logger LOGGER = Logger.getLogger(JSONDatabase.class);
    private static final Settings SETTINGS = Settings.from("../database.properties");

    private static final int MAX_RADIOS_PER_CLIENT = SETTINGS.get("MAX_RADIOS_PER_CLIENT",3);

    private static final String WORKING_DIR     = SETTINGS.get("WORKING_DIR","../.database/");
    private static final String USERS_DB        = SETTINGS.get("USER_DB","user");
    private static final String STATIONS_DB     = SETTINGS.get("STATION_DB", "station");
    private static final String CONNECTIONS_DB  = SETTINGS.get("CONNECTION_DB","connection");

    private static final int OFFSET_TIMESTAMP   = SETTINGS.get("OFFSET_TIMESTAMP", 2);

    JSONDatabase() throws IOException {
        File workingDir = new File(WORKING_DIR);
        if (!workingDir.exists()) {
            if (!workingDir.mkdir()) {
                if (!workingDir.exists()) {
                    LOGGER.fatal("Cannot create working dir");
                    throw new IOException("Cannot create working dir");
                }
            }
            LOGGER.debug("Created working dir for DB: " + WORKING_DIR);
        }
        String[] files = {STATIONS_DB, CONNECTIONS_DB, USERS_DB};
        for (String file : files) {
            String path = WORKING_DIR + file;
            File f = new File(path);
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    if (!f.exists()) {
                        LOGGER.fatal("Cannot create file " + path);
                        throw new IOException("Cannot create file " + path);
                    }
                }
                LOGGER.info(String.format("Created file DB: \"%s\"",path));
            }
        }
    }

    public void cleanDatabases() {
        String[] DBNames = {STATIONS_DB, USERS_DB, CONNECTIONS_DB};
        for (String DBName: DBNames) {
            if (writeJSON(new JSONObject(), DBName)) {
                LOGGER.info(DBName + " cleaned");
            } else {
                LOGGER.warn("Cannot clean " + DBName);
            }
        }

    }

    private synchronized boolean writeJSON(JSONObject json, String fileName) {
        try (FileWriter file = new FileWriter(WORKING_DIR + fileName)) {
            file.write(json.toJSONString());
            file.close();
            return true;
        } catch (IOException e) {
            LOGGER.warn("Cannot write " + WORKING_DIR + fileName);
            LOGGER.debug(e);
        }
        return false;
    }

    private synchronized JSONObject readJSON(String fileName) {
        JSONParser parser = new JSONParser();

        try {
            Object obj = parser.parse(new FileReader(WORKING_DIR + fileName));
            return (JSONObject) obj;

        } catch (IOException | ParseException e) {
            LOGGER.warn("Cannot read " + WORKING_DIR + fileName);
            LOGGER.debug(e);
        }
        return null;
    }

    public void addUserInRadio(String userName, String userQueue, String radio) {
        JSONObject stations = readJSON(STATIONS_DB);

        if (stations == null) {
            return;
        }
        JSONArray userQueues = stations.containsKey(radio) ? (JSONArray) stations.get(radio) : new JSONArray();
        userQueues.add(userQueue);

        stations.put(radio, userQueues);

        if (writeJSON(stations, STATIONS_DB)) {
            JSONObject users = readJSON(USERS_DB);
            if (users == null) {
                return;
            }
            Long count = users.containsKey(userName) ? (Long) users.get(userName) : 0;
            users.put(userName, count + 1);
            if (writeJSON(users, USERS_DB)) {
                LOGGER.info("Added user '" + userName + "' to radio " + radio);
            }
        }
    }

    public void deleteUserFromRadio(String userName, String userQueue, String radio) {
        JSONObject stations = readJSON(STATIONS_DB);

        if (stations == null || !stations.containsKey(radio)) {
            return;
        }

        JSONArray userQueues = (JSONArray) stations.get(radio);
        JSONArray userQueuesNew = new JSONArray();
        for(Object queue: userQueues){
            if (queue instanceof String) {
                if (!((String) queue).equalsIgnoreCase(userQueue)) {
                    userQueuesNew.add(queue);
                }
            }
        }
        stations.put(radio, userQueuesNew);

        if (writeJSON(stations, STATIONS_DB)) {
            JSONObject users = readJSON(USERS_DB);
            if (users == null) {
                return;
            }
            Long count = users.containsKey(userName) ? (Long) users.get(userName) : 1;
            users.put(userName, count - 1);
            if (writeJSON(users, USERS_DB)) {
                LOGGER.info("Delete user '" + userName + "' in radio " + radio);
            }
        }
    }

    public boolean existStation(String radio) {
        JSONObject stations = readJSON(STATIONS_DB);
        if (stations == null) {
            return false;
        }
        if (!stations.containsKey(radio)) {
            return false;
        }
        return true;
    }

    public boolean userCanHearRadio(String userName, String radio) {


        JSONObject users = readJSON(USERS_DB);
        if (users == null) {
            return false;
        }
        if (users.containsKey(userName)) {
            Long radioCount = (Long) users.get(userName);
            return radioCount < MAX_RADIOS_PER_CLIENT;
        }
        return true;

    }

    public void updateUserActivity(String userName) {
        JSONObject usersActivity = readJSON(CONNECTIONS_DB);
        if (usersActivity == null) {
            return;
        }
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (usersActivity.containsKey(userName)) {
            JSONObject userActivity = (JSONObject) usersActivity.get(userName);
            Long lastTimeStamp = (Long) userActivity.get("last");
            userActivity.put("last", timestamp);
            if (timestamp.getTime() - lastTimeStamp > OFFSET_TIMESTAMP) {
                userActivity.put("total", (Long)userActivity.get("total") + 1);
            } else {
                userActivity.put("total", (Long)userActivity.get("total") + (timestamp.getTime() - lastTimeStamp));
            }
            usersActivity.put(userName, userActivity);
        } else {
            JSONObject userActivity = new JSONObject();
            userActivity.put("last", timestamp.getTime());
            userActivity.put("total", 1);
            usersActivity.put(userName, userActivity);
        }
        if (writeJSON(usersActivity, CONNECTIONS_DB)) {
            LOGGER.info("Update activity for user " + userName);
        }

    }

    public List<String> getStations() {
        ArrayList<String> stationsArray = new ArrayList<>();
        JSONObject stations = readJSON(STATIONS_DB);
        if (stations == null) {
            return stationsArray;
        }
        stationsArray.addAll(stations.keySet());
        return stationsArray;
    }

    public List<String> getTopUsers(int count) {
        JSONObject users = readJSON(CONNECTIONS_DB);
        if (users == null) {
            return new ArrayList<>();
        }
        return (List<String>) users.keySet().stream()
                .sorted( (u1,u2) -> {
                    JSONObject user1 = (JSONObject) users.get(u1);
                    JSONObject user2 = (JSONObject) users.get(u2);
                    Long total1 = (Long) user1.get("total");
                    Long total2 = (Long) user2.get("total");
                    return -total1.compareTo(total2);
                })
                .limit(count)
                .map(userName -> userName + " | total: " + ((JSONObject)users.get(userName)).get("total") + " sec.")
                .collect(Collectors.toList());
    }

    public List<String> getUsersInStation(String station) {
        List<String> userQueue = new ArrayList<>();
        JSONObject stations = readJSON(STATIONS_DB);
        if (stations == null || !stations.containsKey(station)) {
            return userQueue;
        }
        JSONArray usersQueue = (JSONArray) stations.get(station);
        for (Object queue : usersQueue) {
            userQueue.add((String)queue);
        }
        return userQueue;
    }

    public void addStation(String station) {
        JSONObject stations = readJSON(STATIONS_DB);
        if (stations == null || stations.containsKey(station)) {
            return;
        }
        stations.put(station, new JSONArray());
        if (writeJSON(stations, STATIONS_DB)) {
            LOGGER.info("Added station " + station + " in DB");
        }
    }

    public void deleteStation(String station) {
        JSONObject stations = readJSON(STATIONS_DB);
        if (stations == null || !stations.containsKey(station)) {
            return;
        }
        stations.remove(station);
        if (writeJSON(stations, STATIONS_DB)) {
            LOGGER.info("Deleted station " + station + " in DB");
        }
    }

    public List<String> getCountUserPerStation() {
        return new ArrayList<>();
    }
}
