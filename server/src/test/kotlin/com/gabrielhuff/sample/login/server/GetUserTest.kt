package com.gabrielhuff.sample.login.server

import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class GetUserTest {

    @Autowired
    private lateinit var client: WebTestClient

    private lateinit var token: String

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

        token = client.post().uri("/token")
                .header("Authorization", "Basic dXNlcjpwYXNzd2Q=")
                .exchange()
                .returnResult<String>()
                .responseBody
                .map { JSONObject(it) }
                .single().block()
                ?.getString("token") ?: throw IllegalStateException("Couldn't issue token")
    }

    @Test
    fun `Get user succeeds`() {
        client.get().uri("/user")
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus().isOk
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
    fun `Get user with invalid token fails`() {
        client.get().uri("/user")
                .header("Authorization", "Bearer ${token}_invalid")
                .exchange()
                .expectStatus().isUnauthorized
    }

    @Test
    fun `Get user with invalid authentication scheme fails`() {
        client.get().uri("/user")
                .header("Authorization", "Invalid $token")
                .exchange()
                .expectStatus().isBadRequest
    }
}