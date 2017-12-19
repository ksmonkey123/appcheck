package ch.awae.appcheck.api;

import java.util.Collection;
import javax.ejb.Timer;

/**
 * interface to be used for all timers that should be monitored.
 *
 * AppCheck is unable to collect information about {@link Timer Timers} itself
 * and relies on the timer EJB itself to provide the relevant information.
 * A default implementation is provided in the method documentation.
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public interface IMonitoredTimer {

    /**
     * Provides a collection of all registered timers.
     *
     * The following implementation should be used:
     *
     * {@code return timerService.getTimers();}
     * where {@code timerService} is the {@link javax.ejb.TimerService}
     * of the EJB.
     *
     * @return a collection of all timers.
     */
    Collection<Timer> getTimers();

}
