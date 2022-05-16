package be.vlaanderen.informatievlaanderen.ldes.client.services;

import java.util.List;

public interface LdesService {

    boolean hasPagesToProcess();
    String getNextPageUrl();
    List<String[]> processNextPage();
}
