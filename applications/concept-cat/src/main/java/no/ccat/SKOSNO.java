package no.ccat;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the
 * <a href="http://difi.no/skosno"> SKOS-NO Recommendation</a>.
 */
public class SKOSNO {
    /**
     * The namespace of the SKOS-NO vocabulary as a string
     */
    public static final String uri = "http://difi.no/skosno#";
    /**
     * The RDF model that holds the SKOS-NO entities
     */
    private static final Model m = ModelFactory.createDefaultModel();
    /**
     * The namespace of the SKOS-NO vocabulary
     */
    public static final Resource NAMESPACE = m.createResource(uri);
    /* ##########################################################
     * Defines SKOS-NO Classes
       ########################################################## */
    public static final Resource Definition = m.createResource(uri + "Definisjon");
    /* ##########################################################
     * Defines SKOS-XL Properties
       ########################################################## */
    public static final Property betydningsbeskrivelse = m.createProperty(uri + "betydningsbeskrivelse");

    /**
     * Returns the namespace of the SKOS-NO schema as a string
     *
     * @return the namespace of the SKOS-NO schema
     */
    public static String getURI() {
        return uri;
    }

}
