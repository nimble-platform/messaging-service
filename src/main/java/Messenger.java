import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by evgeniyh on 12/28/17.
 */
@ApplicationPath("/")
@Path("/")
@Singleton
public class Messenger extends Application {
    private final static Logger logger = Logger.getLogger(Messenger.class);
    private final static String MESSAGING_TOPIC = "messaging_service_topic";

    private final  String MESSAGING_TABLE;
    private final  String SESSIONS_TABLE;

    private final Map<String, Collaborations> keyToCollaboration;

    private final Gson gson = new Gson();
    private final String sendMessageUrl;
    private final DBManager dbManager;

    public Messenger() throws Exception {
        super();
        logger.info("Subscribing to kafka topic");

        MESSAGING_TABLE = System.getenv("MESSAGING_TABLE");
        SESSIONS_TABLE = System.getenv("SESSIONS_TABLE");
        if (Common.isNullOrEmpty(MESSAGING_TABLE) || Common.isNullOrEmpty(SESSIONS_TABLE)) {
            logger.error("Messaging and sessions tables can't be null or empty");
            throw new IllegalStateException("Missing table names");
        }

        String csbUrl = System.getenv("CSB_URL");
        String serviceUrl = System.getenv("SERVICE_URL");

        if (Common.isNullOrEmpty(csbUrl) || Common.isNullOrEmpty(serviceUrl)) {
            logger.error("Failed to initialize - missing URLs for connecting and receiving messages");
            throw new IllegalArgumentException("Missing URLs values");
        }

        sendMessageUrl = String.format("http://%s/producer/send/%s", csbUrl, MESSAGING_TOPIC);

        String subscribeUrl = String.format("http://%s/consumer/subscribe/%s?handler=http://%s/receive", csbUrl, MESSAGING_TOPIC, serviceUrl);
        logger.info("CSB subscribe URL is set to - " + subscribeUrl);
        HttpPost httpPost = new HttpPost(subscribeUrl);

        dbManager = new DBManager(MESSAGING_TABLE, SESSIONS_TABLE, true);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

            HttpResponse response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IllegalAccessError("Failed to subscribe to CSB");
            } else {
                String res = inputStreamToString(response.getEntity().getContent());
                logger.info("Successfully subscribed to CSB, response : " + res);
            }

