package io.everon;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ApigeeHttpClientTest {

    private static final String ORGANIZATION_NAME = "organization";
    private static final String PORTAL_NAME = "portal";

    private final ApigeeHttpClient client = new ApigeeHttpClient(ORGANIZATION_NAME, PORTAL_NAME);

    @Test
    void basicTest() {

        assertThat(client).isNotNull();

    }

}
