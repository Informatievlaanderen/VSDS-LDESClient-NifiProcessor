package be.vlaanderen.informatievlaanderen.ldes.client.services;

import java.util.List;

public interface LdesService {

    void populateFragmentQueue();
    boolean hasFragmentsToProcess();
    List<String[]> processNextFragment();
}
