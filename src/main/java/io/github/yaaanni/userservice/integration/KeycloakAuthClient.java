package io.github.yaaanni.userservice.integration;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface KeycloakAuthClient {

    @PostExchange(
            url = "/realms/{realm}/protocol/openid-connect/token",
            contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    KeycloakTokenResponse getToken(
            @PathVariable String realm,
            @RequestBody MultiValueMap<String, String> formData
    );

    @PostExchange(
            url = "/realms/{realm}/protocol/openid-connect/logout",
            contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    void logout(
            @PathVariable String realm,
            @RequestBody MultiValueMap<String, String> formData
    );
}
