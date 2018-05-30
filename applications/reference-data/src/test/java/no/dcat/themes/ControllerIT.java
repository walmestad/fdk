package no.dcat.themes;


import no.dcat.shared.Subject;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

@Profile("unit-integration")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ControllerIT {

    @Autowired
    Controller controller;

    @Test
    @WithMockUser(authorities = "INTERNAL_CALL")
    public void addSubject() throws Throwable {

        controller.getRemoteResourceForSubject("https://data-david.github.io/Begrep/begrep/Organisasjonsnummer");

        Subject subject1 = controller.getRemoteResourceForSubject("http://localhost:8950/subject/s1");
        Assert.assertThat(subject1.getPrefLabel().get("no"), Is.is("s1"));

        Subject subject2 = controller.getRemoteResourceForSubject("http://localhost:8950/subject/S200");
        Assert.assertThat(subject2.getPrefLabel().get("no"), Is.is("S200"));

    }
}
