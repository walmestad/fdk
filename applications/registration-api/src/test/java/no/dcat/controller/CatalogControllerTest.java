package no.dcat.controller;

import no.dcat.authorization.EntityNameService;
import no.dcat.model.Catalog;
import no.dcat.service.CatalogRepository;
import no.dcat.service.EnhetService;
import no.dcat.shared.Publisher;
import no.dcat.shared.testcategories.UnitTest;
import no.dcat.webutils.exceptions.BadRequestException;
import no.dcat.webutils.exceptions.NotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

@Category(UnitTest.class)
public class CatalogControllerTest {

    CatalogController spyCatalogController;
    @Mock
    CatalogRepository mockCatalogRepository;
    @Mock
    EnhetService mockEnhetService;
    @Mock
    EntityNameService mockEntityNameService;

    private String catalogId = "1234";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        CatalogController catalogController = new CatalogController(
            mockCatalogRepository,
            null,
            mockEntityNameService,
            null,
            mockEnhetService,
            "https://data.brreg.no/enhetsregisteret/api/enheter/");

        spyCatalogController = spy(catalogController);


    }

    @Test(expected = NullPointerException.class)
    public void checkGetPublisherIfUriIsNullShouldFail(){

        Catalog catalog = new Catalog();
        catalog.setUri(null);
        spyCatalogController.getPublisher(catalog);

    }

    @Test
    public void checkIfPublisherReturnSuccess(){

        //mockEntityNameService = mock(EntityNameService.class);

        Enhet enhet = mock(Enhet.class);
        String orgNr = "12345";
        Catalog catalog = new Catalog();
        catalog.setId(orgNr);
        catalog.setUri("https://data.brreg.no/enhetsregisteret/api/enheter/");

        when(mockEnhetService.getByOrgNr(anyString(), anyString(), any())).thenReturn(enhet);

        Publisher actual = spyCatalogController.getPublisher(catalog);

        Assert.assertThat(actual, is(notNullValue()));
    }

    @Test(expected = no.dcat.webutils.exceptions.NotFoundException.class)
    public void checkIfUpdateCatalogWithNullIdNotFound(){

        Catalog catalog = new Catalog();
        catalog.setId(catalogId);
        //when(mockCatalogRepository.findById(anyString())).thenReturn(Optional.of(catalog));

        spyCatalogController.updateCatalog("", catalog);
    }

}
