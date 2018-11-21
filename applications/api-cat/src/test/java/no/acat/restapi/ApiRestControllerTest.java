package no.acat.restapi;

import no.acat.model.ApiDocument;
import no.acat.repository.ApiDocumentRepository;
import no.dcat.shared.testcategories.UnitTest;
import no.dcat.webutils.exceptions.NotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(UnitTest.class)
public class ApiRestControllerTest {

    ApiDocumentRepository apiDocumentRepository;

    private ApiRestController controller;

    @Before
    public void setup() {
        apiDocumentRepository = mock(ApiDocumentRepository.class);
        controller = new ApiRestController(apiDocumentRepository);
    }

    @Test
    public void testGetApiDocumentIfWithIdReturnSuccess() throws Exception {
        String id = "a";

        ApiDocument testDocument = new ApiDocument();
        when(apiDocumentRepository.getById(id)).thenReturn(Optional.of(testDocument));

        ApiDocument apiDocument = controller.getApiDocument(id);

        Assert.assertSame(testDocument, apiDocument);
    }

    @Test(expected = NotFoundException.class)
    public void checkIfIdGetResponseNotExistsAndShouldFailed() throws NotFoundException, IOException {
        String id = "b";

        when(apiDocumentRepository.getById(id)).thenReturn(Optional.empty());

        controller.getApiDocument(id);
    }
}