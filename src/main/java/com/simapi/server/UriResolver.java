package com.simapi.server;

import com.simapi.config.ApiConfig;
import com.simapi.config.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Qualifier("uriResolver")
public class UriResolver {
    @Autowired()
    @Qualifier("apiConfig")
    private ApiConfig apiConfig;

    Mapping findUriMatchedMapping(String uri) {
        if (null == uri || null == apiConfig) {
            throw new IllegalArgumentException("uri and/or apiConfig can't be null");
        }
        if (uri.trim().length() == 0) {
            return null;
        }
        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        if (uri.startsWith("/")) {
            uri = uri.substring(1, uri.length());
        }

        List<Mapping> mappings = apiConfig.getMappings();
        for (Mapping mapping : mappings) {
            if (mapping.getUri().contains(uri)) {
                return mapping;
            }
        }
        return null;
    }

    public ApiConfig getApiConfig() {
        return apiConfig;
    }

    public void setApiConfig(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
    }
}
