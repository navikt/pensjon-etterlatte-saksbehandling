package no.nav.etterlatte

import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import no.nav.etterlatte.hendelserpdl.module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplicationTest {
    @Test
    fun testRoot() {
        testApplication {
            application { this.module() }
            assertEquals(HttpStatusCode.OK, client.get("/").status)
        }
    }
}
