package be.vlaanderen.informatievlaanderen.ldes.client.valueobjects;

import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;

import java.io.IOException;

import static java.util.Arrays.stream;

public class LdesFragment {
    private static final String IMMUTABLE = "immutable";
    private static final String MAX_AGE = "max-age";

    public LdesFragment() {
        this.model = ModelFactory.createDefaultModel();
    }

    private Long maxAge;
    private final Model model;

    public Long getMaxAge() {
        return maxAge;
    }

    public Model getModel() {
        return model;
    }

    public static LdesFragment fromURL(String url) {
        LdesFragment ldesFragment = new LdesFragment();

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();

            HttpResponse httpResponse = httpClient.execute(new HttpGet(url));

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
