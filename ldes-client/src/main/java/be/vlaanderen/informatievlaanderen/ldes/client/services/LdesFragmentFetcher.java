package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.LdesFragment;

public interface LdesFragmentFetcher {

	LdesFragment fetchFragment(String fragmentUrl);
}
