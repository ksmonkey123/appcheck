package ch.awae.serviceCheck.statistics;

/**
 * Wrapper wrapping a statistics frame into a statistics summary
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public class SummaryWrapper implements StatSummary {

    private StatFrame frame;
    private String title;

    public SummaryWrapper(StatFrame frame, String title) {
        this.frame = frame;
        this.title = title;
    }

    @Override
    public double getAvg() {
        return frame.getAvg();
    }

    @Override
    public double getMax() {
        return frame.getMax();
    }

    @Override
    public double getMin() {
        return frame.getMin();
    }

    @Override
    public long getCount() {
        return frame.getCount();
    }

    @Override
    public long getEndTime() {
        return frame.getEndTime();
    }

    @Override
    public long getStartTime() {
        return frame.getStartTime();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public long getDuration() {
        return getEndTime() - getStartTime();
    }
}
