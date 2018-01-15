package com.gabrielhuff.sample.login.server

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class PostTokenTest {

    @Autowired
    private lateinit var client: WebTestClient

    @Before
    fun setup() {
        client.post().uri("/user")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q=")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody("""
                {
                  "username": "user",
                  "skill_rx_java": 0.6,
                  "skill_docker": 0.5,
                  "skill_kotlin": 0.7
                }
                """)
                .exchange()
    }

    @Test
    fun `Issue token succeeds`() {
        client.post().uri("/token")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q=")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.token").isNotEmpty
    }

    @Test
    fun `Issue token without authentication fails`() {
        client.post().uri("/token")
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun `Issue token with invalid credentials fails`() {
        client.post().uri("/token")
                .header("Authorization", "Basic dXNlcjppbnZhbGlk")
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun `Issue token with invalid basic auth value fails`() {
        client.post().uri("/token")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q==")
                .exchange()
                .expectStatus().isBadRequest
    }

    @Test
    fun `Issue token with invalid authorization scheme fails`() {
        client.post().uri("/token")
                .header("Authorization", "Invalid dXNlcjpwYXNzd2Q=")
                .exchange()
                .expectStatus().isBadRequest
    }
}