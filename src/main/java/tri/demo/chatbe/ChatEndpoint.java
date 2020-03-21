package tri.demo.chatbe;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import tri.demo.chatbe.model.Message;
import tri.demo.chatbe.model.MessageEncoder;
import tri.demo.chatbe.model.MessageDecoder;

@ServerEndpoint(
        value = "/chat/{username}",
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class)
public class ChatEndpoint {

    private static Set<ChatEndpoint> chatEndpoints = new CopyOnWriteArraySet<>();
    private static Map<String, String> users = new ConcurrentHashMap<>();

    private Session session;

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        // Get session and WebSocket connection
        chatEndpoints.add(this);
        users.put(session.getId(), username);
        this.session = session;

        Message message = new Message(username, "Connected");
        broadcast(message);
    }

    @OnMessage
    public void onMessage(Session session, Message message) throws IOException {
        // Handle new messages
        message.setFrom(users.getOrDefault(session.getId(), "undefined"));
        broadcast(message);
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        // WebSocket connection closes
        chatEndpoints.remove(this);
        Message message = new Message(
                users.getOrDefault(session.getId(), "undefined"),
                "Disconnected");
        broadcast(message);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Do error handling here
    }

    private static void broadcast(Message message) {
        chatEndpoints.forEach(endpoint -> {
            synchronized (endpoint) {
                try {
                    endpoint.session.getBasicRemote().sendObject(message);
                } catch (IOException | EncodeException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
