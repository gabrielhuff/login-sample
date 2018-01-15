package com.gabrielhuff.sample.login.server

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class PostUserTest {

    @Autowired
    private lateinit var client: WebTestClient

    @Test
    fun `Create user succeeds`() {
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
                .expectStatus().isCreated
                .expectBody()
                .json("""
                {
                  "username": "user",
                  "skill_rx_java": 0.6,
                  "skill_docker": 0.5,
                  "skill_kotlin": 0.7
                }
                """)
    }

    @Test
    fun `Create user with username already used fails`() {
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
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `Create user with different credentials and body username fails`() {
        client.post().uri("/user")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q=")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody("""
                {
                  "username": "user2",
                  "skill_rx_java": 0.6,
                  "skill_docker": 0.5,
                  "skill_kotlin": 0.7
                }
                """)
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun `Create user with invalid credentials data fails`() {
        client.post().uri("/user")
                .header("Authorization", "Basic dXNlcsK1OnBhc3N3ZA==")
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
                .expectStatus().isBadRequest
    }

    @Test
    fun `Create user with invalid body data fails`() {
        client.post().uri("/user")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q=")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody("""
                {
                  "username": "user",
                  "skill_rx_java": 1.2,
                  "skill_docker": 0.5,
                  "skill_kotlin": 0.7
                }
                """)
                .exchange()
                .expectStatus().isBadRequest
    }

    @Test
    fun `Create user with missing body param fails`() {
        client.post().uri("/user")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q=")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody("""
                {
                  "username": "user"
                  "skill_rx_java": 0.6,
                  "skill_docker": 0.5
                }
                """)
                .exchange()
                .expectStatus().isBadRequest
    }

    @Test
    fun `Create user with invalid body format fails`() {
        client.post().uri("/user")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q=")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody("""
                {
                  "username": "user"
                  "skill_rx_java": 0.6,
                  "skill_docker": 0.5,
                  "skill_kotlin": 0.7
                }
                """)
                .exchange()
                .expectStatus().isBadRequest
    }

    @Test
    fun `Create user with invalid body content type fails`() {
        client.post().uri("/user")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q=")
                .syncBody("""
                {
                  "username": "user",
                  "skill_rx_java": 0.6,
                  "skill_docker": 0.5,
                  "skill_kotlin": 0.7
                }
                """)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    }

    @Test
    fun `Create user with invalid basic auth value fails`() {
        client.post().uri("/user")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q==")
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
                .expectStatus().isBadRequest
    }

    @Test
    fun `Create user with invalid authentication scheme fails`() {
        client.post().uri("/user")
                .header("Authorization", "Invalid dXNlcjpwYXNzd2Q=")
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
                .expectStatus().isBadRequest
    }
}