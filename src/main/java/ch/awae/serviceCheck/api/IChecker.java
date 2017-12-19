package ch.awae.serviceCheck.api;

/**
 * base interface for any checker.
 *
 * new checkers can be implemented and added to the {@link ch.awae.serviceCheck.RootCheckerBean}
 * to extend the checking functionality of AppCheck.
 *
 * There exists no pre-built enabling/disabling functionality for checks.
 * Each check should determine if it should run or not. Checks that should
 * not run can simply be terminated by returning null from the {@link #doCheck(String)}
 * method. This empty response will be omitted in the full check.
 *
 * @author Andreas Wälchli
 * @version 1.1
 *
 * @see ch.awae.serviceCheck.RootCheckerBean#addChecker(IChecker)
 */
public interface IChecker {

    /**
     * performs a check.
     *
     * this method may not throw any exceptions. Exceptions should instead
     * be wrapped in a response object. It is recommended to use the
     * {@link CheckResponse} implementation for the response.
     *
     * @param uid the uid of the current check.
     *            This id can be used to identify checks log messages.
     *            It should therefore be mentioned in all log messages.
     *
     * @return the check response holding the results of the check or null if no results are to be returned
     */
    ICheckResponse doCheck(String uid);

}
