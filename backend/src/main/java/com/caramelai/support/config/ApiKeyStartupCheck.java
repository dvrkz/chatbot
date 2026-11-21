package com.caramelai.support.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails startup when the Google API key is missing or blank. The message names the variable
 * and never prints any part of the value (constitution Principle I).
 */
@Component
public class ApiKeyStartupCheck implements InitializingBean {

    static final String PROPERTY = "spring.ai.google.genai.api-key";

    private final Environment environment;

    public ApiKeyStartupCheck(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        String key;
        try {
            key = environment.getProperty(PROPERTY);
        } catch (IllegalArgumentException unresolved) {
            // An unresolved ${GOOGLE_API_KEY} placeholder means the variable is not set.
            key = null;
        }
        if (key == null || key.isBlank() || key.startsWith("${")) {
            throw new IllegalStateException(
                    "GOOGLE_API_KEY is not set. Add it to the .env file at the repository root "
                            + "or export it in the shell before starting the backend.");
        }
    }
}
