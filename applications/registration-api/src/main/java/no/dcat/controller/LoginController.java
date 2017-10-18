package no.dcat.controller;

import no.dcat.authorization.EntityNameService;
import no.dcat.config.FdkSamlUserDetails;
import no.dcat.configuration.SpringSecurityContextBean;
import no.dcat.model.Catalog;
import no.dcat.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static no.dcat.config.Roles.ROLE_USER;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class LoginController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    private CatalogController catalogController;
    private SpringSecurityContextBean springSecurityContextBean;

    private final EntityNameService entityNameService;

    @Autowired
    public LoginController(CatalogController catalogController, SpringSecurityContextBean springSecurityContextBean, EntityNameService entityNameService) {
        this.catalogController = catalogController;
        this.springSecurityContextBean = springSecurityContextBean;
        this.entityNameService = entityNameService;
    }


    @CrossOrigin
    @RequestMapping(value = "/innloggetBruker", method = GET)
    HttpEntity<User> innloggetBruker() {
        Authentication authentication = springSecurityContextBean.getAuthentication();

        User user = new User();

        if (authentication.getPrincipal() instanceof FdkSamlUserDetails) {
            FdkSamlUserDetails userDetails = (FdkSamlUserDetails) authentication.getPrincipal();
            user.setName(entityNameService.getUserName(userDetails.getUid()));
        } else {
            user.setName(entityNameService.getUserName(authentication.getName()));
        }

        logger.info("User {} logged in with authorizations: {}", user.getName(), authentication.getAuthorities().toString());

        List<String> catalogs= authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(catalog -> catalog.matches("\\d{9}"))
                .collect(toList());

        if (!catalogs.isEmpty()) {
            createCatalogsIfNeeded(catalogs);
        }

        if (authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals(ROLE_USER))) {
            return new ResponseEntity<>(user, OK);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/loginerror", method = GET)
    String getLoginError() {
        logger.debug("login error");

        Authentication authentication = springSecurityContextBean.getAuthentication();

        User user = new User();

        if (authentication.getPrincipal() instanceof FdkSamlUserDetails) {
            FdkSamlUserDetails userDetails = (FdkSamlUserDetails) authentication.getPrincipal();
            user.setName(entityNameService.getUserName(userDetails.getUid()));
        } else {
            user.setName(authentication.getName());
        }

        //temporary code below, to check that correct information about non-authorisation can be detected
        //(it can)

        StringBuilder userinfo = new StringBuilder();

        userinfo.append("Brukernavn: ");
        userinfo.append(user.getName()).append("\n");

        if(authentication.getAuthorities().size() <= 1) {
            userinfo.append("Bruker er ikke autorisert i Altinn");
        } else {
            userinfo.append("Autentiseringer: ").append(authentication.getAuthorities().toString());
        }

        return "Brukerinfo: " + userinfo.toString();
    }


    private void createCatalogsIfNeeded(Collection<String> organizations) {
        organizations.forEach(this::createCatalogIfNotExists);
    }

    private Optional<Catalog> createCatalogIfNotExists(String orgnr) {
        if (! orgnr.matches("\\d{9}")) {
            return Optional.empty();
        }

        HttpEntity<Catalog> catalogResponse = catalogController.getCatalog(orgnr);
        if (!((ResponseEntity) catalogResponse).getStatusCode().equals(HttpStatus.OK)) {
            logger.info("Create catalog for {} ", orgnr);
            Catalog catalog = new Catalog(orgnr);

            if (catalog.getTitle() == null) {
                 catalog.setTitle(new HashMap<>());
            }

            String organizationName = entityNameService.getOrganizationName(orgnr);
            if (organizationName != null) {
                catalog.addTitle("no", "Datakatalog for " + organizationName);
            }
            return Optional.of(catalogController.createCatalog(catalog).getBody());
        }
        return Optional.empty();
    }

}
