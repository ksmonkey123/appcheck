package ch.awae.serviceCheck.data;

import com.sun.management.GarbageCollectionNotificationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

/**
 * Notification handler processing Garbage Collection notifications.
 *
 * GC notifications are used to collect data about memory usage.
 *
 * Source: http://stackoverflow.com/questions/2057792/garbage-collection-notification
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
@SuppressWarnings("restriction")
class GCNotificationHandler implements NotificationListener {

    private final GCDataContainer minor, major;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    GCNotificationHandler(GCDataContainer minor, GCDataContainer major) {
        this.minor = minor;
        this.major = major;
    }

    /**
     * Handle a notification
     */
    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            // this is a GC notification
            GarbageCollectionNotificationInfo gcInfo = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
            logger.info("GC recorded: " + gcInfo.getGcAction()+" (" + gcInfo.getGcName() +"): " + gcInfo.getGcCause());

            // feed GCInfo to correct container
            if (gcInfo.getGcAction().contains("minor GC"))
                minor.handle(gcInfo.getGcInfo());
            else
                major.handle(gcInfo.getGcInfo());

        } else {
            logger.warn("received unsupported notification of type " + notification.getType());
        }
    }

}
