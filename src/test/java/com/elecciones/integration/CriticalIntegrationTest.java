package com.elecciones.integration;

import com.elecciones.config.TestcontainersConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CriticalIntegrationTest extends TestcontainersConfig {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    private final String password = "Admin12345";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanRedis() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }

    @Test
    @Order(1)
    void rate_limit_login_blocks_sixth_attempt() {

        String email = "rate-" + UUID.randomUUID() + "@uni.edu";

        for (int i = 0; i < 5; i++) {

            ResponseEntity<String> response = post(
                    "/api/v1/auth/login",
                    Map.of(
                            "email", email,
                            "password", "wrong-password"));

            assertTrue(response.getStatusCode().is4xxClientError());
        }

        ResponseEntity<String> sixth = post(
                "/api/v1/auth/login",
                Map.of(
                        "email", email,
                        "password", "wrong-password"));

        assertEquals(429, sixth.getStatusCode().value());
    }

    @Test
    @Order(2)
    void jwt_filter_valid_missing_invalid_and_blacklisted_refresh() throws Exception {

        String adminEmail = uniqueEmail("admin");

        register(adminEmail, List.of("ADMIN", "AUDITOR"));

        JsonNode login = login(adminEmail);

        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();

        ResponseEntity<String> valid = get("/api/v1/elections", accessToken);
        assertNotEquals(401, valid.getStatusCode().value());

        ResponseEntity<String> missing = rest.getForEntity("/api/v1/elections", String.class);

        assertEquals(401, missing.getStatusCode().value());

        HttpHeaders invalidHeaders = new HttpHeaders();
        invalidHeaders.setBearerAuth("token-invalido");

        ResponseEntity<String> invalid = rest.exchange(
                "/api/v1/elections",
                HttpMethod.GET,
                new HttpEntity<>(invalidHeaders),
                String.class);

        assertEquals(401, invalid.getStatusCode().value());

        ResponseEntity<String> logout = postAuth(
                "/api/v1/auth/logout",
                Map.of("refreshToken", refreshToken),
                accessToken);

        assertTrue(logout.getStatusCode().is2xxSuccessful());

        ResponseEntity<String> refresh = post(
                "/api/v1/auth/refresh",
                Map.of("refreshToken", refreshToken));

        assertEquals(401, refresh.getStatusCode().value());
    }

    @Test
    @Order(3)
    void duplicate_vote_and_closed_election_return_conflict() throws Exception {

        String adminEmail = uniqueEmail("admin");
        String voterEmail = uniqueEmail("voter");

        register(adminEmail, List.of("ADMIN", "AUDITOR"));
        register(voterEmail, List.of("VOTER"));

        String adminToken = login(adminEmail).get("accessToken").asText();
        String voterToken = login(voterEmail).get("accessToken").asText();

        TestElectionData data = createCompleteElection(adminToken, "Eleccion Duplicado");

        activateElection(adminToken, data.electionId());

        ResponseEntity<String> firstVote = vote(voterToken, data.electionId(), data.listId());

        assertTrue(firstVote.getStatusCode().is2xxSuccessful());

        ResponseEntity<String> secondVote = vote(voterToken, data.electionId(), data.listId());

        assertEquals(409, secondVote.getStatusCode().value());

        TestElectionData closed = createCompleteElection(adminToken, "Eleccion Cerrada");

        activateElection(adminToken, closed.electionId());
        closeElection(adminToken, closed.electionId());

        ResponseEntity<String> voteClosed = vote(voterToken, closed.electionId(), closed.listId());

        assertEquals(409, voteClosed.getStatusCode().value());
    }

    @Test
    @Order(4)
    void complete_flow_create_positions_lists_activate_vote_results()
            throws Exception {

        String adminEmail = uniqueEmail("admin");
        String voterEmail = uniqueEmail("voter");

        register(adminEmail, List.of("ADMIN", "AUDITOR"));
        register(voterEmail, List.of("VOTER"));

        String adminToken = login(adminEmail).get("accessToken").asText();
        String voterToken = login(voterEmail).get("accessToken").asText();

        TestElectionData data = createCompleteElection(adminToken, "Flujo Completo");

        activateElection(adminToken, data.electionId());

        ResponseEntity<String> vote = vote(voterToken, data.electionId(), data.listId());

        assertTrue(vote.getStatusCode().is2xxSuccessful());

        ResponseEntity<String> results = get(
                "/api/v1/elections/" + data.electionId() + "/results",
                adminToken);

        assertEquals(200, results.getStatusCode().value());

        JsonNode json = objectMapper.readTree(results.getBody());

        assertTrue(json.toString().contains("votes"));
    }

    @Test
    @Order(5)
    void hmac_altered_payload_returns_invalid() throws Exception {

        String adminEmail = uniqueEmail("admin");
        String voterEmail = uniqueEmail("voter");

        register(adminEmail, List.of("ADMIN", "AUDITOR"));
        register(voterEmail, List.of("VOTER"));

        String adminToken = login(adminEmail).get("accessToken").asText();
        String voterToken = login(voterEmail).get("accessToken").asText();

        TestElectionData data = createCompleteElection(adminToken, "Eleccion HMAC");

        activateElection(adminToken, data.electionId());
        vote(voterToken, data.electionId(), data.listId());
        closeElection(adminToken, data.electionId());

        ResponseEntity<String> audit = get(
                "/api/v1/elections/" + data.electionId() + "/audit",
                adminToken);

        assertEquals(200, audit.getStatusCode().value());

        Map<String, Object> exported = objectMapper.readValue(audit.getBody(), Map.class);

        Map<String, Object> payload = (Map<String, Object>) exported.get("payload");

        payload.put("election_title", "ALTERADO");

        ResponseEntity<String> verify = postAuth(
                "/api/v1/audit/verify",
                exported,
                adminToken);

        assertEquals(200, verify.getStatusCode().value());

        JsonNode verifyJson = objectMapper.readTree(verify.getBody());

        assertFalse(verifyJson.get("valid").asBoolean());
    }

    @Test
    @Order(6)
    void concurrent_500_votes_same_user_persist_only_one_vote()
            throws Exception {

        String adminEmail = uniqueEmail("admin");
        String voterEmail = uniqueEmail("voter");

        register(adminEmail, List.of("ADMIN", "AUDITOR"));
        register(voterEmail, List.of("VOTER"));

        String adminToken = login(adminEmail).get("accessToken").asText();
        String voterToken = login(voterEmail).get("accessToken").asText();

        TestElectionData data = createCompleteElection(adminToken, "Eleccion Concurrente");

        activateElection(adminToken, data.electionId());

        ExecutorService executor = Executors.newFixedThreadPool(10);

        try {
            List<Callable<Integer>> tasks = IntStream.range(0, 500)
                    .mapToObj(i -> (Callable<Integer>) () -> vote(
                            voterToken,
                            data.electionId(),
                            data.listId()).getStatusCode().value())
                    .toList();

            List<Future<Integer>> responses = executor.invokeAll(
                    tasks,
                    90,
                    TimeUnit.SECONDS);

            long success = responses.stream()
                    .map(f -> {
                        try {
                            if (f.isCancelled()) {
                                return 500;
                            }
                            return f.get();
                        } catch (Exception e) {
                            return 500;
                        }
                    })
                    .filter(code -> code >= 200 && code < 300)
                    .count();

            assertEquals(1, success);

            ResponseEntity<String> results = get(
                    "/api/v1/elections/" + data.electionId() + "/results",
                    adminToken);

            assertEquals(200, results.getStatusCode().value());

            JsonNode json = objectMapper.readTree(results.getBody());

            assertTrue(
                    json.toString().contains("\"votes\":1"),
                    json.toPrettyString());

        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    private void register(String email, List<String> roles) {

        ResponseEntity<String> response = post(
                "/api/v1/auth/register",
                Map.of(
                        "email", email,
                        "password", password,
                        "roles", roles));

        assertTrue(
                response.getStatusCode().is2xxSuccessful()
                        || response.getStatusCode().value() == 409);
    }

    private JsonNode login(String email) throws Exception {

        ResponseEntity<String> response = post(
                "/api/v1/auth/login",
                Map.of(
                        "email", email,
                        "password", password));

        if (response.getStatusCode().value() == 429) {
            Thread.sleep(1500);

            response = post(
                    "/api/v1/auth/login",
                    Map.of(
                            "email", email,
                            "password", password));
        }

        assertEquals(200, response.getStatusCode().value());

        return objectMapper.readTree(response.getBody());
    }

    private TestElectionData createCompleteElection(
            String adminToken,
            String title) throws Exception {

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        ResponseEntity<String> electionResponse = postAuth(
                "/api/v1/elections",
                Map.of(
                        "title", title + " " + suffix,
                        "description", "Descripcion valida de prueba",
                        "startDate",
                        LocalDateTime.now()
                                .plusMinutes(10)
                                .withNano(0)
                                .toString(),
                        "endDate",
                        LocalDateTime.now()
                                .plusDays(1)
                                .withNano(0)
                                .toString()),
                adminToken);

        assertEquals(
                201,
                electionResponse.getStatusCode().value(),
                electionResponse.getBody());

        String electionId = objectMapper.readTree(electionResponse.getBody())
                .get("id")
                .asText();

        String rectorId = addPosition(adminToken, electionId, "Rector " + suffix);

        String decanoId = addPosition(adminToken, electionId, "Decano " + suffix);

        ResponseEntity<String> listResponse = postAuth(
                "/api/v1/elections/" + electionId + "/lists",
                Map.of(
                        "name", "Lista A " + suffix,
                        "description", "Lista Test",
                        "candidates", List.of(
                                Map.of(
                                        "positionId", rectorId,
                                        "fullName", "Juan Garcia",
                                        "career", "Derecho",
                                        "proposal", "Mejorar"),
                                Map.of(
                                        "positionId", decanoId,
                                        "fullName", "Ana Lopez",
                                        "career", "Ingenieria",
                                        "proposal", "Innovar"))),
                adminToken);

        assertEquals(
                201,
                listResponse.getStatusCode().value(),
                listResponse.getBody());

        String listId = objectMapper.readTree(listResponse.getBody())
                .get("id")
                .asText();

        return new TestElectionData(electionId, listId);
    }

    private String addPosition(
            String adminToken,
            String electionId,
            String name) throws Exception {

        ResponseEntity<String> response = postAuth(
                "/api/v1/elections/" + electionId + "/positions",
                Map.of("name", name),
                adminToken);

        assertEquals(
                201,
                response.getStatusCode().value(),
                response.getBody());

        return objectMapper.readTree(response.getBody())
                .get("id")
                .asText();
    }

    private void activateElection(String token, String electionId) {

        ResponseEntity<String> response = putAuth(
                "/api/v1/elections/" + electionId + "/activate",
                null,
                token);

        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    private void closeElection(String token, String electionId) {

        ResponseEntity<String> response = putAuth(
                "/api/v1/elections/" + electionId + "/close",
                null,
                token);

        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    private ResponseEntity<String> vote(
            String token,
            String electionId,
            String listId) {

        return postAuth(
                "/api/v1/elections/" + electionId + "/vote",
                Map.of("listId", listId),
                token);
    }

    private ResponseEntity<String> post(String url, Object body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return rest.postForEntity(
                url,
                new HttpEntity<>(body, headers),
                String.class);
    }

    private ResponseEntity<String> postAuth(
            String url,
            Object body,
            String token) {

        return rest.postForEntity(
                url,
                new HttpEntity<>(body, authHeaders(token)),
                String.class);
    }

    private ResponseEntity<String> putAuth(
            String url,
            Object body,
            String token) {

        return rest.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(body, authHeaders(token)),
                String.class);
    }

    private ResponseEntity<String> get(String url, String token) {

        return rest.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class);
    }

    private HttpHeaders authHeaders(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        return headers;
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@uni.edu";
    }

    private record TestElectionData(
            String electionId,
            String listId) {
    }
}