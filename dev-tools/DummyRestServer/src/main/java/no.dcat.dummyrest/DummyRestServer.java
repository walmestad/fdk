package no.dcat.dummyrest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Random;

@RestController
public class DummyRestServer {

    private static Logger logger = LoggerFactory.getLogger(DummyRestServer.class);

    @RequestMapping(value = "/subject/{label}", produces = "text/turtle")
    public @ResponseBody
    ResponseEntity getSubject(@PathVariable("label") String label, HttpServletRequest request) {

        String begrep = "# Hovedenhet\n" +
                "@prefix rdf:\t<http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix rdfs:\t<http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "@prefix owl:\t<http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix dct:\t<http://purl.org/dc/terms/> .\n" +
                "@prefix skos: \t<http://www.w3.org/2004/02/skos/core#> .\n" +
                "@prefix xsd:\t<http://www.w3.org/2001/XMLSchema#> .\n" +
               // "@prefix :\t<http://dummyrestserver:8950/subject/> .\n" +
                "\n" +
                "<Hovedenhet>\n" +
                "\t\ta skos:Concept ;\n" +
                "\t\tskos:prefLabel \"XXYYXX\"@no ;\n" +
                "\t\tskos:definition \"enhet på øverste nivå i registreringsstrukturen i Enhetsregisteret\"@no ;\n" +
                "\t\tskos:note \"Enkeltpersonforetak, foreninger, selskap, sameier og andre som er registrert i Enhetsregisteret. Identifiseres med organisasjonsnummer.\"@no ;\n" +
                "\t\tdct:source <https://jira.brreg.no/browse/BEGREP-226> .";

        String uri = request.getRequestURL().toString();
        begrep = begrep.replace("Hovedenhet", uri).replace("XXYYXX", label);

        logger.info(begrep);

        ResponseEntity responseEntity = new ResponseEntity(begrep,null, HttpStatus.OK);
        Random random = new Random();

        int latency = random.nextInt(3000);
        boolean fail = random.nextInt(5) == 1;
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info(begrep);

        if(fail) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        else {
            return responseEntity;
        }
    }
}

