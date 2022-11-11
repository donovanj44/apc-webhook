import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.FormsScopes;
import com.google.api.services.forms.v1.model.*;
import com.google.auth.oauth2.GoogleCredentials;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rapidoid.http.Resp;
import org.rapidoid.setup.On;

import java.net.*;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;


public class Webhook extends Privates {

    final String question1Id = "059d7375", question2Id = "106fb4ba", question3Id = "01f8242e", question4Id = "69fbc838";

    private static final String APPLICATION_NAME = "apc-webhook";
    private static Forms formsService;


    static {

        try {

            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();


            formsService = new Forms.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    jsonFactory, null)
                    .setApplicationName(APPLICATION_NAME).build();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {

        On.get("/fire").json()
//        On.get("/fire")((Req req)) -> {
//            Resp resp = req.response();
//        }

    }

    public static String getAccessToken() throws IOException {
        GoogleCredentials credential = GoogleCredentials.fromStream(Objects.requireNonNull(
                Webhook.class.getResourceAsStream("creds.json"))).createScoped(FormsScopes.all());
        return credential.getAccessToken() != null ?
                credential.getAccessToken().getTokenValue() :
                credential.refreshAccessToken().getTokenValue();
    }

    public static void fireWebhook() throws IOException {
        String token = getAccessToken();
        readResponses(formID, token);
        DiscordWebhook webhook = new DiscordWebhook(url);
        webhook.setAvatarUrl(avatarUrl);
        webhook.setContent("Test");
        webhook.execute();
    }


    private static void readResponses(String formId, String token) throws IOException {
        ListFormResponsesResponse responses = formsService.forms().responses().list(formId).setOauthToken(token).execute();
        JSONObject json = new JSONObject(responses);
        JSONArray arr = json.getJSONArray("responses");
        String responseID = arr.getJSONObject(arr.length() - 1).getString("responseId");
        System.out.println(responses.toPrettyString());
        FormResponse response = formsService.forms().responses().get(formId, responseID).setOauthToken(token).execute();
        System.out.println(response.toPrettyString());
    }
}
