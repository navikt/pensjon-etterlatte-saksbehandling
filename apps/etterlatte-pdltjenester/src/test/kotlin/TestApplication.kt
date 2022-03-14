package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.pdl.ParallelleSannheterKlient
import no.nav.etterlatte.person.PersonService


fun main() {
    val personServiceStub = PersonService(
        pdlKlient = mockk { coEvery { hentPerson(any(), any()) } returns mockResponse("/pdl/person.json") },
        ppsKlient = ParallelleSannheterKlient(
            httpClient = HttpClient(OkHttp) {
                install(JsonFeature) { serializer = JacksonSerializer(objectMapper) }
            },
            apiUrl = "https://pensjon-parallelle-sannheter.dev.intern.nav.no"
        )
    )

    val applicationContext = mockk<ApplicationContext>().apply {
        every { personService } returns personServiceStub
        every { securityMediator } returns SecurityContextMediatorStub
    }

    Server(applicationContext).run()
}
