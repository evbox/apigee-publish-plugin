package io.everon;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;

import java.net.ConnectException;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {8787})
class ApigeeHttpClientTest {

    private static final String ORGANIZATION_NAME = "organization";
    private static final String PORTAL_NAME = "portal";

    private MockServerClient mockServerClient;
    private final ApigeeHttpClient client = new ApigeeHttpClient(ORGANIZATION_NAME, PORTAL_NAME);

    @BeforeEach
    public void setup(MockServerClient mockServerClient) {
        this.mockServerClient = mockServerClient;
    }

    @Test
    void obtainApigeeAccessTokenSuccessfully() {

        String accessToken = "You shall pass!";
        String basicAuthToken = UUID.randomUUID().toString();
        client.setLoginUrl("http://127.0.0.1:8787/oauth/token");
        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/oauth/token")
                        .withHeader("Authorization", "Basic " + basicAuthToken), exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_OK)
                        .withBody("{ \"access_token\": \"" + accessToken + "\" }"));

        String fetchedAcessToken =
                client.obtainApigeeAccessToken("chucknorris", "chucknorris", basicAuthToken);

        assertThat(fetchedAcessToken).isEqualTo(accessToken);

    }

    @Test
    void obtainApigeeAccessTokenConnectionBrokenExceptionThrown() {

        String basicAuthToken = UUID.randomUUID().toString();
        client.setLoginUrl("http://127.0.0.1:8787/oauth/token");
        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/oauth/token")
                        .withHeader("Authorization", "Basic " + basicAuthToken), exactly(1))
                .respond(response -> {
                    throw new ConnectException("Connection exploded!");
                });

        assertThrows(RuntimeException.class,
                () -> client.obtainApigeeAccessToken("chucknorris", "chucknorris", basicAuthToken));

    }

    @Test
    void obtainApigeeAccessTokenNonSuccessStatusReturnedExceptionThrown() {

        String basicAuthToken = UUID.randomUUID().toString();
        client.setLoginUrl("http://127.0.0.1:8787/oauth/token");
        mockServerClient
                .when(request()
                        .withMethod("POST")
                        .withPath("/oauth/token")
                        .withHeader("Authorization", "Basic " + basicAuthToken), exactly(1))
                .respond(response()
                        .withStatusCode(HttpStatus.SC_BAD_REQUEST));

        assertThrows(RuntimeException.class,
                () -> client.obtainApigeeAccessToken("chucknorris", "chucknorris", basicAuthToken));

    }

}
