import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.*;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.FormsScopes;
import com.google.api.services.forms.v1.model.FormResponse;
import com.google.api.services.forms.v1.model.ListFormResponsesResponse;
import com.google.auth.oauth2.GoogleCredentials;
import org.json.*;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;
import org.rapidoid.setup.*;


import java.awt.*;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.time.LocalDateTime;


public class Webhook extends Privates {

    public static final String question1Id = "059d7375";
    public static final String question2Id = "106fb4ba";
    public static final String question3Id = "01f8242e";
    public static final String question4Id = "69fbc838";

    static String name;
    static String college;
    static String major;

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
//        webhook.setContent("Test");
        webhook.addEmbed(new DiscordWebhook.EmbedObject()
                .setTitle("New Senior Map Response!")
                .setColor(Color.RED)
                .addField("Name", name, false)
                .addField("College", college, false)
                .addField("Major", major, false)
//                .setThumbnail("https://kryptongta.com/images/kryptonlogo.png")
                .setUrl("https://apc-mhs.com/seniormap/"));
        webhook.addEmbed(new DiscordWebhook.EmbedObject());
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
        ListFormResponsesResponse responses = formsService.forms().responses().list(formId).setOauthToken(getAccessToken()).execute();
        JSONObject goodJson = new JSONObject(responses);
        JSONArray arr = goodJson.getJSONArray("responses");
        String responseID = arr.getJSONObject(arr.length() - 1).getString("responseId");
        FormResponse response = formsService.forms().responses().get(formId, responseID).setOauthToken(token).execute();
        JSONObject responseJson = new JSONObject(response);
        for (int i = 0; i < arr.length(); i++){
            check = formsService.forms().responses().get(formId,responseID).setOauthToken(token).execute();
            checkLDT = LocalDateTime.parse(arr.getJSONObject(i).getString("lastSubmittedTime").substring(0, arr.getJSONObject(i).getString("lastSubmittedTime").length() - 3));
            if(checkLDT.isAfter(dateTime)){
                dateTime = checkLDT;
            }
        }
        ListFormResponsesResponse returnedResponse = formsService.forms().responses().list(formId).setOauthToken(getAccessToken()).setFilter("timestamp >= " + dateTime.toString() + "Z").execute();
        JSONObject returnedJSON = new JSONObject(returnedResponse);
        name = getQuestionResponses(returnedJSON, question1Id) + getQuestionResponses(returnedJSON, question2Id);
        college = getQuestionResponses(returnedJSON, question3Id);
        major = getQuestionResponses(returnedJSON, question4Id);
    }

    public static String getQuestionResponses(JSONObject returnedJSON, String questionId){
        return returnedJSON.getJSONArray("responses")
                .getJSONObject(0)
                .getJSONObject("answers")
                .getJSONObject(questionId)
                .getJSONObject("textAnswers")
                .getJSONArray("answers")
                .getJSONObject(0)
                .getString("value");
    }

}
