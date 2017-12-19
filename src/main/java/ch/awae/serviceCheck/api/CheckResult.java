package ch.awae.serviceCheck.api;

/**
 * Binary result values for check responses.
 *
 * @author Andreas Wälchli
 * @version 1.1
 *
 * @see ICheckResponse#setResult(CheckResult)
 */
public enum CheckResult {
    CHECK_OK,
    CHECK_NOK
}