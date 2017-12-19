package ch.awae.appcheck.statistics;

/**
 * Base interface for a statistical summary of a data set
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public interface StatSummary {

    String getTitle();

    long getStartTime();

    long getEndTime();

    long getCount();

    double getMin();

    double getAvg();

    double getMax();

    long getDuration();

}