            logger.info("Loading the collaborations from the database");
            keyToCollaboration = dbManager.loadCollaborations();
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
    }

    //region Hello And Health

    @GET
    public Response root() {
        logCalledEndpoint("/");
        return logAndCreateResponse(200, "Hello from communication service");
    }

    @GET
    @Path("/health-check")
    public Response runHealthCheck() {
        logCalledEndpoint("/health-check");
        synchronized (dbManager) {
            return dbManager.isConnected() ?
                    logAndCreateResponse(200, "OK") :
                    logAndCreateResponse(400, "Failed");
        }
    }

    //endregion

    @GET
    @Path("/{user_id}/sessions")
    public Response getAllSessionsForUser(@PathParam("user_id") String user) {
        logCalledEndpoint(String.format("/%s/sessions", user));
        try {
            logger.info("Retrieving all the session for user - " + user);
            List<SessionInfo> sessions = dbManager.getAllSession(user);
            return logAndCreateResponse(200, new Gson().toJson(sessions));
        } catch (Throwable t) {
            t.printStackTrace();
            return logAndCreateResponse(500, "Failed to retrieve all sessions - " + t.getMessage());
        }
    }

    //region Read Messages

    @GET
    @Path("/{s_id}/latest")
    public Response getLatestFrom(@QueryParam("source") String source, @QueryParam("target") String target, @PathParam("s_id") int sid) {
        logCalledEndpoint(String.format("/%d/latest", sid), new Parameter("from", source), new Parameter("target", target));

        String key = Common.createCollaborationKey(source, target);
        Collaborations collaborations = keyToCollaboration.get(key);
        if (collaborations == null) {
            return logAndCreateResponse(404, String.format("No keyToCollaboration exists between the users '%s' and '%s'", source, target));
        }
        MessageData msg = collaborations.getLastMessageFrom(sid, source);
        if (msg == null) {
            return logAndCreateResponse(404, String.format("No messages were sent from '%s' to '%s'", source, target));
        }

        return logAndCreateResponse(200, msg.getData());
    }

    @GET
    @Path("/{s_id}/all")
    public Response getAllFrom(@QueryParam("source") String source, @QueryParam("target") String target, @PathParam("s_id") int sid) {
        logCalledEndpoint(String.format("/%d/all", sid), new Parameter("source", source), new Parameter("target", target));

        String key = Common.createCollaborationKey(source, target);
        Collaborations collaborations = keyToCollaboration.get(key);
        if (collaborations == null) {
            return logAndCreateResponse(404, String.format("No keyToCollaboration exists between the users '%s' and '%s'", source, target));
        }
        List<MessageData> messages = collaborations.getAllMessagesFrom(sid, source);
        if (messages.size() == 0) {
            return logAndCreateResponse(404, String.format("No messages were sent from '%s' to '%s'", source, target));
        }
        JsonArray array = new JsonArray();
        messages.forEach((m) -> array.add(m.getData()));

        return logAndCreateResponse(200, gson.toJson(array));
    }
    //endregion

    //region Send - Receive

    @POST
    @Path("/receive")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receiveMessage(String requestBody) {
        logCalledEndpoint("/receive", new Parameter("message", requestBody));
        MessageData messageData = new Gson().fromJson(requestBody, MessageData.class);
        if (messageData == null) {
            logger.error("Failed to parse the received message to json");
            throw new NullPointerException("Message is null");
        }
        String key = messageData.getKey();
        Collaborations collaborations = keyToCollaboration.get(key);
        if (collaborations == null) {
            logger.error("Something really bad has happened - received message for non existing keyToCollaboration key");
            throw new NullPointerException("Collaborations is null");
        }

        try {
            int sid = messageData.getSessionId();

            collaborations.addNewMessage(sid, messageData);
            return logAndCreateResponse(200, "MessageData was received");
        } catch (Exception e) {
            return logAndCreateResponse(400, "Problem with storing the message");

        }
    }

    @POST
    @Path("/{s_id}/send")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response sendMessage(@QueryParam("source") String source,
                                @QueryParam("target") String target,
                                @QueryParam("message") String msg,
                                @PathParam("s_id") int sid,
                                String data) {
        logCalledEndpoint(String.format("/%d/send", sid), new Parameter("source", source), new Parameter("target", target),
                new Parameter("message", msg), new Parameter("data", data));

        if (msg == null) {
            msg = data;
        }
        String key = Common.createCollaborationKey(source, target);
        Collaborations c = keyToCollaboration.get(key);
        if (!c.isActive(sid)) {
            return logAndCreateResponse(400, String.format("Can't send messages collaboration with id %d has been archived", sid));
        }

        long time = System.currentTimeMillis();
        MessageData messageData = new MessageData(time, sid, source, target, key, msg);
        String message = gson.toJson(messageData, MessageData.class);

        logger.info("The created message is : " + message);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(sendMessageUrl);
            HttpEntity entity = new ByteArrayEntity(message.getBytes("UTF-8"));
            httpPost.setEntity(entity);

            HttpResponse response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                return logAndCreateResponse(400, "Error during sending the message to CSB-service");
            }

            String res = inputStreamToString(response.getEntity().getContent());
            logger.info("Successfully sent message to CSB service : " + res);

            logger.info("Sending the message to the database");
            dbManager.addNewMessage(messageData);

            return logAndCreateResponse(200, "MessageData was sent");

        } catch (Throwable e) {
            logger.error(e);
            return logAndCreateResponse(400, "Error handling the send message - " + String.valueOf(e));
        }
    }
    //endregion

    //region Start - Archive

    @POST
    @Path("/{s_id}/archive")
    public Response archiveCollaboration(@PathParam("s_id") int sid, @QueryParam("id1") String user1, @QueryParam("id2") String user2) throws SQLException {
        logCalledEndpoint(String.format("/%d/archive", sid), new Parameter("id1", user1), new Parameter("id2", user2));
        String key = Common.createCollaborationKey(user1, user2);
        Collaborations c = keyToCollaboration.get(key);
        if (c == null) {
            return logAndCreateResponse(400, String.format("No session with id %d exists for user=%s and user=s", sid, user1, user2));
        }
        if (!c.isActive(sid)) {
            return logAndCreateResponse(400, String.format("Can't archive collaboration with id %d has already been archived", sid));
        } else {
            try {
                c.archive(sid);

                logger.info("Archiving the collaboration at the Database");
                dbManager.archiveCollaboration(key, sid);

                return logAndCreateResponse(200, String.format("Collaboration with id %d was archived", sid));
            } catch (Throwable t) {
                t.printStackTrace();
                return logAndCreateResponse(400, String.format("Failed to archive session with id %d - %s", sid, t.getMessage()));
            }
        }
    }


    @POST
    @Path("/start-new")
    public Response startNewCommunication(@QueryParam("id1") String user1, @QueryParam("id2") String user2) {
        logCalledEndpoint("/start-new", new Parameter("id1", user1), new Parameter("id2", user2));
        String cKey = Common.createCollaborationKey(user1, user2);
        Collaborations collaborations = keyToCollaboration.get(cKey);

        if (collaborations == null) {
            collaborations = new Collaborations();
            keyToCollaboration.put(cKey, collaborations);
        }
        try {
            int sessionId = collaborations.createNewSessionId();
            collaborations.startNewSession(sessionId);
            dbManager.addNewActiveSession(cKey, sessionId);
            logger.info(String.format("Started new session, session id=%d between user-id=%s and user-id=%s", sessionId, user1, user2));
            return logAndCreateResponse(201, String.valueOf(sessionId));
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error("Failed to start new session");
            return logAndCreateResponse(500, "Failed to start new session");
        }
    }

    //endregion

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

    private String inputStreamToString(InputStream stream) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, "UTF-8");
        return writer.toString();
    }

    private Collaborations getCollaboration(String user1, String user2) {
        String key = Common.createCollaborationKey(user1, user2);
        return keyToCollaboration.get(key);
    }

    private Response logAndCreateResponse(int responseCode, String msg) {
        String logMessage = "The returned message is: " + msg + " and return code is: " + String.valueOf(responseCode);
        if (responseCode >= 200 && responseCode < 300) {
            logger.info(logMessage);
        } else {
            logger.error(logMessage);
        }

        return Response.status(responseCode).entity(msg).build();
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

