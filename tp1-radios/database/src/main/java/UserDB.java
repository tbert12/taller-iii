import Entities.UserActivity;
import Entities.UserListenCount;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UserDB {

    private static final Logger LOGGER = Logger.getLogger(UserDB.class);
    private static final Settings SETTINGS = Settings.from("../database.properties");

    private static final String WORKING_DIR       = SETTINGS.get("WORKING_DIR","../.database/");

    private static final String USER_STATION_DB   = SETTINGS.get("USER_STATION_DB","user_station");
    private static final String USER_ACTIVITY_DB  = SETTINGS.get("USER_ACTIVITY_DB","user_activity");

    private static final int MAX_RADIOS_PER_CLIENT = SETTINGS.get("MAX_RADIOS_PER_CLIENT",3);
    private static final int OFFSET_TIMESTAMP     = SETTINGS.get("OFFSET_TIMESTAMP", 2);


    private static final int BLOCK_SIZE           = SETTINGS.get("BLOCK_SIZE", 100);

    private final FileCellBlock stationCountDB;
    private final FileCellBlock activityDB;

    UserDB() throws IOException {
        if (!new File(WORKING_DIR).exists()) {
            throw new IOException(String.format("Working directory '%s' not exist", WORKING_DIR));
        }
        stationCountDB = new FileCellBlock(USER_STATION_DB, BLOCK_SIZE);
        activityDB = new FileCellBlock(USER_ACTIVITY_DB, BLOCK_SIZE);
    }

    private void createDir(String path) throws IOException {
        File workingDir = new File(path);
        if (!workingDir.exists()) {
            if (!workingDir.mkdir()) {
                if (!workingDir.exists()) {
                    LOGGER.fatal("Cannot create dir: " + path);
                    throw new IOException("Cannot create working dir: " + path);
                }
            }
            LOGGER.debug("Created working dir for DB: " + path);
        }
    }

    boolean canConnectToRadio(String userName, String radio) {
        List<String> result = stationCountDB.find(r -> {
            UserListenCount user = UserListenCount.from(r);
            return  (user != null && user.is(userName));
        });
        if (result.isEmpty()) {
            return true;
        }
        UserListenCount user = UserListenCount.from(result.get(0));
        return user != null && user.getCount() < MAX_RADIOS_PER_CLIENT;
    }

    private void updateRadio(String userName, UserListenCount defaultValue, Consumer<UserListenCount> action) {
        stationCountDB.update(defaultValue.toString(), row -> {
            UserListenCount userRow = UserListenCount.from(row);
            if (userRow != null && userRow.is(userName)) {
                action.accept(userRow);
                return userRow.toString();
            }
            return row;
        });
    }

    void addRadio(String userName) {
        UserListenCount defaultValue = new UserListenCount(userName, 1);
        updateRadio(userName, defaultValue, UserListenCount::addListen);
    }

    void removeRadio(String userName) {
        UserListenCount defaultValue = new UserListenCount(userName, 0);
        updateRadio(userName, defaultValue, UserListenCount::addListen);
    }

    void updateActivity(String userName) {
        UserActivity initialActivity = UserActivity.init(userName);
        activityDB.update(initialActivity.toString(), row -> {
            UserActivity userRow = UserActivity.from(row);
            if (userRow != null && userRow.is(userName)) {
                userRow.update(OFFSET_TIMESTAMP);
                return userRow.toString();
            }
            return row;
        });
    }


    List<String> getTopUsers(int count) {
        List<String> topUser = new ArrayList<>();
        stationCountDB.find(s-> true).stream()
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

    private void deleteFile(FileCellBlock fileCellBlock, String toLog) {
        if (fileCellBlock.getFile().delete()) {
            LOGGER.info("Deleted DB: " + toLog);
        } else {
            LOGGER.warn("Cannot delete DB: " + toLog);
        }
    }

    public void clean() {
        deleteFile(activityDB, "UserActivityDB");
        deleteFile(stationCountDB, "UserStationCountDB");
        LOGGER.info("Cleaned USER's DB");
    }
}
