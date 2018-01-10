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
import java.util.HashMap;
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
    private final static String MESSAGING_TOPIC = "test_messaging_topic";
    private final static String MESSAGING_TABLE = "test_messaging";
    private final static String ACTIVE_TABLE = "test_active";

    private final static Map<String, Collaborations> collaborations = new HashMap<>();

    private final Gson gson = new Gson();
    private final String sendMessageUrl;
    private final DBManager dbManager = new DBManager(MESSAGING_TABLE, ACTIVE_TABLE);

    public Messenger() {
        super();
        logger.info("Subscribing to kafka topic");

        String csbUrl = System.getenv("CSB_URL");
        String serviceUrl = System.getenv("SERVICE_URL");

        if (csbUrl == null || serviceUrl == null || csbUrl.isEmpty() || serviceUrl.isEmpty()) {
            logger.error("Failed to initialize - missing URLs for connecting and receiving messages");
            throw new IllegalArgumentException("Missing URLs values");
        }

        sendMessageUrl = String.format("http://%s/producer/send/%s", csbUrl, MESSAGING_TOPIC);

        String subscribeUrl = String.format("http://%s/consumer/subscribe/%s?handler=http://%s/receive", csbUrl, MESSAGING_TOPIC, serviceUrl);
        logger.info("CSB subscribe URL is set to - " + subscribeUrl);
        HttpPost httpPost = new HttpPost(subscribeUrl);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {

            HttpResponse response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IllegalAccessError("Failed to subscribe to CSB");
            } else {
                String res = inputStreamToString(response.getEntity().getContent());
                logger.info("Successfully subscribed to CSB, response : " + res);
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    //region Hello From Service

    @GET
    public Response root() {
        logCalledEndpoint("/");
        return logAndCreateResponse(200, "Hello from communication service");
    }
    //endregion

    //region Read Messages

    @GET
    @Path("/{c_id}/latest")
    public Response getLatestFrom(@QueryParam("from") String fromUser, @QueryParam("to") String toUser, @PathParam("c_id") int cid) {
        logCalledEndpoint(String.format("/%d/latest", cid), new Parameter("from", fromUser), new Parameter("to", toUser));

        String key = Common.createCollaborationKey(fromUser, toUser);
        Collaborations collaborations = Messenger.collaborations.get(key);
        if (collaborations == null) {
            return logAndCreateResponse(404, String.format("No collaborations exists between the users '%s' and '%s'", fromUser, toUser));
        }
        MessageData msg = collaborations.getLastMessageFrom(cid, fromUser);
        if (msg == null) {
            return logAndCreateResponse(404, String.format("No messages were sent from '%s' to '%s'", fromUser, toUser));
        }

        return logAndCreateResponse(200, msg.getData());
    }

    @GET
    @Path("/{c_id}/all")
    public Response getAllFrom(@QueryParam("from") String fromUser, @QueryParam("to") String toUser, @PathParam("c_id") int cid) {
        logCalledEndpoint(String.format("/%d/all", cid), new Parameter("from", fromUser), new Parameter("to", toUser));

        String key = Common.createCollaborationKey(fromUser, toUser);
        Collaborations collaborations = Messenger.collaborations.get(key);
        if (collaborations == null) {
            return logAndCreateResponse(404, String.format("No collaborations exists between the users '%s' and '%s'", fromUser, toUser));
        }
        List<MessageData> messages = collaborations.getAllMessagesFrom(cid, fromUser);
        if (messages.size() == 0) {
            return logAndCreateResponse(404, String.format("No messages were sent from '%s' to '%s'", fromUser, toUser));
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
        Collaborations collaborations = Messenger.collaborations.get(key);
        if (collaborations == null) {
            logger.error("Something really bad has happened - received message for non existing collaborations key");
            throw new NullPointerException("Collaborations is null");
        }

        try {
            int cid = messageData.getCid();

            collaborations.addNewMessage(cid, messageData);
            return logAndCreateResponse(200, "MessageData was received");
        } catch (Exception e) {
            return logAndCreateResponse(400, "Problem with storing the message");

        }
    }

    @POST
    @Path("/{c_id}/send")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response sendMessage(@QueryParam("from") String fromUser,
                                @QueryParam("to") String toUser,
                                @QueryParam("message") String msg,
                                @PathParam("c_id") int cid,
                                String data) {
        logCalledEndpoint(String.format("/%d/send", cid), new Parameter("from", fromUser), new Parameter("to", toUser),
                new Parameter("message", msg), new Parameter("data", data));

        if (msg == null) {
            msg = data;
        }
        String key = Common.createCollaborationKey(fromUser, toUser);
        Collaborations c = collaborations.get(key);
        if (!c.isActive(cid)) {
            return logAndCreateResponse(400, String.format("Can't send messages collaboration with id %d has been archived", cid));
        }
        String message = createMessage(fromUser, toUser, key, msg, cid);
        logger.info("The created message is : " + message);

        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(sendMessageUrl);
            HttpEntity entity = new ByteArrayEntity(message.getBytes("UTF-8"));
            httpPost.setEntity(entity);

            HttpResponse response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IllegalAccessError("Failed to send the message");
            } else {
                String res = inputStreamToString(response.getEntity().getContent());
                logger.info("Successfully sent message to CSB service : " + res);
            }
        } catch (Exception e) {
            logger.error(e);
        }

        return logAndCreateResponse(200, "MessageData was sent");
    }
    //endregion

    //region Start - Archive

    @POST
    @Path("/{c_id}/archive")
    public Response archiveCollaboration(@PathParam("c_id") int cid, @QueryParam("id1") String user1, @QueryParam("id2") String user2) {
        logCalledEndpoint(String.format("/%d/archive", cid), new Parameter("id1", user1), new Parameter("id2", user2));
        String key = Common.createCollaborationKey(user1, user2);
        Collaborations c = collaborations.get(key);
        if (!c.isActive(cid)) {
            return logAndCreateResponse(400, String.format("Can't archive collaboration with id %d has already been archived", cid));
        } else {
            c.archive(cid);
            return logAndCreateResponse(200, String.format("Collaboration with id %d was archived", cid));
        }
    }


    @POST
    @Path("/start-new")
    public Response startNewCommunication(@QueryParam("id1") String user1, @QueryParam("id2") String user2) {
        logCalledEndpoint("/start-new", new Parameter("id1", user1), new Parameter("id2", user2));
        String key = Common.createCollaborationKey(user1, user2);
        Collaborations collaborations = Messenger.collaborations.get(key);

        if (collaborations == null) {
            collaborations = new Collaborations(user1, user2);
            Messenger.collaborations.put(key, collaborations);
        }
        collaborations.startNew();
        int count = collaborations.getCount();
        logger.info(String.format("Started new collaborations, count=%d between user-id=%s and user-id=%s", count, user1, user2));

        return logAndCreateResponse(201, String.valueOf(count));
    }
    //endregion

    private String createMessage(String fromUser, String toUser,String key, String msg, int cid) {
        long time = System.currentTimeMillis();

        MessageData messageData = new MessageData(time, cid, fromUser, toUser, key, msg);

        return gson.toJson(messageData, MessageData.class);
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

    private String inputStreamToString(InputStream stream) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, "UTF-8");
        return writer.toString();
    }

    private Collaborations getCollaboration(String user1, String user2) {
        String key = Common.createCollaborationKey(user1, user2);
        return collaborations.get(key);
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

