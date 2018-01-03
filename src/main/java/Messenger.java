import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import sun.net.www.http.HttpClient;

import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
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

    private final static Map<String, Collaboration> collaborations = new HashMap<>();

    private final Gson gson = new Gson();
    private final String sendMessageUrl;

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

    static String createCollaborationKey(String userId1, String userId2) {
        if (userId1 == null || userId2 == null || userId1.isEmpty() || userId2.isEmpty()) {
            throw new NullPointerException("User ids can't be null or empty");
        }
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Ids can't be the same");
        }

        return (userId1.compareTo(userId2) > 0) ? String.format("%s%d%s", userId1, userId1.hashCode(), userId2) : String.format("%s%d%s", userId2, userId2.hashCode(), userId1);
    }

    @GET
    public Response root() {
        logCalledEndpoint("/");
        return logAndCreateResponse(200, "Hello from communication service");
    }

    @GET
    @Path("/{c_id}/latest")
    public Response getLatestFrom(@QueryParam("from") String fromUser, @QueryParam("to") String toUser, @PathParam("c_id") int cid) {
        logCalledEndpoint("/latest", new Parameter("from", fromUser), new Parameter("to", toUser));

        String key = createCollaborationKey(fromUser, toUser);
        Collaboration collaboration = collaborations.get(key);
        if (collaboration == null){
            return logAndCreateResponse(404, String.format("No collaborations exists between the users '%s' and '%s'", fromUser, toUser));
        }
        MessageData msg = collaboration.getLastMessageFrom(fromUser);
        if (msg == null){
            return logAndCreateResponse(404, String.format("No messages were sent from '%s' to '%s'", fromUser, toUser));
        }

        return logAndCreateResponse(200, msg.getMessage());
    }

    @GET
    @Path("/all")
    public Response getAllFrom(@QueryParam("from") String fromUser, @QueryParam("to") String toUser) {
        logCalledEndpoint("/latest", new Parameter("from", fromUser), new Parameter("to", toUser));

        String key = createCollaborationKey(fromUser, toUser);
        Collaboration collaboration = collaborations.get(key);
        if (collaboration == null){
            return logAndCreateResponse(404, String.format("No collaborations exists between the users '%s' and '%s'", fromUser, toUser));
        }
        List<MessageData> messages = collaboration.getAllMessagesFrom(fromUser);
        if (messages.size() == 0){
            return logAndCreateResponse(404, String.format("No messages were sent from '%s' to '%s'", fromUser, toUser));
        }
        JsonArray array = new JsonArray();
        messages.forEach((m) -> array.add(m.getMessage()));

        return logAndCreateResponse(200, gson.toJson(array));
    }

    private Collaboration getCollaboration(String user1, String user2) {
        String key = createCollaborationKey(user1, user2);
        return collaborations.get(key);
    }

    @POST
    @Path("/start-new")
    public Response startNewCommunication(@QueryParam("id1") String user1, @QueryParam("id2") String user2) {
        logCalledEndpoint("/start-new", new Parameter("id1", user1), new Parameter("id2", user2));
        String key = createCollaborationKey(user1, user2);
        Collaboration collaboration = collaborations.get(key);
        int collaborationCount = 1;
        if (collaboration == null) {
            collaboration = new Collaboration(user1, user2);
            collaborations.put(key, collaboration);
        } else {
            collaboration.startNew();
            collaborationCount = collaboration.getCount();
        }

        return logAndCreateResponse(201, String.format("Started new collaboration, count=%d between user-id=%s and user-id=%s", collaborationCount, user1, user2));
    }

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
        Collaboration collaboration = collaborations.get(key);
        if (collaboration == null) {
            logger.error("Something really bad has happened - received message for non existing collaboration key");
            throw new NullPointerException("Collaboration is null");
        }
        collaboration.addNewMessage(messageData);
        return logAndCreateResponse(200, "MessageData was received");
    }

    @POST
    @Path("/send")
    public Response sendMessage(@QueryParam("from") String fromUser, @QueryParam("to") String toUser, @QueryParam("message") String msg) {
        logCalledEndpoint("/send", new Parameter("from", fromUser), new Parameter("to", toUser), new Parameter("message", msg));

        String message = createMessage(fromUser, toUser, msg);
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

    private String createMessage(String fromUser, String toUser, String msg) {
        long time = System.currentTimeMillis();
        String key = createCollaborationKey(fromUser, toUser);

        MessageData messageData = new MessageData(time, fromUser, toUser, key, msg);

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
