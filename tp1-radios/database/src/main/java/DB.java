import java.util.*;

public interface DB {

    void cleanDatabases();

    void addUserInRadio(String userName, String userQueue, String radio);

    List<String> getUsersInStation(String radio);

    boolean userCanHearRadio(String userName, String radio);

    void deleteUserFromRadio(String userName, String userQueue, String radio);

    void addStation(String station);

    boolean existStation(String radio);

    void deleteStation(String station);

    void updateUserActivity(String userName);

    List<String> getStations();

    List<String> getTopUsers(int count);

    List<String> getCountUserPerStation();
}
