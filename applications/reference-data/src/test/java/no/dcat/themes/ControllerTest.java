package no.dcat.themes;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ControllerTest {

    @Autowired
    Controller controller;

    @Test
    @WithMockUser(authorities = "INTERNAL_CALL" )
    public void addSubject() throws  Throwable{

        //controller.getRemoteResourceForSubject("https://data-david.github.io/Begrep/begrep/Organisasjonsnummer");

        controller.getRemoteResourceForSubject("http://localhost:8950/subject/s001");

    }
}
