package ch.awae.serviceCheck.checker;


import ch.awae.serviceCheck.api.CheckResult;
import ch.awae.serviceCheck.api.ICheckResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Collection of utilities for the checkers
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
public class CheckerUtilities {

    /**
     * Determines the check result based on the child results.
     * The check is considered OK at least a given ratio of child results are OK.
     *
     * If the {@code response} does not have any children no modification is made.
     *
     * A {@code strictness} value of {@code -1} disabled the propagation and retains the current result.
     *
     * @param response the response to propagate the child results for
     * @param strictness the minimal ratio of children required to be OK in order for the check to be OK
     * @throws NullPointerException if the {@code response} argument is {@code null}
     * @throws IllegalArgumentException if the {@code strictness} value exceeds {@code 1} or is a negative value other than {@code -1}
     */
    public static void propagateResult(final ICheckResponse response, final float strictness) {
        Objects.requireNonNull(response, "response may not be null!");

        if (strictness == -1)
            return;

        if (strictness < 0 || strictness > 1)
            throw new IllegalArgumentException("invalid strictness value: " + strictness + " - value must be '-1' or in the range [0;1]!");

        final List<ICheckResponse> subs = response.getSubChecks();
        final int size = subs.size();

        if (size == 0) {
            return;
        }

        // count OK's
        int acc = 0;
        for (ICheckResponse res : subs) {
            if (res.isCheckOK())
                acc++;
        }
        final double score = ((double) acc) / size;

        // apply result
        response.setResult(score >= strictness ? CheckResult.CHECK_OK : CheckResult.CHECK_NOK);
    }

    /**
     * Loads a given properties file into a {@link Properties} instance.
     *
     * The classloader of this class (CheckerUtilities) is used to access the properties file.
     *
     * @param path the path of the properties file inside the *.ear
     * @return the Properties contained in the given file
     * @throws IOException if an I/O Exception occurs.
     * @throws NullPointerException if the {@code path} is {@code null}
     * @throws IllegalArgumentException if no properties file could be found under {@code path}
     */
    public static Properties loadProperties(final String path) throws IOException {
        Objects.requireNonNull(path, "property file path may not be null");

        final InputStream inputStream = CheckerUtilities.class.getClassLoader().getResourceAsStream(path);

        if (inputStream == null)
            throw new IllegalArgumentException("no resource under path '" + path + "'");

        final Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

    public static List<String> loadFile(final String path) throws IOException {
        Objects.requireNonNull(path, "file path may not be null");

        Scanner s = new Scanner(CheckerUtilities.class.getClassLoader().getResourceAsStream(path));
        ArrayList<String> list = new ArrayList<>();
        while(s.hasNextLine())
            list.add(s.nextLine());
        s.close();

        return list;
    }

    /**
     * rounds a given value to n digits behind the decimal point.
     * @param value the value to round
     * @param digits the number of digits behind the decimal point to round to.
     * @return the rounded value.
     */
    private static double round(final double value, final int digits) {
        final double factor = Math.pow(10, digits);

        return Math.round(value * factor) / factor;
    }

    /**
     * Data unit array used by the {@link #formatDataSize(long, boolean)} method to determine the correct unit.
     */
    private final static String[] DATA_UNITS = {"B", "KiB", "MiB", "GiB", "TiB", "PiB"};

    /**
     * Formats a data size given in bytes.
     *
     * The byte number is converted to the largest full power of 1024 and decorated with the correct unit.
     * All values are rounded to 2 decimal places.
     *
     * e.g. 12345 bytes are converted to 12.06KiB
     *
     * Currently sizes up to ca. 999.99PiB are supported. The exact limit however is subject to rounding and may vary.
     *
     * If not {@code enabled} this will simply return the input number with the appended unit "B"
     *
     * @param bytes the byte number to convert
     * @param enabled determines if the formatting is enabled
     * @return the formatted byte number according to the described rules
     * @throws IllegalArgumentException if the {@code bytes} argument is negative or too large (above 999.99PiB)
     */
    public static String formatDataSize(final long bytes, final boolean enabled) {
        if (bytes < 0)
            throw new IllegalArgumentException("data size may not be negative: " + bytes);

        double raw = bytes;
        int counter = 0;
        while(raw >= 1024) {
            raw /= 1024;
            counter++;
        }
        raw = round(raw, 2);
        if (counter >= DATA_UNITS.length)
            throw new IllegalArgumentException("data size to large to format: " + raw + " * 1024^" + counter);

        return raw + DATA_UNITS[counter];
    }

    /**
     * Formats a duration given in milliseconds.
     *
     * If formatting is enabled:
     *   - Durations shorter than 1000ms are represented as "xxxms" (e.g. 453ms)
     *   - Durations above 999ms are represented as "d hh:mm:ss.uuu" (e.g. 5d 12:45:32.234)
     *   - Negative durations are always represented as "xxxms" (e.g. -123462ms)
     *
     * If formatting is disabled:
     *   - All durations are represented as "xxxms" (e.g. 1238675234ms)
     *
     * @param millis the duration in milliseconds to format
     * @param enabled determines if the formatting is enabled
     * @return the formatted duration according to the rules above
     */
    public static String formatDuration(final long millis, final boolean enabled) {
        if (!enabled || millis < 1000L)
            return millis + "ms";
        else
            return String.format("%dd %02d:%02d:%02d.%03d",
                    (millis / 86400000), // 1 day = 86400000ms
                    (millis / 3600000) % 24, // 1h = 3600000ms
                    (millis / 60000) % 60, // 1min = 60000ms
                    (millis / 1000) % 60, // 1s = 1000ms
                    (millis) % 1000);
    }

    /**
     * Determines the strictness value given the property string and values for default strictness and inherited
     * strictness. The strictness is determined as follows:
     * <ol>
     *     <li>If the property is {@code "inherit"}, the inherited strictness value is used</li>
     *     <li>If the property is {@code "default"}, the default strictness value is used</li>
     *     <li>Otherwise the strictness value is parsed from the property</li>
     * </ol>
     * @param property the property String to parse
     * @param inherit_strictness the strictness value for {@code "inherited"}. A negative value indicates that
     *                           inheritance is not allowed.
     * @param default_strictness the strictness value for {@code "default"}
     * @return the strictness value
     * @throws IllegalArgumentException if the property indicates inheritance, but inheritance is not allowed
     * @throws NullPointerException if the property is {@code null}
     * @throws NumberFormatException if the property cannot be parsed as a float value
     */
    public static float getStrictness(String property, float inherit_strictness, float default_strictness) {
        if (property.equals("inherit"))
            if (inherit_strictness == -1)
                throw new IllegalArgumentException("inheritance not possible");
            else
                return inherit_strictness;
        if (property.equals("default"))
            return default_strictness;
        return Float.parseFloat(property);
    }

}
