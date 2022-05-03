import java.io.IOException;

public class Webhook extends privates {
    public static void main(String []args) throws IOException{
        
        DiscordWebhook webhook = new DiscordWebhook(url);
        webhook.setAvatarUrl(avatarUrl);
        webhook.setContent("Test");
        webhook.execute();
    }
}
