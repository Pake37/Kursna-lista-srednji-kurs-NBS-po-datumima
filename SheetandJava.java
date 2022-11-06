package com.nbs_kurs_po_datumu;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

public class SheetandJava {
protected static Sheets sheetsService;
private static String APPLICATION_NAME = "Primer";
protected static String SPREADSHEET_ID = System.getenv("nbs_kurs_evro_SPREADSHEET_ID");

private static Credential authorize() throws IOException, GeneralSecurityException{

    InputStream in = SheetandJava.class.getResourceAsStream(System.getenv("nbs_kurs_evro_fajl_sa_kredencijalima"));
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), new InputStreamReader(in));
    List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),clientSecrets,scopes).setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens"))).setAccessType("offline").build();
    Credential credential = new AuthorizationCodeInstalledApp(flow,new LocalServerReceiver()).authorize("user");
    return credential;
}

public static Sheets getSheetsService() throws IOException,GeneralSecurityException{
    Credential credential = authorize();
    return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(),JacksonFactory.getDefaultInstance(),credential ).setApplicationName(APPLICATION_NAME).build();

}

public static String preuzimanjeDatumaIzGoogleSheet() throws GeneralSecurityException, IOException {
    sheetsService = getSheetsService();
    String range = "Sheet1!G2";
    ValueRange response1 = sheetsService.spreadsheets().values().get(SPREADSHEET_ID,range).execute();
    List<List<Object>> values = response1.getValues();

    if(values == null || values.isEmpty() ){
        System.out.println("No values found.");
    }


    return values.get(0).toString();
}
/*
    public static void main(String[] args) {
        // GOOGLE SHEETS OVDE POČINJE
        try {
            sheetsService = getSheetsService();
            ValueRange body = new ValueRange().setValues(Arrays.asList(Arrays.asList("updated")));


            UpdateValuesResponse result = sheetsService.spreadsheets().values().update(SPREADSHEET_ID,"A2",body).setValueInputOption("RAW").execute();


        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
*/
}
