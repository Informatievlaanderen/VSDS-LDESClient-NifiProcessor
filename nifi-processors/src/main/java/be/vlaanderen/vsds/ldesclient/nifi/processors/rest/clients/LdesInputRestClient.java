package be.vlaanderen.vsds.ldesclient.nifi.processors.rest.clients;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class LdesInputRestClient {
    private final Gson gson;

    public LdesInputRestClient(Gson gson) {
        this.gson = gson;
    }

    public JsonObject retrieveLdesInput(String url) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(url);

            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity httpEntity = response.getEntity();

            if (response.getStatusLine().getStatusCode() == 200) {
                return gson.fromJson(EntityUtils.toString(httpEntity), JsonObject.class);
            }
            else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
