package be.vlaanderen.informatievlaanderen.ldes.client;

import org.apache.jena.riot.Lang;

import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesFragmentFetcher;
import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesFragmentFetcherImpl;
import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesService;
import be.vlaanderen.informatievlaanderen.ldes.client.services.LdesServiceImpl;

public class LdesClientImplFactory {

	public static LdesService getLdesService(Lang dataFormat) {
		return new LdesServiceImpl(dataFormat);
	}

	public static LdesService getLdesService(Lang dataSourceFormat, Lang dataDestinationFormat) {
		return new LdesServiceImpl(dataSourceFormat, dataDestinationFormat);
	}
	
	public static LdesFragmentFetcher getFragmentFetcher(Lang dataSourceFormat) {
		return new LdesFragmentFetcherImpl(dataSourceFormat);
	}
}
