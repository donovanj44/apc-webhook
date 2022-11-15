import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.FormsScopes;
import com.google.api.services.forms.v1.model.FormResponse;
import com.google.api.services.forms.v1.model.ListFormResponsesResponse;
import com.google.auth.oauth2.GoogleCredentials;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;
import org.rapidoid.setup.*;


import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;




public class Webhook extends Privates {

    final String question1Id = "059d7375", question2Id = "106fb4ba", question3Id = "01f8242e", question4Id = "69fbc838";

    String name, college, major;

    private static final String APPLICATION_NAME = "apc-webhook";
    private static Forms formsService;


    static {

        try {

            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();


            formsService = new Forms.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    jsonFactory, null)
                    .setApplicationName(APPLICATION_NAME).build();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args){
        On.get("/fire").html((Req req) -> {
            Resp resp = req.response();
            fireWebhook();
            return "resp";
        });
        On.post("/fire").html((Req req) -> {
            Resp resp = req.response();
            fireWebhook();
            return "resp";
        });


    }





    public static void fireWebhook() throws IOException {

        String token = getAccessToken();
        readResponses(Privates.formID, token);
        DiscordWebhook webhook = new DiscordWebhook(Privates.url);
        webhook.setAvatarUrl(Privates.avatarUrl);
        webhook.setContent("Test");
        webhook.execute();
    }

    public static String getAccessToken() throws IOException {
        GoogleCredentials credential = GoogleCredentials.fromStream(Objects.requireNonNull(
                Webhook.class.getResourceAsStream("creds.json"))).createScoped(FormsScopes.all());
        return credential.getAccessToken() != null ?
                credential.getAccessToken().getTokenValue() :
                credential.refreshAccessToken().getTokenValue();
    }

    public static void readResponses(String formId, String token) throws IOException {
        ListFormResponsesResponse responses = formsService.forms().responses().list(formId).setOauthToken(token).execute();
        JSONObject goodJson = new JSONObject(responses);
        JSONArray arr = goodJson.getJSONArray("responses");
        String responseID = arr.getJSONObject(arr.length() - 1).getString("responseId");
//        System.out.println(responses.toPrettyString());
        FormResponse response = formsService.forms().responses().get(formId, responseID).setOauthToken(token).execute();
//        System.out.println(response.toPrettyString());
        JSONObject responseJson = new JSONObject(response);


    }
}
