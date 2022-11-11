import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.FormsScopes;
import com.google.api.services.forms.v1.model.*;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;


public class Webhook extends Privates {

    private static final String APPLICATION_NAME = "apc-webhook";
    private static Drive driveService;
    private static Forms formsService;


    static {

        try {

            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            driveService = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    jsonFactory, null)
                    .setApplicationName(APPLICATION_NAME).build();

            formsService = new Forms.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    jsonFactory, null)
                    .setApplicationName(APPLICATION_NAME).build();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {

        String token = token = getAccessToken();
        String formID = createNewForm(token), responseID = "ACYDBNhRt_yvBT58aLZvybdmUCWQkyldz" +
                "Yun-d_LQ15naDKMovqMlal1hhstVvEMaA9CBBw";
        publishForm(formID,token);
        readResponses(formID, token);
        DiscordWebhook webhook = new DiscordWebhook(url);
        webhook.setAvatarUrl(avatarUrl);
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

    private static String createNewForm(String token) throws IOException {
        Form form = new Form();
        form.setInfo(new Info());
        form.getInfo().setTitle("New Form Quiz Created from Java");
        form = formsService.forms().create(form)
                .setAccessToken(token)
                .execute();
        return form.getFormId();
    }

    public static boolean publishForm(String formId, String token) throws GeneralSecurityException, IOException {

        PermissionList list = driveService.permissions().list(formId).setOauthToken(token).execute();

        if (!list.getPermissions().stream().filter((it) -> it.getRole().equals("reader")).findAny().isPresent()) {
            Permission body = new Permission();
            body.setRole("reader");
            body.setType("anyone");
            driveService.permissions().create(formId, body).setOauthToken(token).execute();
            return true;
        }

        return false;
    }

    private static void readResponses(String formId, String token) throws IOException {
        ListFormResponsesResponse response = formsService.forms().responses().list(formId).setOauthToken(token).execute();
        System.out.println(response.toPrettyString());
    }
}
