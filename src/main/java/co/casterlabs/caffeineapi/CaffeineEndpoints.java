package co.casterlabs.caffeineapi;

public class CaffeineEndpoints {
    // Prepend
    public static final String IMAGES = "https://images.caffeine.tv";
    public static final String ASSETS = "https://assets.caffeine.tv";

    // Formatted
    public static final String USERS = "https://api.caffeine.tv/v1/users/%s";
    public static final String FOLLOWERS = "https://api.caffeine.tv/v2/users/%s/followers?limit=100&offset=%d";
    public static final String FOLLOWING = "https://api.caffeine.tv/v2/users/%s/following?limit=100&offset=%d";
    public static final String SIGNED = "https://api.caffeine.tv/v1/users/%s/signed";
    public static final String CHAT_MESSAGE = "https://realtime.caffeine.tv/v2/reaper/stages/%s/messages";
    public static final String CHAT = "wss://realtime.caffeine.tv/v2/reaper/stages/%s/messages";
    public static final String UPVOTE_MESSAGE = "https://realtime.caffeine.tv/v2/reaper/messages/%s/endorsements";

    // Standalone
    public static final String GAMES_LIST = "https://api.caffeine.tv/v1/games";
    public static final String PROPS_LIST = "https://payments.caffeine.tv/store/get-digital-items";
    public static final String SIGNIN = "https://api.caffeine.tv/v1/account/signin";
    public static final String TOKEN = "https://api.caffeine.tv/v1/account/token";
    public static final String QUERY = "wss://realtime.caffeine.tv/public/graphql/query";

}
