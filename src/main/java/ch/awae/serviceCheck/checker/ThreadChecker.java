package ch.awae.serviceCheck.checker;

import ch.awae.serviceCheck.api.CheckResponse;
import ch.awae.serviceCheck.api.CheckResult;
import ch.awae.serviceCheck.api.IChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Properties;

/**
 * provides a list of all currently running threads
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public class ThreadChecker implements IChecker {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ThreadMXBean bean;

    private final boolean _isEnabled;
    private final int _cpuTimeMode;

    public ThreadChecker(final Properties properties) {
        this.bean = ManagementFactory.getThreadMXBean();

        _isEnabled = Boolean.parseBoolean(properties.getProperty("check.thread.enabled"));
        logger.debug("thread check enabled? " + _isEnabled);

        if (_isEnabled) {
            final String mode = properties.getProperty("check.thread.cpuTime.mode");
            if (mode.equals("all"))
                _cpuTimeMode = 2;
            else if (mode.equals("summary"))
                _cpuTimeMode = 1;
            else if (mode.equals("none"))
                _cpuTimeMode = 0;
            else
                throw new IllegalArgumentException("unknown cpuTime mode: " + mode);

            logger.debug("thread cpu time mode = " + _cpuTimeMode);
        } else {
            _cpuTimeMode = 0;
        }
    }

    @Override
    public CheckResponse doCheck(String uid) {
        if (!_isEnabled)
            return null;

        CheckResponse response = new CheckResponse("Thread Check", "Check des JVM Thread Systems");

        try {

            CheckResponse threadCount = new CheckResponse("Thread Count", "Number of currently running threads");
            threadCount.setMessage(bean.getThreadCount() + "");
            threadCount.setResult(CheckResult.CHECK_OK);

            CheckResponse peakCount = new CheckResponse("Peak Thread Count", "Peak number of threads");
            peakCount.setMessage(bean.getPeakThreadCount() + "");
            peakCount.setResult(CheckResult.CHECK_OK);

            response.addSubCheck(threadCount);
            response.addSubCheck(peakCount);
            if (_cpuTimeMode > 0) {

                CheckResponse cpuResp = new CheckResponse("CPU Time", "CPU Time");

                long acc = 0L;
                long[] ids = bean.getAllThreadIds();
                for (long id : ids) {
                    long time = (bean.getThreadCpuTime(id) / 1000);
                    ThreadInfo info = bean.getThreadInfo(id);
                    acc += time;
                    if (_cpuTimeMode == 2) {
                        CheckResponse resp = new CheckResponse("Thread " + info + " (#time)", "Thread CPU Time");
                        resp.setMessage(CheckerUtilities.formatDuration(time, true));
                        resp.setResult(CheckResult.CHECK_OK);
                        cpuResp.addSubCheck(resp);
                    }
                }

                cpuResp.setMessage(CheckerUtilities.formatDuration(acc, true));
                cpuResp.setResult(CheckResult.CHECK_OK);

                response.addSubCheck(cpuResp);

            }

            response.setResult(CheckResult.CHECK_OK);

        } catch (RuntimeException rte) {
            response.setError(rte);
        }

        return response;
    }
}
