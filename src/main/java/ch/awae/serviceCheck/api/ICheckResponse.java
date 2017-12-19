package ch.awae.serviceCheck.api;

import java.io.Serializable;
import java.util.List;

/**
 * base interface for check responses
 *
 * it is recommended to always use the provided {@link CheckResponse} implementation
 *
 * @author Andreas Wälchli
 * @version 1.1
 *
 * @see CheckResponse
 */
public interface ICheckResponse extends Serializable {

    /**
     * provides the binary result of the response
     */
    CheckResult getResult();

    /**
     * provides the title
     *
     * The title should provide a short description of the response contents.
     * The title of is fixed and must be handled by the implementation.
     */
    String getTitle();

    /**
     * provides the description
     *
     * The description allows adding additional information about a
     * response that is independent from the result.
     * The description is fixed and must be handled by the implementation.
     */
    String getDescription();

    String getMessage();

    String getStackTrace();

    String getErrorMessage();

    /**
     * provides a list of all sub-responses
     *
     * An implementation may choose to only provide read-only lists.
     */
    List<ICheckResponse> getSubChecks();

    /**
     * sets the binary result of the response
     *
     * @param result the result
     */
    void setResult(CheckResult result);

    /**
     * sets the response message
     *
     * This message can hold additional information about the
     * check that is not covered by the title, description or result.
     */
    void setMessage(String message);

    /**
     * sets the stack trace to the response
     */
    void setStackTrace(String stackTrace);

    /**
     * sets the error message of the response
     */
    void setErrorMessage(String errorMessage);

    /**
     * adds a sub-check response
     *
     * @param response the sub-response to add
     */
    void addSubCheck(ICheckResponse response);

    /**
     * adds an error to this response
     *
     * the result is set to {@link CheckResult#CHECK_NOK NOK}
     *
     * @param throwable the Throwable to add.
     */
    void setError(Throwable throwable);

    /**
     * adds an error to this response and updates the result value
     *
     * @param throwable the Throwable to add
     * @param result the new result value
     */
    void setError(Throwable throwable, CheckResult result);

    /**
     * checks if this response is OK.
     *
     * @return true if OK, false if NOK
     */
    boolean isCheckOK();

    /**
     * check recursively if response is OK.
     *
     * This response and all sub-responses are tested with {@link #isCheckOK()}.
     *
     * @return true if this response and all sub-responses are OK, false otherwise
     */
    boolean isTreeOK();
}
