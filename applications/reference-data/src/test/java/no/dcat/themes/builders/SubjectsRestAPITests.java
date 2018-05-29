package no.dcat.themes.builders;


import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.jena.ext.com.google.common.base.Utf8;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class SubjectsRestAPITests {

    private CloseableHttpClient httpClient = HttpClients.createDefault();

    @Test @Ignore
    public void testGetSubject()  {

            RestTemplate restTemplate = new RestTemplate();
        for(int i = 0; i < 100; i++) {
            ResponseEntity<String> responseEntity;
            try {
                responseEntity = restTemplate.getForEntity("http://localhost:8100/subject/uri=" + URLEncoder.encode("http://localhost:8950/s" + i), String.class);
                MediaType contentType = responseEntity.getHeaders().getContentType();
                HttpStatus statusCode = responseEntity.getStatusCode();
                String kake = responseEntity.getBody();
                System.out.println(URLDecoder.decode(kake));
            }
            catch(HttpClientErrorException e){
                System.out.println(e);
            }


        }

    }
}








