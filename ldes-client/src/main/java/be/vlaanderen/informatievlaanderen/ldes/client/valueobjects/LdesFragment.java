package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static java.util.Arrays.stream;

public class LdesFragment {
    private static final String IMMUTABLE = "immutable";
    private static final String MAX_AGE = "max-age";

    private Long maxAge;
    private String fragmentId;
    private final Model model;

    public LdesFragment() {
        this.model = ModelFactory.createDefaultModel();
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public Model getModel() {
        return model;
    }

    public String getFragmentId() {
        return fragmentId;
    }

    public static LdesFragment fromURL(String url) {
        LdesFragment ldesFragment = new LdesFragment();

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();

            HttpClientContext context = HttpClientContext.create();

            ldesFragment.fragmentId = Optional.ofNullable(context.getRedirectLocations())
                    .flatMap(uris -> uris.stream().reduce((uri, uri2) -> uri2))
                    .map(URI::toString)
                    .orElse(url);

            HttpResponse httpResponse = httpClient.execute(new HttpGet(url), context);

            RDFParser.source(httpResponse.getEntity().getContent())
                    .forceLang(Lang.JSONLD11)
                    .parse(ldesFragment.model);

            stream(httpResponse.getHeaders("Cache-Control"))
                    .findFirst()
                    .ifPresent(header -> {
                        if (stream(header.getElements()).noneMatch(headerElement -> IMMUTABLE.equals(header.getName()))) {
                            ldesFragment.maxAge = stream(header.getElements())
                                    .filter(headerElement -> MAX_AGE.equals(headerElement.getName()))
                                    .findFirst()
                                    .map(HeaderElement::getValue)
                                    .map(Long::parseLong)
                                    .orElse(null);
                        }
                    });

            return ldesFragment;
        } catch (IOException e) {
            return ldesFragment;
        }
    }
}
