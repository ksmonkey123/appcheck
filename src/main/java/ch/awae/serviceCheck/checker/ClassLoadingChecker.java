package ch.awae.serviceCheck.checker;

import ch.awae.serviceCheck.api.CheckResponse;
import ch.awae.serviceCheck.api.CheckResult;
import ch.awae.serviceCheck.api.IChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.util.Properties;

/**
 * Checks the ClassLoading
 *
 * This Check is always OK and acts as a readout of the number of classes.
 *
 * It lists the number of currently loaded classes, the total number of classes loaded since
 * VM start and the total number of classes unloaded since VM start.
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public class ClassLoadingChecker implements IChecker {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ClassLoadingMXBean bean;

    private final boolean _isEnabled;

    public ClassLoadingChecker(final Properties properties) {
        this.bean = ManagementFactory.getClassLoadingMXBean();

        this._isEnabled = Boolean.parseBoolean(properties.getProperty("check.classloading.enabled"));
        logger.debug("classloading check enabled? " + _isEnabled);
    }

    @Override
    public CheckResponse doCheck(String uid) {
        if (!_isEnabled)
            return null;
        CheckResponse response = new CheckResponse("ClassLoader Check", "Check des Class Loaders");

        try {

            CheckResponse currentCL = new CheckResponse("Loaded Classes", "Number of currently loaded classes");
            currentCL.setMessage("" + bean.getLoadedClassCount());
            currentCL.setResult(CheckResult.CHECK_OK);

            CheckResponse totalCL = new CheckResponse("Total Classes", "Total number of classes loaded since VM start");
            totalCL.setMessage("" + bean.getTotalLoadedClassCount());
            totalCL.setResult(CheckResult.CHECK_OK);

            CheckResponse unloadedCL = new CheckResponse("Unloaded Classes", "Total number of classes unloaded since VM start");
            unloadedCL.setMessage("" + bean.getUnloadedClassCount());
            unloadedCL.setResult(CheckResult.CHECK_OK);

            response.addSubCheck(currentCL);
            response.addSubCheck(totalCL);
            response.addSubCheck(unloadedCL);
            response.setResult(CheckResult.CHECK_OK);

        } catch (RuntimeException rte) {
            response.setError(rte);
        }

        return response;
    }
}
