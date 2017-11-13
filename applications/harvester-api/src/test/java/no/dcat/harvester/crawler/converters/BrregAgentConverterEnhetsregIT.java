package no.dcat.harvester.crawler.converters;

import no.dcat.harvester.HarvesterApplication;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.util.FileManager;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

public class BrregAgentConverterEnhetsregIT {
    Logger logger = LoggerFactory.getLogger(BrregAgentConverter.class);

    String expected;
    BrregAgentConverter converter = new BrregAgentConverter(HarvesterApplication.getBrregCache());


    @Test
    public void testConvertOnRDFWithIdentifier() throws Exception {
        BrregAgentConverter converter = new BrregAgentConverter(HarvesterApplication.getBrregCache());
        Model model = FileManager.get().loadModel("rdf/virksomheter.ttl");
        converter.collectFromModel(model);
        NodeIterator countryiter = model.listObjectsOfProperty(
                model.createResource("http://data.brreg.no/enhetsregisteret/enhet/991825827/forretningsadresse"),
                model.createProperty("http://data.brreg.no/meta/land"));
        assertEquals("Norge" , countryiter.next().asLiteral().getValue().toString());
    }

    @Test
    public void testConvertOnRDFReplaceCanonicalName() throws Exception {
        BrregAgentConverter converter = new BrregAgentConverter(HarvesterApplication.getBrregCache());
        Model model = FileManager.get().loadModel("rdf/virksomheter.ttl");
        converter.collectFromModel(model);
        NodeIterator nameiter = model.listObjectsOfProperty(
                model.createResource("http://data.brreg.no/enhetsregisteret/enhet/971040238"),
                FOAF.name);
        assertEquals("Kartverket" , nameiter.next().asLiteral().getValue().toString());
    }

    @Test
    public void organizationNameSubstitutionFromCanonicalTableOK() throws Throwable {
        Model model = FileManager.get().loadModel("oljedir.xml");

        Resource publisherResource = model.getResource("https://register.geonorge.no/register/organisasjoner/kartverket/difi");

        String previousName = publisherResource.getProperty(FOAF.name).getString();
        logger.info("name before {}",previousName);

        converter.collectFromModel(model);

        String newName = publisherResource.getProperty(FOAF.name).getString();

        OutputStream output2 = new ByteArrayOutputStream();
        model.write(output2, "TURTLE");
        logger.debug("[model_before_conversion] \n{}", output2.toString());

        logger.info("name after {}",newName);

        Assert.assertThat(newName, Is.is("DIFI"));
    }

    @Test
    public void organizationNumberFromGeonorgeForVegvesen() throws Throwable {
        Model model = FileManager.get().loadModel("geonorge-data-2017-10-19.xml");

        Resource publisherResource = model.getResource("https://register.geonorge.no/register/organisasjoner/kartverket/statens-vegvesen");

        String previousName = publisherResource.getProperty(FOAF.name).getString();
        logger.info("name before {}",previousName);

        converter.collectFromModel(model);

        String newName = publisherResource.getProperty(FOAF.name).getString();

        OutputStream output2 = new ByteArrayOutputStream();
        model.write(output2, "TURTLE");
        logger.debug("[model_before_conversion] \n{}", output2.toString());

        logger.info("name after {}",newName);

        Assert.assertThat(newName, Is.is("STATENS VEGVESEN"));
    }

    @Test
    public void organizationNumberInIdentifierUsesMasterRegistryNameOK() throws Throwable {
        Model model = FileManager.get().loadModel("oljedir.xml");

        Resource publisherResource = model.getResource("https://register.geonorge.no/register/organisasjoner/kartverket/oljedirektoratet");

        String previousName = publisherResource.getProperty(FOAF.name).getString();
        logger.info("name before {}",previousName);

        converter.collectFromModel(model);

        String newName = publisherResource.getProperty(FOAF.name).getString();

        OutputStream output2 = new ByteArrayOutputStream();
        model.write(output2, "TURTLE");
        logger.debug("[model_before_conversion] \n{}", output2.toString());

        logger.info("name after {}",newName);

        Assert.assertThat(newName, Is.is("OLJEDIREKTORATET"));
    }



    @Test
    public void organizationNameSubstitutionFromCanonicalTableWhenOfficalURIIsUsedOK() throws Throwable {
        Model model = FileManager.get().loadModel("oljedir.xml");

        Resource publisherResource = model.getResource("http://data.brreg.no/enhetsregisteret/enhet/971526920");

        String previousName = publisherResource.getProperty(FOAF.name).getString();
        logger.info("name before {}",previousName);

        converter.collectFromModel(model);

        String newName = publisherResource.getProperty(FOAF.name).getString();

        OutputStream output2 = new ByteArrayOutputStream();
        model.write(output2, "TURTLE");
        logger.debug("[model_before_conversion] \n{}", output2.toString());

        logger.info("name after {}",newName);

        Assert.assertThat(newName, Is.is("SSB"));
    }
}