package ch.awae.serviceCheck.statistics;

/**
 * root manager managing multiple parallel periods.
 * This allows for creation of multiple statistics
 * over different time periods.
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public class StatManager {

    private int channelCount;
    private StatPeriod periods[];
    private String periodTitles[];

    public StatManager(int channelCount, PeriodConfig... configs) {
        this.channelCount = channelCount;
        long timestamp = System.currentTimeMillis();
        periods = new StatPeriod[configs.length];
        periodTitles = new String[configs.length];
        for (int i = 0; i < configs.length; i++) {
            periods[i] = new StatPeriod(configs[i], channelCount, timestamp);
            periodTitles[i] = periods[i].getTitle();
        }
    }

    public void addDataPoint(double... values) {
        long timestamp = System.currentTimeMillis();
        for (StatPeriod period : periods)
            period.addValues(values, timestamp);
    }

    /**
     * returns 2D-Array of all summaries.
     *
     * Sorting:
     *
     *  first index is channel
     *  second index is period
     *
     * @return the summaries
     */
    public StatSummary[][] getAllSummaries() {
        StatSummary[][] summaries = new StatSummary[channelCount][periods.length];

        // iterate over all periods
        for (int p = 0; p < periods.length; p++) {
            StatSummary[] sum = periods[p].getSummary();
            // fill values into array
            for (int i = 0; i < channelCount; i++) {
                summaries[i][p] = sum[i];
            }
        }

        return summaries;
    }

    public String[] getPeriodTitles() {
        return periodTitles;
    }

}
