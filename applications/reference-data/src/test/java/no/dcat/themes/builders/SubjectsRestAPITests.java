package no.dcat.themes.builders;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

@Ignore
public class SubjectsRestAPITests {
    private final static Logger logger = LoggerFactory.getLogger(SubjectsRestAPITests.class);

    @Test
    public void testGetSubject() {

        TestRestTemplate restTemplate = new TestRestTemplate();

        for(int i = 0; i < 100; i++) {
            ResponseEntity<String> responseEntity;
            try {
                String subjectUrl = "http://dummyrestserver:8950/subject/s" + i;
                responseEntity = restTemplate
                        .withBasicAuth("user", "password")
                        .getForEntity("http://localhost:8100/subjects?uri=" + subjectUrl, String.class);

                logger.info("{} - response: {} {}", i, responseEntity.getStatusCode(), responseEntity.getBody());
            }
            catch(HttpClientErrorException e){
                System.out.println(e);
            }

        }

    }
}








