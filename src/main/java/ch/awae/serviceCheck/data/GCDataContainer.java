package ch.awae.serviceCheck.data;

import ch.awae.serviceCheck.checker.CheckerUtilities;
import ch.awae.serviceCheck.statistics.PeriodConfig;
import ch.awae.serviceCheck.statistics.StatManager;
import ch.awae.serviceCheck.statistics.StatSummary;
import com.sun.management.GcInfo;

import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Data container holding recorded garbage collection data
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
@SuppressWarnings("restriction")
public class GCDataContainer {

    private StatManager manager;

    private long[] poolMax = new long[5];

    public GCDataContainer() throws IOException {
        manager = buildStatManager();
    }

    public StatSummary[][] getSummaries() {
        return manager.getAllSummaries();
    }

    /**
     * record a new data set
     *
     * @param gcinfo the data set to record
     */
    public void handle(GcInfo gcinfo) {
        double[] values = new double[9];

        // CHANNEL_0: GC Duration
        values[0] = gcinfo.getDuration();
        // CHANNEL_1: Total RAM after clear
        values[1] = 0;

        Map<String, MemoryUsage> afterMap = gcinfo.getMemoryUsageAfterGc();

        // get memory objects
        MemoryUsage eden = afterMap.get("PS Eden Space");
        MemoryUsage survivor = afterMap.get("PS Survivor Space");
        MemoryUsage old = afterMap.get("PS Old Gen");
        MemoryUsage perm = afterMap.get("PS Perm Gen");
        MemoryUsage code = afterMap.get("Code Cache");

        // collect them into an array
        MemoryUsage pools[] = {eden, survivor, old, perm, code};

        // CHANNEL_3 -> CHANNEL_7 are memory pools
        for (int i = 0; i < 5; i++) {
            poolMax[i] = pools[i].getMax();
            values[i + 3] = pools[i].getUsed();
            values[1] += values[i + 3];
        }

        // CHANNEL_2: RAM cleared
        {
            Map<String, MemoryUsage> beforeMap = gcinfo.getMemoryUsageBeforeGc();
            // get memory objects
            MemoryUsage _eden = beforeMap.get("PS Eden Space");
            MemoryUsage _survivor = beforeMap.get("PS Survivor Space");
            MemoryUsage _old = beforeMap.get("PS Old Gen");
            MemoryUsage _perm = beforeMap.get("PS Perm Gen");
            MemoryUsage _code = beforeMap.get("Code Cache");

            long totalBefore =
                      _eden.getUsed()
                    + _survivor.getUsed()
                    + _old.getUsed()
                    + _perm.getUsed()
                    + _code.getUsed();

            values[2] = totalBefore - values[1];
        }

        // commit values
        manager.addDataPoint(values);
    }

    public long[] getPoolMax() {
        return poolMax;
    }

    public String[] getPeriodTitle() {
        return manager.getPeriodTitles();
    }

    private static StatManager buildStatManager() throws IOException {
        final Properties properties = CheckerUtilities.loadProperties(System.getProperty("servicecheck.props"));
        String config = properties.getProperty("check.gc.stats");

        List<PeriodConfig> accumulator = new ArrayList<>();

        for (String part : config.split(";")) {
            try {
                if (part.equals("infinite")) {
                    accumulator.add(PeriodConfig.INFINITE);
                    continue;
                }
                String word[] = part.split(",");
                if (word.length != 3)
                    throw new IllegalArgumentException("illegal format in string: " + part);
                // word length is ok
                String desc = word[0];
                long duration = Long.parseLong(word[1]);
                int frames = Integer.parseInt(word[2]);
                // create config
                accumulator.add(new PeriodConfig(duration, frames, desc));
            }catch
                    (RuntimeException e) {
                throw new IllegalArgumentException("invalid period configuration string: " + part, e);
            }
        }

        // construct manager
        PeriodConfig configs[] = accumulator.toArray(new PeriodConfig[0]);
        /*
        There are 8 statistics channels:
        -   0: GC duration in milliseconds
        -   1: total memory usage after GC
        -   2: total amount of memory cleared by GC
        - 3-7: for all 5 memory pools: memory usage after GC
         */
        return new StatManager(8, configs);
    }

}
