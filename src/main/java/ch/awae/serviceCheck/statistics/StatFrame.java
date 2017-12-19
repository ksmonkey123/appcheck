package ch.awae.serviceCheck.statistics;

/**
 * A single time frame in the statistics.
 * Frames can have new data added to and they can be merged with other frames.
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public final class StatFrame {

    private final long startTime;
    private volatile long endTime = 0;
    private volatile long count;
    private volatile double min, avg, max;

    /**
     * Creates a new frame
     *
     * @param startTime the system time the statistics start at
     */
    public StatFrame(final long startTime) {
        this.startTime = startTime;
        this.min = 0;
        this.max = 0;
        this.avg = 0;
        this.count = 0;
    }

    /**
     * Copy Constructor
     */
    public StatFrame(StatFrame frame) {
        startTime = frame.startTime;
        endTime = frame.endTime;
        count = frame.count;
        min = frame.min;
        max = frame.max;
        avg = frame.avg;
    }

    /**
     * Merges 2 frames into one
     */
    public static StatFrame merge(StatFrame f1, StatFrame f2) {
        if (f1.getCount() == 0)
            return f2;
        if (f2.getCount() == 0)
            return f1;
        // both are non-empty, therefore merge is OK
        return new StatFrame(f1, f2);
    }

    /**
     * Merging Constructor
     */
    private StatFrame(StatFrame f1, StatFrame f2) {
        assert f1.getCount() > 0 && f2.getCount() > 0;

        this.startTime = Math.min(f1.startTime, f2.startTime);
        this.endTime = Math.max(f1.endTime, f2.endTime);
        this.count = f1.count + f2.count;
        this.min = Math.min(f1.min, f2.min);
        this.max = Math.max(f1.max, f2.max);
        // calculate new average
        this.avg = ((f1.avg * f1.count) + (f2.avg * f2.count)) / (this.count);
    }

    /**
     * Add a data point
     *
     * @param value
     */
    public synchronized void addPoint(final double value) {
        if (this.count == 0) {
            // no data yet - need to be careful about average
            this.avg = value;
            this.min = value;
            this.max = value;
            this.count = 1;
        } else {
            // already have some data - handle normally
            if (this.min > value)
                this.min = value;
            if (this.max < value)
                this.max = value;
            // calculate new average
            this.avg = ((this.avg * this.count) / (this.count + 1)) + (value / (this.count + 1));
            this.count++;
        }
    }

    public void setEndTime(final long endTime) {
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getCount() {
        return count;
    }

    public double getMin() {
        return min;
    }

    public double getAvg() {
        return avg;
    }

    public double getMax() {
        return max;
    }
}
