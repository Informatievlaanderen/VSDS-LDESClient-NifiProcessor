package be.vlaanderen.vsds.ldesclient.nifi.processors.models;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.StreamSupport;

public class LdesPage {

    private final JsonObject ldesPage;
    private final Gson gson;

    public LdesPage(JsonObject ldesPage, Gson gson) {
        this.ldesPage = ldesPage;
        this.gson = gson;
    }

    public String getPage() {
        return gson.toJson(ldesPage);
    }

    public JsonArray getLdesItems() {
        return ldesPage.getAsJsonArray("items");
    }

    public String getLdesMetadata() {
        JsonObject metadata = new JsonObject();
        metadata.add("@context", ldesPage.get("@context"));
        metadata.add("@type", ldesPage.get("@type"));
        metadata.add("viewOf", ldesPage.get("viewOf"));
        metadata.add("collectionInfo", ldesPage.get("collectionInfo"));
        return gson.toJson(metadata);
    }

    public String getPageId() {
        return ldesPage.get("@id").getAsString();
    }

    public Optional<String> getRelationId(String treeDirection) {
        Spliterator<JsonElement> iterator = ldesPage.get("tree:relation").getAsJsonArray().spliterator();
        return StreamSupport.stream(iterator, false)
                .map(JsonElement::getAsJsonObject)
                .filter(jsonObject -> (treeDirection).equals(jsonObject.get("@type").getAsString()))
                .map(jsonObject -> jsonObject.get("tree:node").getAsString())
                .findFirst();
    }
}
