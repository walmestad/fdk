package no.dcat.themes.builders;


import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URLEncoder;

public class SubjectsRestAPITests {
    private static Logger logger = LoggerFactory.getLogger(SubjectsRestAPITests.class);


    @Test
    public void testGetSubject() throws Throwable  {

        TestRestTemplate restTemplate = new TestRestTemplate();
        restTemplate.withBasicAuth("user", "password");

        for(int i = 0; i < 2; i++) {
            ResponseEntity<String> responseEntity;
            try {
                String subjectUrl = "http://dummyrestserver:8950/subject/s" + i;
                responseEntity = restTemplate
                        .withBasicAuth("user", "password")
                        .getForEntity("http://localhost:8100/subjects?uri=" + subjectUrl, String.class);
                logger.info("{} - response: {} {}", i, responseEntity.getStatusCode(), responseEntity.getHeaders());
            }
            catch(HttpClientErrorException e){
                System.out.println(e);
            }


        }

    }
}








