import org.apache.log4j.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import java.util.AbstractMap;

/**
 * Created by evgeniyh on 12/28/17.
 */
@ApplicationPath("/")
@Path("/")
public class Messenger extends Application {
    private final static Logger logger = Logger.getLogger(Messenger.class);

    public Messenger() {
        super();
        String csbUrl = System.getenv("CSB_URL");
        logger.info("CSB url is set to - " + csbUrl);
    }

    @GET
    @Path("/")
    public String root() {
        logCalledEndpoint("/", new Parameter("AD","ADS"), new Parameter("11AD","ADS23"));
        return logAndCreateResponse(true, "Hello from communication service");
    }

    @POST
    @Path("/start-new")
    public String startNewCommunication(@QueryParam("id1") String user1, @QueryParam("id1") String user2) {

        return logAndCreateResponse(true, "new communication started");
    }

    private void logCalledEndpoint(String endpoint, Parameter... params) {
        StringBuilder sb = new StringBuilder(String.format("Called endpoint %s with params: ", endpoint));

        if (params == null || params.length == 0) {
            sb.append("NONE");
        } else {
            sb.append(params[0]);
            for (int i = 1; i < params.length; i++) {
                sb.append(", ").append(params[i].toString());
            }
        }

        logger.info(sb.toString());
    }


    private String logAndCreateResponse(boolean success, String msg) {
        String logMessage = "The returned message is: " + msg;
        if (success) {
            logger.info(logMessage);
        } else {
            logger.error(logMessage);
        }
        return String.format("{\"success\":%b, \"message\":\"%s\"}", success, msg);
    }

    private class Parameter {
        private final String key;
        private final String value;

        Parameter(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}
