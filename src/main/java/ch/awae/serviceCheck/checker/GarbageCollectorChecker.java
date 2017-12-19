package ch.awae.serviceCheck.checker;

import ch.awae.serviceCheck.api.CheckResponse;
import ch.awae.serviceCheck.api.CheckResult;
import ch.awae.serviceCheck.api.IChecker;
import ch.awae.serviceCheck.data.GCDataContainer;
import ch.awae.serviceCheck.statistics.StatSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static ch.awae.serviceCheck.checker.CheckerUtilities.formatDataSize;
import static ch.awae.serviceCheck.checker.CheckerUtilities.propagateResult;
import static ch.awae.serviceCheck.checker.CheckerUtilities.formatDuration;

/**
 * GC checker
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public class GarbageCollectorChecker implements IChecker {

    private final GCDataContainer minorGcDataContainer, majorGcDataContainer;

    // CONFIGURATION START
    private final boolean _isEnabled;
    private final float _root_strictness;
    private final boolean _time_exact;
    private final boolean _memory_exact;
    private final GarbageCollectionCheckerSubConfiguration _minor, _major;
    // CONFIGURATION END

    public GarbageCollectorChecker(final Properties properties, final GCDataContainer minor, final GCDataContainer major) {
        this.minorGcDataContainer = minor;
        this.majorGcDataContainer = major;

        Logger logger = LoggerFactory.getLogger(getClass());
        logger.debug("reading properties");

        _isEnabled = Boolean.parseBoolean(properties.getProperty("check.gc.enabled"));

        logger.debug("gc check enabled? " + _isEnabled);

        if (_isEnabled) {
            // strictness default value
            float default_strictness = Float.parseFloat(properties.getProperty("check.common.strictness"));
            float inherit_strictness = getStrictness(properties.getProperty("check.gc.strictness"), -1, default_strictness);
            // determine strictness level
            {
                _root_strictness = getStrictness(properties.getProperty("check.gc.root_strictness"), -1, default_strictness);
                _time_exact = Boolean.parseBoolean(properties.getProperty("check.gc.exactTime"));
                _memory_exact = Boolean.parseBoolean(properties.getProperty("check.gc.mem.exactSize"));
            }
            // load subconfigs
            _minor = new GarbageCollectionCheckerSubConfiguration(properties, "check.gc.minor.", default_strictness, inherit_strictness);
            _major = new GarbageCollectionCheckerSubConfiguration(properties, "check.gc.major.", default_strictness, inherit_strictness);
        } else {
            _root_strictness = 0;
            _time_exact = false;
            _memory_exact = false;
            _minor = null;
            _major = null;
        }
        logger.debug("init done");
    }

    private float getStrictness(String property, float inherit_strictness, float default_strictness) {
        if (property.equals("inherit"))
            if (inherit_strictness == -1)
                throw new IllegalArgumentException("inheritance not possible");
            else
                return inherit_strictness;
        if (property.equals("default"))
            return default_strictness;
        return Float.parseFloat(property);
    }

    @Override
    public CheckResponse doCheck(String uid) {
        if (!_isEnabled)
            return null;

        // DO CHECK
        CheckResponse result = new CheckResponse("GC Statistics", "Long-Term GC Statistics");
        try {
            // check both GC types
            result.addSubCheck(doGcGheck("minor", minorGcDataContainer, _minor));
            result.addSubCheck(doGcGheck("major", majorGcDataContainer, _major));
            // propagate results
            propagateResult(result, _root_strictness);
        } catch (Exception ex) {
            result.setError(ex);
        }
        return result;
    }

    private CheckResponse doGcGheck(String type, GCDataContainer container, GarbageCollectionCheckerSubConfiguration config) {
        CheckResponse result = new CheckResponse(type + " GC", "Statistics for " + type + " Garbage Collection");
        StatSummary[][] summaries = container.getSummaries();
        String[] periodTitles = container.getPeriodTitle();

        // handle all-balls case
        boolean allBalls = true;
        for (StatSummary sum : summaries[0])
            if (sum.getCount() != 0) {
                allBalls = false;
                break;
            }
        if (allBalls) {
            result.setMessage("GC has not yet run! No data available.");
            result.setResult(CheckResult.CHECK_OK);
            return result;
        }

        // GC FREQUENCY
        {
            CheckResponse duration = new CheckResponse("GC frequency", "Frequency of GC runs");
            for (int i = 0; i < periodTitles.length; i++) {
                CheckResponse sub = new CheckResponse("GC frequency - " + periodTitles[i], "Statistics over the " + periodTitles[i]);
                StatSummary sum = summaries[0][i];

                // frequency in runs per minute
                double frequency = ((double) sum.getCount()) / sum.getDuration() * 60000;
                sub.setMessage(frequency + " runs per minute");
                if (frequency > config.max_frequency)
                    sub.setResult(CheckResult.CHECK_NOK);
                else
                    sub.setResult(CheckResult.CHECK_OK);
                duration.addSubCheck(sub);
            }
            propagateResult(duration, config.innerStrictness);
            result.addSubCheck(duration);
        }

        // CHANNEL_0: GC duration
        {
            CheckResponse duration = new CheckResponse("GC duration", "Time spent per GC run");
            for (int i = 0; i < periodTitles.length; i++) {
                CheckResponse sub = new CheckResponse("GC duration - " + periodTitles[i], "Statistics over the " + periodTitles[i]);
                StatSummary sum = summaries[0][i];

                String min = formatDuration((long) sum.getMin(), _time_exact);
                String avg = formatDuration((long) sum.getAvg(), _time_exact);
                String max = formatDuration((long) sum.getMax(), _time_exact);

                sub.setMessage(String.format("%s / %s / %s", min, avg, max));
                if (sum.getAvg() > config.max_duration)
                    sub.setResult(CheckResult.CHECK_NOK);
                else
                    sub.setResult(CheckResult.CHECK_OK);
                duration.addSubCheck(sub);
            }
            propagateResult(duration, config.innerStrictness);
            result.addSubCheck(duration);
        }

        // CHANNEL_1: Total RAM Usage
        {
            CheckResponse usage = new CheckResponse("RAM usage after GC", "Memory usage after each GC run");
            for (int i = 0; i < periodTitles.length; i++) {
                CheckResponse sub = new CheckResponse("RAM usage - " + periodTitles[i], "Statistics over the " + periodTitles[i]);
                StatSummary sum = summaries[1][i];

                long maxMemory = 0;
                for (long pool : container.getPoolMax())
                    maxMemory += pool;

                double fillRatio = sum.getAvg() / maxMemory;

                String min = formatDataSize((long) sum.getMin(), _memory_exact);
                String avg = formatDataSize((long) sum.getAvg(), _memory_exact);
                String max = formatDataSize((long) sum.getMax(), _memory_exact);
                String rat = String.format("%.2f", fillRatio * 100) + "%";

                sub.setMessage(String.format("%s / %s / %s (avg %s)", min, avg, max, rat));

                if (fillRatio > config.max_MemoryUsage)
                    sub.setResult(CheckResult.CHECK_NOK);
                else
                    sub.setResult(CheckResult.CHECK_OK);
                usage.addSubCheck(sub);
            }
            propagateResult(usage, config.innerStrictness);
            result.addSubCheck(usage);
        }

        // CHANNEL_2: RAM Cleared
        {
            CheckResponse reclamation = new CheckResponse("RAM reclamation per GC", "Memory reclaimed per GC run");
            for (int i = 0; i < periodTitles.length; i++) {
                CheckResponse sub = new CheckResponse("RAM reclamation - " + periodTitles[i], "Statistics over the " + periodTitles[i]);
                StatSummary sum = summaries[2][i];

                String min = formatDataSize((long) sum.getMin(), _memory_exact);
                String avg = formatDataSize((long) sum.getAvg(), _memory_exact);
                String max = formatDataSize((long) sum.getMax(), _memory_exact);

                sub.setMessage(String.format("%s / %s / %s", min, avg, max));
                sub.setResult(CheckResult.CHECK_OK);
                reclamation.addSubCheck(sub);
            }
            propagateResult(reclamation, config.innerStrictness);
            result.addSubCheck(reclamation);
        }

        // CHANNEL_3 -> CHANNEL_7 => POOL USAGES
        long[] poolMax = container.getPoolMax();

        result.addSubCheck(checkPool(summaries[3], periodTitles, "Eden Space", poolMax[0], config.max_eden, config.innerStrictness));
        result.addSubCheck(checkPool(summaries[4], periodTitles, "Survivor Space", poolMax[1], config.max_survivor, config.innerStrictness));
        result.addSubCheck(checkPool(summaries[5], periodTitles, "Old Gen", poolMax[2], config.max_old, config.innerStrictness));
        result.addSubCheck(checkPool(summaries[6], periodTitles, "Perm Gen", poolMax[3], config.max_perm, config.innerStrictness));
        result.addSubCheck(checkPool(summaries[7], periodTitles, "Code Cache", poolMax[4], config.max_code, config.innerStrictness));

        propagateResult(result, config.strictness);

        return result;
    }

    private CheckResponse checkPool(StatSummary[] summaries, String[] periodTitles, String poolName, long poolMax, float limit, float strictness) {
        CheckResponse usage = new CheckResponse(poolName + " usage after GC", "Usage of the memory pool '" + poolName + "' after the GC");
        for (int i = 0; i < periodTitles.length; i++) {
            CheckResponse sub = new CheckResponse(poolName + " usage - " + periodTitles[i], "Statistics over the " + periodTitles[i]);
            StatSummary sum = summaries[i];

            double ratio = sum.getAvg() / poolMax;

            String min = formatDataSize((long) sum.getMin(), _memory_exact);
            String avg = formatDataSize((long) sum.getAvg(), _memory_exact);
            String max = formatDataSize((long) sum.getMax(), _memory_exact);
            String rat = String.format("%.2f", ratio * 100) + "%";

            sub.setMessage(String.format("%s / %s / %s (avg %s)", min, avg, max, rat));

            if (ratio > limit)
                sub.setResult(CheckResult.CHECK_NOK);
            else
                sub.setResult(CheckResult.CHECK_OK);
            usage.addSubCheck(sub);
        }
        propagateResult(usage, strictness);
        return usage;
    }

}