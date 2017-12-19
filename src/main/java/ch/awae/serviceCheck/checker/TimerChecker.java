package ch.awae.serviceCheck.checker;

import ch.awae.serviceCheck.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Timer;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class TimerChecker implements IChecker {

    private HashMap<String, List<String>> requiredTimers = new HashMap<>();
    private boolean _enabled;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    public TimerChecker(Properties props){
        _enabled = Boolean.parseBoolean(props.getProperty("check.timer.enabled"));

        if (_enabled) {
            try {
                String file = System.getProperty("servicecheck.timers");

                List<String> lines = CheckerUtilities.loadFile(file);
                for(String line: lines) {
                    // skip empty lines
                    if (line.isEmpty())
                        continue;
                    // skip comment lines
                    if (line.startsWith("#"))
                        continue;
                    String parts[] = line.split(";");
                    if (parts.length == 1 && line.endsWith(";"))
                        parts = new String[]{parts[0], null};
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("invalid timer configuration entry: '" + line+ "'");
                    }
                    // create map entry for bean if required
                    if(!requiredTimers.containsKey(parts[0])){
                        requiredTimers.put(parts[0], new ArrayList<String>());
                    }
                    // add timer to list
                    List<String> list = requiredTimers.get(parts[0]);
                    list.add(parts[1]);
                }

            } catch (IOException e) {
                logger.error("io exception thrown during timer checker init", e);
                throw new RuntimeException(e);
            }
        } else {
            this.requiredTimers = null;
        }
    }


    @Override
    public ICheckResponse doCheck(String uid) {
        if (!_enabled)
            return null;

        CheckResponse result = new CheckResponse("Timer Check", "Checks if all required timers are present");

        for(Map.Entry<String, List<String>> entry : requiredTimers.entrySet()) {
            // check timer
            CheckResponse beanResult = new CheckResponse("Timer Bean: " + entry.getKey(), "Checks all the timers configured on the " + entry.getKey() + " bean");
            try {
                // get timer
                IMonitoredTimer bean = (IMonitoredTimer) InitialContext.doLookup(entry.getKey());
                // check timer
                List<String> set = new ArrayList<>();
                for (Timer timer : bean.getTimers()) {
                    Serializable ser = timer.getInfo();
                    timer.getNextTimeout().toString();
                    if (ser == null)
                        set.add(null);
                    else
                        set.add(ser.toString());
                }

                // check if all desired timers are present
                for (String req : entry.getValue()) {
                    CheckResponse sub = new CheckResponse("Timer: " + req, null);
                    if (set.contains(req)) {
                        sub.setMessage("exists");
                        sub.setResult(CheckResult.CHECK_OK);
                        // remove timer from set
                        set.remove(req);

                    } else {
                        sub.setMessage("does not exist");
                        sub.setResult(CheckResult.CHECK_NOK);
                        beanResult.setResult(CheckResult.CHECK_NOK);
                        result.setResult(CheckResult.CHECK_NOK);
                    }
                    beanResult.addSubCheck(sub);
                }

                // list out all additional timers
                for (String t : set) {
                    CheckResponse sub = new CheckResponse("Additional Timer: " + t, null);
                    sub.setMessage("exists, but is not required");
                    beanResult.addSubCheck(sub);
                }

            } catch(NamingException | ClassCastException e) {
                beanResult.setError(e, CheckResult.CHECK_NOK);
                result.setResult(CheckResult.CHECK_NOK);
            }

            result.addSubCheck(beanResult);
        }

        return result;
    }
}
