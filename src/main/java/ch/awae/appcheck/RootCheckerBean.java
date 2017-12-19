package ch.awae.appcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.awae.appcheck.api.CheckResponse;
import ch.awae.appcheck.api.ICheckResponse;
import ch.awae.appcheck.api.IChecker;
import ch.awae.appcheck.checker.*;
import ch.awae.appcheck.data.CheckerDataBean;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.IOException;
import java.util.*;

/**
 * Base Checker Bean managing sub-checks.
 *
 * Default checkers are created automatically, additional checkers can be added at any time
 * using {@link #addChecker(IChecker)}.
 *
 * @author Andreas Wälchli
 * @version 1.1
 */
@Singleton(name = "RootCheckerEJB")
public class RootCheckerBean implements IChecker {

    @EJB(beanName = "CheckerDataEJB")
    private CheckerDataBean checkerData;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Properties checkerProps;

    private final Object LOCK = new Object();
    // checker array is filled in init()
    private List<IChecker> checkers;
    private final float _strictness;

    public RootCheckerBean() throws IOException {
        this.checkerProps = CheckerUtilities.loadProperties(System.getProperty("servicecheck.props"));

        _strictness = Float.parseFloat(checkerProps.getProperty("check.root.strictness"));
        logger.debug("root strictness = " + _strictness);

        /* note: subcheckers are initialised in the init() method. This is automatically called
             after object creation after the EJB references are filled in. Required for getting the
             GCDataContainer from the CheckerDataBean for the GCChecker */
    }

    @PostConstruct
    private void init() {
        try {
            ArrayList<IChecker> checkers = new ArrayList<>();
            checkers.addAll(Arrays.asList(
                    new GarbageCollectorChecker(checkerProps,
                            checkerData.getMinorGCDataContainer(),
                            checkerData.getMajorGcDataContainer()),
                    new ClassLoadingChecker(checkerProps),
                    new RuntimeChecker(checkerProps),
                    new ThreadChecker(checkerProps),
                    new TimerChecker(checkerProps)));
            synchronized (LOCK) {
                this.checkers = checkers;
            }
        } catch(RuntimeException e) {
            logger.error("an error occurred while initializing the RootChecker: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Performs a full check. If any sub-check throws an exception
     * that exception will be wrapped in the response.
     *
     * @param uid the uid of the current check.
     *            This id can be used to identify checks log messages.
     *            It should therefore be mentioned in all log messages.
     *
     * @return the check response
     */
    @Override
    public ICheckResponse doCheck(String uid) {
        synchronized (LOCK) {
            CheckResponse response = new CheckResponse("Technical Checks", "Checks der technischen Attribute");
            response.setMessage("Prüft diverse performance-relevante Parameter");

            try {
                // add subchecks in subchecker array
                for (IChecker checker : this.checkers) {
                    ICheckResponse res = checker.doCheck(uid);
                    if (res != null)
                        response.addSubCheck(res);
                }

                CheckerUtilities.propagateResult(response, _strictness);

            } catch (RuntimeException rte) {
                response.setError(rte);
            }

            return response;
        }
    }

    /**
     * Add a new sub-checker
     *
     * @param checker the checker to be added
     * @throws NullPointerException the checker parameter is null
     * @throws IllegalStateException the bean has not been fully initialised
     * @throws IllegalArgumentException the checker has already been added
     */
    public void addChecker(IChecker checker) {
        synchronized (LOCK) {
            if (checkers == null)
                throw new IllegalStateException("not yet initialised");
            Objects.requireNonNull(checker, "custom sub-check may not be null");
            if (checkers.contains(checker))
                throw new IllegalArgumentException("checker already exists");
            checkers.add(checker);
        }
    }

    /**
     * Removes an existing sub-checker if it exists
     *
     * @param checker the checker to be removed
     * @return true if the checker was removed, false otherwise
     * @throws NullPointerException the checker parameter is null
     * @throws IllegalStateException the bean has not been fully initialised
     */
    public boolean removeChecker(IChecker checker) {
        synchronized (LOCK) {
            if (checkers == null)
                throw new IllegalStateException("not yet initialised");
            Objects.requireNonNull(checker, "custom sub-check may not be null");
            if (checkers.contains(checker)) {
                checkers.remove(checker);
                return true;
            }
            return false;
        }
    }

}
