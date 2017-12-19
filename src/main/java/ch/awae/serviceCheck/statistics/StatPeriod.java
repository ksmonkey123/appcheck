package ch.awae.serviceCheck.statistics;

/**
 * A statistics period holds data for a limited amount of
 * time int multiple frames and discards frames that are too
 * old automatically.
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public class StatPeriod {

    private final long totalTime;
    private final long frameTime;
    private final int frameCount;
    private final String title;

    private final boolean isInfinite;

    private StatFrame[][] frames;
    private int channelCount;

    public StatPeriod(PeriodConfig config, int channelCount, long startTime) {
        this.channelCount = channelCount;
        isInfinite = config.isInfinite();
        if (isInfinite) {
            totalTime = Long.MAX_VALUE;
            frameTime = Long.MAX_VALUE;
            frameCount = 1;
        } else {
            totalTime = config.getPeriodTime();
            frameTime = config.getFrameTime();
            frameCount = config.getFrameCount();
        }
        frames = new StatFrame[this.channelCount][frameCount];
        for (int i = 0; i < this.channelCount; i++) {
            frames[i][0] = new StatFrame(startTime);
        }
        title = config.getTitle();
    }

    public synchronized void addValues(double values[], long timestamp) {
        if (!isInfinite)
            updateFrames(timestamp);
        assert values.length == channelCount;
        for (int i = 0; i < channelCount; i++) {
            frames[i][0].addPoint(values[i]);
        }
    }

    public int getChannelCount() {
        return this.channelCount;
    }

    public long getConfiguredTime() {
        return this.totalTime;
    }

    public int getConfiguredFrameCount() {
        return this.frameCount;
    }

    public int getEffectiveFrameCount() {
        if (!isInfinite)
            updateFrames();
        // find first non-null frame
        for (int i = 0; i < frameCount; i++)
            if (frames[0][i] == null)
                return frameCount - i;
        return 0;
    }

    public long getEffectiveTime() {
        long timestamp = System.currentTimeMillis();
        if (!isInfinite)
            updateFrames(timestamp);
        // find oldest frame
        for (int i = frameCount-1; i>= 0;i--)
            if (frames[0][i] != null)
                return frames[0][i].getStartTime();
        throw new IllegalStateException("unable to find non-null frame!");
    }

    public synchronized StatSummary[] getSummary() {
        long timestamp = System.currentTimeMillis();
        if (isInfinite) {
            StatSummary[] res = new StatSummary[channelCount];
            for (int i = 0; i < channelCount; i++) {
                StatFrame frame = new StatFrame(frames[i][0]);
                frame.setEndTime(timestamp);
                res[i] = new SummaryWrapper(frame, title);
            }
            return res;
        } else {
            updateFrames(timestamp);
            StatSummary[] res = new StatSummary[channelCount];
            for (int i = 0; i < channelCount; i++) {
                StatFrame frame = new StatFrame(frames[i][0]);
                frame.setEndTime(timestamp);
                merge:
                for (int j = 1; j < frameCount; j++) {
                    StatFrame next = frames[i][j];
                    if (next == null)
                        break merge;
                    frame = StatFrame.merge(frame, next);
                }
                res[i] = new SummaryWrapper(frame, title);
            }
            return res;
        }
    }

    private void updateFrames() {
        updateFrames(System.currentTimeMillis());
    }

    private void updateFrames(long timestamp) {
        assert !isInfinite;
        // check if newest frame is too old
        long start = frames[0][0].getStartTime();
        long end = start + frameTime;
        if (end > timestamp)
            return;
        // it's too old
        for (int i = 0; i < channelCount; i++) {
            frames[i][0].setEndTime(end);
            // roll array
            for (int j = frameCount - 1; j > 0; j--)
                frames[i][j] = frames[i][j - 1];
            // calculate start time of new frame
            long delta = (timestamp - end) % frameTime; // time passed since expected frame start
            long nextStart = timestamp - delta;
            // create new frame
            frames[i][0] = new StatFrame(nextStart);
        }
    }

    public String getTitle() {
        return title;
    }

}
