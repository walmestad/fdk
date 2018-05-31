package no.dcat.dummyrest.test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SubjectsRestAPITests implements  Runnable{
    private static Logger logger = LoggerFactory.getLogger(SubjectsRestAPITests.class);


    @Ignore
    @Test
    public void testGetSubject() throws Throwable {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();
        for(int i = 0; i < 1000; i++){
            futures.add(executorService.submit(new SubjectsRestAPITests()));
        }
        for(Future future : futures){
            future.get();
        }
        executorService.shutdown();
    }

    public void run() {

        for (int i = 0; i < 10; i++) {
            System.out.println(".");
            TestRestTemplate restTemplate = new TestRestTemplate();
            ResponseEntity<String> responseEntity;
            try {
                String subjectUrl = "http://dummyrestserver:8950/subject/s" + i;
                responseEntity = restTemplate
                        .withBasicAuth("user", "password")
                        .getForEntity("http://localhost:8100/subjects?uri=" + subjectUrl, String.class);
                logger.info("{} - response: {} {}", responseEntity.getStatusCode(), responseEntity.getBody());
            } catch (HttpClientErrorException e) {
                System.out.println(e);
            } catch(org.apache.jena.tdb.TDBException e){
                logger.info("Logged TDBException ", e);
            }
            //Call the actuator
            ResponseEntity<ContainerMetrics> responseEntityActuator;
            try {
                responseEntityActuator = restTemplate
                        .withBasicAuth("actuator", "password")
                        .getForEntity("http://localhost:8100/metrics/", ContainerMetrics.class);
                Assert.assertEquals(HttpStatus.OK, responseEntityActuator.getStatusCode());
                logger.info("mem             : " + responseEntityActuator.getBody().getMem());
                logger.info("system load avg : " + responseEntityActuator.getBody().getSystemloadAverage());
                logger.info("threads         : " + responseEntityActuator.getBody().getThreads());
            } catch (HttpClientErrorException e) {
                System.out.println(e);
            }

        }
    }

}








