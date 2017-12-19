package ch.awae.appcheck.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * concrete default implementation of the {@link ICheckResponse} interface
 *
 * This class is used by all provided checks and should be used by any custom checks as well.
 *
 * @author Andreas Wälchli
 * @version 1.1
 *
 * @see CheckResponse
 * @see IChecker
 */
public class CheckResponse implements ICheckResponse {

    private static final long serialVersionUID = 1L;

    private CheckResult result = CheckResult.CHECK_OK;
    private String title, description, message, stackTrace, errorMessage;
    private List<ICheckResponse> subChecks = new ArrayList<>();
    private final transient Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Creates a new CheckResponse with the provided title and description.
     * The check result is {@link CheckResult#CHECK_OK OK} by default.
     */
    public CheckResponse(String title, String description) {
        this.title =title;
        this.description=description;
    }

    @Override
    public CheckResult getResult() {
        return result;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getMessage(){
        return message;
    }

    @Override
    public String getStackTrace() {
        return stackTrace;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public List<ICheckResponse> getSubChecks() {
        return Collections.unmodifiableList(subChecks);
    }

    @Override
    public void setResult(CheckResult result) {
        this.result = result;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public void addSubCheck(ICheckResponse response) {
        subChecks.add(response);
    }

    @Override
    public void setError(Throwable throwable) {
        setError(throwable, CheckResult.CHECK_NOK);
    }

    @Override
    public void setError(Throwable throwable, CheckResult result) {
        this.errorMessage = throwable.getMessage();
        this.result = result;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = null;
        try {
            ps = new PrintStream(bos);
            throwable.printStackTrace(ps);
            ps.flush();
            this.stackTrace = bos.toString();
        } finally {
            if (ps != null) {
                ps.close();
            }
            try {
                bos.close();
            } catch (IOException ioe) {
                logger.error("Exception while closing outputstream", throwable);
            }
        }
    }

    @Override
    public boolean isCheckOK() {
        return result == CheckResult.CHECK_OK;
    }

    @Override
    public boolean isTreeOK() {
        if (!isCheckOK())
            return false;
        for (ICheckResponse child : subChecks)
            if (child != null && !child.isTreeOK())
                return false;
        return true;
    }

}
