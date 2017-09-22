package nuclei3.notifications.model;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(version = 2, entities = {NotificationMessage.class, NotificationData.class}, exportSchema = false)
public abstract class NotificationsDb extends RoomDatabase {

    public abstract NotificationsDao notificationsDao();

}
