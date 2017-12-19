package ch.awae.serviceCheck.statistics;

/**
 * Configuration Object for statistics periods
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public final class PeriodConfig {

    public static final PeriodConfig INFINITE = new PeriodConfig();

    private final String title;
    private final int frameCount;
    private final long frameTime;
    private final long periodTime;
    private final boolean isInfinite;

    /**
     * Creates a new Statistics Period Configuration
     *
     * @param periodTime the total duration of the period (in milliseconds)
     * @param frameCount the number of frames in this period
     */
    public PeriodConfig(final long periodTime, final int frameCount, final String title) {
        this.frameCount = frameCount;
        this.periodTime = periodTime;
        this.frameTime = periodTime / frameCount;
        this.isInfinite = false;
        this.title = title;
    }

    private PeriodConfig() {
        this.frameCount = 0;
        this.frameTime = 0;
        this.periodTime = 0;
        this.title = "lifetime";
        this.isInfinite = true;
    }

    public boolean isInfinite() {
        return isInfinite;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public long getFrameTime() {
        return frameTime;
    }

    public long getPeriodTime() {
        return periodTime;
    }

    public String getTitle() {
        return title;
    }
}
