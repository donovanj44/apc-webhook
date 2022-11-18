import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.*;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.FormsScopes;
import com.google.api.services.forms.v1.model.FormResponse;
import com.google.api.services.forms.v1.model.ListFormResponsesResponse;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.apache.commons.lang3.StringUtils;
import org.json.*;
import org.rapidoid.http.Req;
import org.rapidoid.setup.*;
import com.opencsv.*;
import org.apache.commons.io.*;
import org.json.*;
import com.fasterxml.jackson.dataformat.csv.*;
import com.fasterxml.jackson.databind.*;


import javax.print.DocFlavor;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


public class Webhook extends Privates {

    public static final String question1Id = "059d7375";
    public static final String question2Id = "106fb4ba";
    public static final String question3Id = "01f8242e";
    public static final String question4Id = "69fbc838";

    static String sheetName = "logos";
    static String range = "A1:B218";

    static String name;
    static String college;
    static String major;
    static String collegeImageUrl;


    public static LocalDateTime dateTime = LocalDateTime.parse("2017-01-14T15:32:56.000");
    public static LocalDateTime checkLDT;

    public static FormResponse check;


    private static final String APPLICATION_NAME = "apc-webhook";
    private static Forms formsService;
    private static Sheets sheetsService;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";


    static {

        try {

            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();


            formsService = new Forms.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, null).setApplicationName(APPLICATION_NAME).build();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        On.get("/fire").html((Req req) -> {
            fireWebhook();
            return "Fired " + name + " " + college + " " + major;
        });

    }


    public static void fireWebhook() throws Exception {

        String token = getAccessToken();
        readResponses(formID, token);
        DiscordWebhook webhook = new DiscordWebhook(url);
        webhook.setAvatarUrl(avatarUrl);
        webhook.setContent("New Submission");
        webhook.setUsername("Senior Map Alerts");
        webhook.setTts(false);
        webhook.addEmbed(new DiscordWebhook.EmbedObject().setTitle("New Senior Map Response!")
                .setColor(Color.RED)
                .addField("Name", name, false)
                .addField("College", college, true)
                .addField("Major", major, true)
                .setUrl("https://apc-mhs.com/seniormap/")
                .setThumbnail(getCollegeImage(college, generateArray(spreadsheetID)))
        );
        webhook.execute();
        System.out.println("Fired " + name + " " + college + " " + major);
    }

    public static String getAccessToken() throws IOException {
        GoogleCredentials credential = GoogleCredentials
                .fromStream(Objects.requireNonNull(Webhook.class.getResourceAsStream("creds.json")))
                .createScoped(FormsScopes.all());
        return credential.getAccessToken() != null ? credential
                .getAccessToken().getTokenValue() : credential
                .refreshAccessToken()
                .getTokenValue();
    }


    public static void readResponses(String formId, String token) throws IOException {
        ListFormResponsesResponse responses = formsService
                .forms()
                .responses()
                .list(formId)
                .setOauthToken(getAccessToken())
                .execute();
        JSONObject goodJson = new JSONObject(responses);
        JSONArray arr = goodJson.getJSONArray("responses");
        String responseID = arr.getJSONObject(arr.length() - 1).getString("responseId");
        for (int i = 0; i < arr.length(); i++) {
            check = formsService
                    .forms()
                    .responses()
                    .get(formId, responseID)
                    .setOauthToken(getAccessToken())
                    .execute();
            checkLDT = LocalDateTime.parse(arr.getJSONObject(i)
                    .getString("lastSubmittedTime")
                    .substring(0, arr.getJSONObject(i)
                            .getString("lastSubmittedTime")
                            .length() - 3));
            if (checkLDT.isAfter(dateTime)) {
                dateTime = checkLDT;
            }
        }
        ListFormResponsesResponse returnedResponse = formsService
                .forms()
                .responses()
                .list(formId)
                .setOauthToken(getAccessToken())
                .setFilter("timestamp >= " + dateTime.toString() + "Z")
                .execute();
        JSONObject returnedJSON = new JSONObject(returnedResponse);
        name = getQuestionResponses(returnedJSON, question1Id) + " " + getQuestionResponses(returnedJSON, question2Id);
        college = getQuestionResponses(returnedJSON, question3Id);
        major = getQuestionResponses(returnedJSON, question4Id);
    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = Webhook.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow
                .Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver
                .Builder()
                .setPort(8888)
                .build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static String[][] generateArray(String spreadsheetId) throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String range = "logos!A2:B218";
        Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        ValueRange response = service
                .spreadsheets()
                .values()
                .get(spreadsheetId, range)
                .execute();
        List<List<Object>> values = response.getValues();
        if (values == null || values.isEmpty()) {
            System.out.println("No data found.");
        } else {
            for (List row : values) {
                int i = 0;
            }
        }

        String[][] array = values.stream()
                .map(l -> l.stream().toArray(String[]::new))
                .toArray(String[][]::new);

        return array;
    }

    public static String getQuestionResponses(JSONObject returnedJSON, String questionId) {
        return returnedJSON
                .getJSONArray("responses")
                .getJSONObject(0)
                .getJSONObject("answers")
                .getJSONObject(questionId)
                .getJSONObject("textAnswers")
                .getJSONArray("answers")
                .getJSONObject(0)
                .getString("value");
    }

    public static String getCollegeImage(String college, String[][] array) {
        for (int i = 0; i < array.length + 2; i++)         {

            if (array[i][0].equals(college.trim())) {
                collegeImageUrl = array[i][1];
                return collegeImageUrl;
            }
        }
        return collegeImageUrl;
    }

}
