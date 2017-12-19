package ch.awae.serviceCheck.checker;

import ch.awae.serviceCheck.api.CheckResponse;
import ch.awae.serviceCheck.api.CheckResult;
import ch.awae.serviceCheck.api.IChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Properties;

/**
 * Runtime Checker
 *
 * Provides the time since VM start
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public class RuntimeChecker implements IChecker {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RuntimeMXBean bean;

    private final boolean _isEnabled;
    private final boolean _exactTime;

    public RuntimeChecker(final Properties properties) {
        this.bean = ManagementFactory.getRuntimeMXBean();

        _isEnabled = Boolean.parseBoolean(properties.getProperty("check.runtime.enabled"));

        logger.debug("rt check enabled? " + _isEnabled);

        if (_isEnabled) {
            _exactTime = Boolean.parseBoolean(properties.getProperty("check.runtime.exactTime"));

            logger.debug("rt exact time values? " + _exactTime);
        } else {
            _exactTime = false;
        }

    }

    @Override
    public CheckResponse doCheck(String uid) {
        if (!_isEnabled)
            return null;


        CheckResponse response = new CheckResponse("Runtime Check", "Check of VM information");

        try {

            CheckResponse uptime = new CheckResponse("Uptime", "Time since VM start");
            uptime.setMessage(CheckerUtilities.formatDuration(bean.getUptime(), !_exactTime));
            uptime.setResult(CheckResult.CHECK_OK);
            response.addSubCheck(uptime);
            response.setResult(CheckResult.CHECK_OK);

        } catch (RuntimeException rte) {
            response.setError(rte);
        }

        return response;
    }
}
