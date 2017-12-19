package ch.awae.serviceCheck.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.management.NotificationEmitter;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/**
 * AppCheck Data Singleton Bean
 *
 * This singleton Bean is initialised on system startup.
 * This is necessary to start recording garbage collection.
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
@Startup
@Singleton(name = "CheckerDataEJB")
public class CheckerDataBean {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GCNotificationHandler gcNotificationHandler;

    // DATA CONTAINERS
    private final GCDataContainer minorGcData;
    private final GCDataContainer majorGcData;

    public CheckerDataBean() throws IOException {
        this.minorGcData = new GCDataContainer();
        this.majorGcData = new GCDataContainer();
        this.gcNotificationHandler = new GCNotificationHandler(this.minorGcData, this.majorGcData);
    }

    @PostConstruct
    private void init() {
        registerGCNotification();
    }

    /**
     * Registers the gcNotificationHandler for all GC notifications.
     *
     * Source: http://stackoverflow.com/questions/2057792/garbage-collection-notification
     */
    private void registerGCNotification() {
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            logger.info("registering GC bean " + bean.getName());
            NotificationEmitter emitter = (NotificationEmitter) bean;
            emitter.addNotificationListener(gcNotificationHandler, null, null);
        }
    }

    public GCDataContainer getMinorGCDataContainer() {
        return this.minorGcData;
    }

    public GCDataContainer getMajorGcDataContainer() {
        return this.majorGcData;
    }

}
