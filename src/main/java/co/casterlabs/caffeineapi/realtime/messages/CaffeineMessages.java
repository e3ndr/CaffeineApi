package co.casterlabs.caffeineapi.realtime.messages;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.caffeineapi.CaffeineApi;
import co.casterlabs.caffeineapi.CaffeineAuth;
import co.casterlabs.caffeineapi.CaffeineEndpoints;
import co.casterlabs.caffeineapi.types.CaffeineProp;
import co.casterlabs.caffeineapi.types.CaffeineUser;
import lombok.Setter;

public class CaffeineMessages implements Closeable {
    private static final String ANONYMOUS_LOGIN_HEADER = "{\"Headers\":{\"Authorization\":\"Anonymous Fish\",\"X-Client-Type\":\"api\"}}";
    private static final String AUTH_LOGIN_HEADER = "{\"Headers\":{\"Authorization\":\"Bearer %s\",\"X-Client-Type\":\"api\"},\"Body\":\"{\\\"user\\\":\\\"%s\\\"}\"}";
    private static final long CAFFEINE_KEEPALIVE = TimeUnit.SECONDS.toMillis(15);

    private @Setter @Nullable CaffeineMessagesListener listener;
    private @Setter @Nullable CaffeineAuth auth;

    private Connection conn;

    public CaffeineMessages(CaffeineUser user) {
        this(user.getStageID());
    }

    public CaffeineMessages(String caidOrStage) {
        try {
            this.conn = new Connection(caidOrStage.replace("CAID", ""));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void connect() {
        if (this.conn.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
            this.conn.connect();
        } else {
            this.conn.reconnect();
        }
    }

    public void connectBlocking() throws InterruptedException {
        if (this.conn.getReadyState() == ReadyState.NOT_YET_CONNECTED) {
            this.conn.connectBlocking();
        } else {
            this.conn.reconnectBlocking();
        }
    }

    public void disconnect() {
        this.conn.close();
    }

    public void disconnectBlocking() throws InterruptedException {
        this.conn.closeBlocking();
    }

    private class Connection extends WebSocketClient {

        public Connection(String stageId) throws URISyntaxException {
            super(new URI(String.format(CaffeineEndpoints.CHAT, stageId)));
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            if (auth == null) {
                this.send(ANONYMOUS_LOGIN_HEADER);
            } else {
                this.send(String.format(AUTH_LOGIN_HEADER, auth.getAccessToken(), auth.getSignedToken()));
            }

            Thread t = new Thread(() -> {
                while (this.isOpen()) {
                    try {
                        this.send("\"HEALZ\"");
                        Thread.sleep(CAFFEINE_KEEPALIVE);
                    } catch (Exception ignored) {}
                }
            });

            t.setName("CaffeineMessages KeepAlive");
            t.start();

            if (listener != null) {
                listener.onOpen();
            }
        }

        @Override
        public void onMessage(String raw) {
            try {
                if (!raw.equals("\"THANKS\"") && (listener != null)) {
                    JsonObject json = CaffeineApi.GSON.fromJson(raw, JsonObject.class);

                    if (!json.has("Compatibility-Mode") && json.has("type")) {
                        CaffeineAlertType type = CaffeineAlertType.fromJson(json.get("type"));

                        if (type != CaffeineAlertType.UNKNOWN) {
                            CaffeineUser sender = CaffeineUser.fromJson(json.getAsJsonObject("publisher"));
                            JsonObject body = json.getAsJsonObject("body");
                            String id = getMessageId(json.get("id").getAsString());

                            switch (type) {
                                case REACTION:
                                    ChatEvent chatEvent = new ChatEvent(sender, body.get("text").getAsString(), id);

                                    if (json.has("endorsement_count")) {
                                        listener.onUpvote((new UpvoteEvent(chatEvent, json.get("endorsement_count").getAsInt())));
                                    } else {
                                        listener.onChat(chatEvent);
                                    }

                                    return;

                                case SHARE:
                                    ShareEvent shareEvent = new ShareEvent(sender, body.get("text").getAsString(), id);

                                    if (json.has("endorsement_count")) {
                                        listener.onUpvote((new UpvoteEvent(shareEvent, json.get("endorsement_count").getAsInt())));
                                    } else {
                                        listener.onShare(shareEvent);
                                    }

                                    return;

                                case DIGITAL_ITEM:
                                    JsonObject propJson = body.getAsJsonObject("digital_item");
                                    CaffeineProp prop = CaffeineProp.fromJson(propJson);
                                    PropEvent propEvent = new PropEvent(sender, body.get("text").getAsString(), id, propJson.get("count").getAsInt(), prop);

                                    if (json.has("endorsement_count")) {
                                        listener.onUpvote((new UpvoteEvent(propEvent, json.get("endorsement_count").getAsInt())));
                                    } else {
                                        listener.onProp(propEvent);
                                    }

                                    return;

                                case FOLLOW:
                                    listener.onFollow(new FollowEvent(sender));

                                    return;

                                case UNKNOWN: // hush mr compiley
                                    return;
                            }
                        }
                    }
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (listener != null) {
                listener.onClose(remote);
            }
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
        }

    }

    private static String getMessageId(String b64) {
        byte[] bytes = Base64.getDecoder().decode(b64);
        JsonObject json = CaffeineApi.GSON.fromJson(new String(bytes), JsonObject.class);

        return json.get("u").getAsString();
    }

    @Override
    public void close() throws IOException {
        try {
            this.disconnectBlocking();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

}
