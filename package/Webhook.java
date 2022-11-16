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
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.time.LocalDateTime;


public class Webhook extends Privates {

    final String question1Id = "059d7375", question2Id = "106fb4ba", question3Id = "01f8242e", question4Id = "69fbc838";

    String name, college, major;

    public static LocalDateTime dateTime = LocalDateTime.parse("2017-01-14T15:32:56.000");
    public static LocalDateTime checkLDT;

    public static FormResponse check;


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


    public static void main(String[] args) {
        On.get("/fire").html((Req req) -> {
            Resp resp = req.response();
            fireWebhook();
            return "fired";
        });

    }


    public static void fireWebhook() throws IOException {

        String token = getAccessToken();
        readResponses(Privates.formID, token);
        DiscordWebhook webhook = new DiscordWebhook(Privates.url);
        webhook.setAvatarUrl(Privates.avatarUrl);
        webhook.setContent("Test");
//        webhook.execute();
    }

    public static String getAccessToken() throws IOException {
        GoogleCredentials credential = GoogleCredentials.fromStream(Objects.requireNonNull(
                Webhook.class.getResourceAsStream("creds.json"))).createScoped(FormsScopes.all());
        return credential.getAccessToken() != null ?
                credential.getAccessToken().getTokenValue() :
                credential.refreshAccessToken().getTokenValue();
    }

    public static void readResponses(String formId, String token) throws IOException {
        ListFormResponsesResponse responses = formsService.forms().responses().list(formId).setOauthToken(getAccessToken()).execute();
        JSONObject goodJson = new JSONObject(responses);
        JSONArray arr = goodJson.getJSONArray("responses");
        String responseID = arr.getJSONObject(arr.length() - 1).getString("responseId");
//        System.out.println(responses.toPrettyString());
        FormResponse response = formsService.forms().responses().get(formId, responseID).setOauthToken(token).execute();
//        System.out.println(response.toPrettyString());
        JSONObject responseJson = new JSONObject(response);
        for (int i = 0; i < arr.length(); i++){
            check = formsService.forms().responses().get(formId,responseID).setOauthToken(token).execute();
            checkLDT = LocalDateTime.parse(arr.getJSONObject(i).getString("lastSubmittedTime").substring(0, arr.getJSONObject(i).getString("lastSubmittedTime").length() - 3));
            if(checkLDT.isAfter(dateTime)){
                dateTime = checkLDT;
//                System.out.println(dateTime);
            }
        }
        ListFormResponsesResponse returnedResponse = formsService.forms().responses().list(formId).setOauthToken(getAccessToken()).setFilter("timestamp >= " + dateTime.toString() + "Z").execute();
        System.out.println(returnedResponse.toPrettyString());
        JSONObject returnedJSON = new JSONObject(returnedResponse);
        JSONArray arr2 = returnedJSON.getJSONArray("responses");
        JSONArray arr3 = arr2.getJSONArray(1);
    }

}
