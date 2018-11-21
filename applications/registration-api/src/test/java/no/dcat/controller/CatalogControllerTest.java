package no.dcat.controller;

import no.dcat.authorization.EntityNameService;
import no.dcat.model.Catalog;
import no.dcat.service.CatalogRepository;
import no.dcat.service.EnhetService;
import no.dcat.shared.testcategories.UnitTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

@Category(UnitTest.class)
public class CatalogControllerTest {

    CatalogController spyCatalogController;
    CatalogRepository catalogRepository;

    EnhetService mockEnhetService;
    EntityNameService mockEntityNameService;


    @Before
    public void setup() {

        mockEnhetService = mock(EnhetService.class);
        mockEntityNameService = mock(EntityNameService.class);
        CatalogController catalogController = new CatalogController(catalogRepository, null, mockEntityNameService, null, mockEnhetService);
        spyCatalogController = Mockito.spy(catalogController);

    }

    @Test(expected = NullPointerException.class)
    public void checkGetPublisherIfUriIsNullShouldFail(){

        Catalog catalog = new Catalog();
        catalog.setUri(null);
        spyCatalogController.getPublisher(catalog);

    }

    @Test
    public void checkGetPublisherIfUriIsNotNullShouldFail(){

        mockEntityNameService = mock(EntityNameService.class);


        Enhet enhet = mock(Enhet.class);
        String orgNr = "12345";
        Catalog catalog = new Catalog();
        catalog.setId(orgNr);
        catalog.setUri("https://data.brreg.no/enhetsregisteret/api/enheter/");

        when(mockEnhetService.getByOrgNr(anyString(), anyString(), any())).thenReturn(enhet);

        when(mockEntityNameService.getOrganizationName(orgNr)).thenReturn("orgnametest");
        spyCatalogController.getPublisher(catalog);

    }


    }
