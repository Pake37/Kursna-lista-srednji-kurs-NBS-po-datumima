package com.nbs_kurs_po_datumu;

import com.google.api.client.http.HttpResponseException;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.nbs_kurs_po_datumu.SheetandJava.*;

public class Main {
    public static void main(String[] args) {
        try {

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            long delay = 1000L;
            long period = 1000L;

            TimerTask task = new TimerTask() {
                public void run() {
                    try {
                        final String url = "https://webservices.nbs.rs/CommunicationOfficeService1_0/ExchangeRateXmlService.asmx?op=GetCurrentExchangeRate";

                        final URL obj = new URL((url));

                        final HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                        con.setRequestMethod("POST");
                        con.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");

                        String prvi_datum = preuzimanjeDatumaIzGoogleSheet();
                        String datum = prvi_datum.replace("[", "").replace("]", "").replace("-", "").trim();



                        String countryCode = "3";
//                      countryCode служи за избор у API-ју "Народне Банке Србије" да ли желимо податке за текући курс(1) или за курс по датуму(3)или за некии други критеријум
                        String korisnik_nbs_aplikacija = System.getenv("nbs_kurs_evro_korisnik_nbs_aplikacija");
                        String sifra_nbs_aplikacija = System.getenv("nbs_kurs_evro_sifra_nbs_aplikacija");
                        String licenca_id_nbs_aplikacija = System.getenv("nbs_kurs_evro_licenca_id_nbs_aplikacija");
//                      Код изнад служи за персоналне податке појединачног корисника којима желимо приступити апликацији "Народне Банке Србије"
//                      Сви подаци корисника у коду изнад су у "Environment variable" варијаблама "Windows" окружења, што повећава безбедност апликације

                        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                                "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">" +
                                "<soap12:Header> <AuthenticationHeader xmlns=\"http://communicationoffice.nbs.rs\"><UserName>" + korisnik_nbs_aplikacija + "</UserName><Password>" + sifra_nbs_aplikacija + "</Password><LicenceID>" + licenca_id_nbs_aplikacija + "</LicenceID></AuthenticationHeader></soap12:Header>" +
                                " <soap12:Body> " +
                                " <GetExchangeRateByDate xmlns=\"http://communicationoffice.nbs.rs\"> " +
                                "<date>" + datum + "</date>" +
                                " <exchangeRateListTypeID>" + countryCode + "</exchangeRateListTypeID>" +
                                " </GetExchangeRateByDate>" +
                                " </soap12:Body>" +
                                "</soap12:Envelope>";
//                      Помоћу варијабли "datum i countryCode" које додајемо линку омогућавамо кориснички унос жељених података
                        con.setDoOutput(true);
                        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                        wr.writeBytes(xml);
                        wr.flush();
                        wr.close();
                        con.getResponseMessage();
                        System.out.println(con.getResponseMessage());
                        BufferedReader in = new BufferedReader(new InputStreamReader(
                                con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        System.out.println(in);
                        in.close();

                        String xml_string = response.toString();

                        String xml_string_corrected = xml_string.replace("&lt;", "<").replace("&gt;", ">");

                        DocumentBuilder builder =
                                DocumentBuilderFactory.newInstance().newDocumentBuilder();
                        StringReader sr = new StringReader(xml_string_corrected);
                        InputSource is = new InputSource(sr);
                        Document doc = builder.parse(is);
                        doc.getDocumentElement().normalize();
                        NodeList nbs_tagovi = doc.getElementsByTagName("ExchangeRate");

                        for (int i = 0; i < nbs_tagovi.getLength(); i++) {
                            Node nbs_tekuca_kursna_lista_node = nbs_tagovi.item(i);

                            if (nbs_tekuca_kursna_lista_node.getNodeType() == Node.ELEMENT_NODE) {
                                Element nbs_tekuca_kursna_lista_element = (Element) nbs_tekuca_kursna_lista_node;

                                Element valutaKursa = (Element) nbs_tekuca_kursna_lista_element.getElementsByTagName("CurrencyNameSerLat").item(0);
                                Element srednjiKurs = (Element) nbs_tekuca_kursna_lista_element.getElementsByTagName("MiddleRate").item(0);

                                if (!srednjiKurs.getTextContent().equals("0.0") && valutaKursa.getTextContent().equals("Evro")) {

                                    String srednji = srednjiKurs.getTextContent().trim();
                                    Float mali = Float.parseFloat(srednji);
                                    System.out.println(mali);
                                    sheetsService = getSheetsService();
                                    ValueRange body = new com.google.api.services.sheets.v4.model.ValueRange().setValues(Arrays.asList(Arrays.asList(mali.toString().replace(".", ",").trim())));
                                    System.out.println(body.toString());
                                    UpdateValuesResponse response2 = sheetsService.spreadsheets().values().update(SheetandJava.SPREADSHEET_ID, "B3", body).setValueInputOption("RAW").execute();
                                    System.out.println(response2.toString());
                                }

                                if ("Evro".equals(valutaKursa.getTextContent())) {

                                    sheetsService = getSheetsService();
                                    ValueRange body = new com.google.api.services.sheets.v4.model.ValueRange().setValues(Arrays.asList(Arrays.asList(valutaKursa.getTextContent())));

                                    sheetsService.spreadsheets().values().update(SheetandJava.SPREADSHEET_ID, "B2", body).setValueInputOption("RAW").execute();
                                }
                            }
                        }
                    } catch (HttpResponseException hre) {
                        hre.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            executor.scheduleAtFixedRate(task, delay, period, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}