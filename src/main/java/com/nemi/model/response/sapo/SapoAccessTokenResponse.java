package com.nemi.model.response.sapo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SapoAccessTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    private String scope;
}
