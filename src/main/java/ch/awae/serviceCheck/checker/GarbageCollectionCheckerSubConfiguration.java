package ch.awae.serviceCheck.checker;

import java.util.Properties;

/**
 * Configuration Object for the {@link GarbageCollectorChecker}
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
class GarbageCollectionCheckerSubConfiguration {

     final long max_duration;
     final double max_frequency;
     final float max_MemoryUsage;
     final float strictness;
     final float innerStrictness;
     final float max_eden, max_survivor, max_old, max_perm, max_code;

     GarbageCollectionCheckerSubConfiguration(Properties props, String prefix, float defaultStrictness, float inheritStrictness) {
        max_duration = Long.parseLong(props.getProperty(prefix + "maxDuration"));
        max_frequency = Double.parseDouble(props.getProperty(prefix + "maxFrequency"));
        max_MemoryUsage = Float.parseFloat(props.getProperty(prefix + "maxMemoryUsage"));
        strictness = CheckerUtilities.getStrictness(props.getProperty(prefix + "strictness"), inheritStrictness, defaultStrictness);
        innerStrictness = CheckerUtilities.getStrictness(props.getProperty(prefix + "innerStrictness"), inheritStrictness, defaultStrictness);
        max_eden = Float.parseFloat(props.getProperty(prefix + "maxEden"));
        max_survivor = Float.parseFloat(props.getProperty(prefix + "maxSurvivor"));
        max_old = Float.parseFloat(props.getProperty(prefix + "maxOld"));
        max_perm = Float.parseFloat(props.getProperty(prefix + "maxPerm"));
        max_code = Float.parseFloat(props.getProperty(prefix + "maxCode"));
    }

}
