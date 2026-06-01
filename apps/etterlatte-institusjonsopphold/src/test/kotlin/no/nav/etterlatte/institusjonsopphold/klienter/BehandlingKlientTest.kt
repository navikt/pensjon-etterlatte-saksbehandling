package no.nav.etterlatte.institusjonsopphold.klienter

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.institusjonsopphold.kafka.KafkaOppholdHendelse
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdEkstern
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdKilde
import no.nav.etterlatte.institusjonsopphold.model.InstitusjonsoppholdsType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BehandlingKlientTest {
    private val institusjonsoppholdKlient = mockk<InstitusjonsoppholdKlient>()

    private fun lagBehandlingKlient(behandlingHttpClient: HttpClient) =
        BehandlingKlient(
            behandlingHttpClient = behandlingHttpClient,
            institusjonsoppholdKlient = institusjonsoppholdKlient,
            resourceUrl = "http://localhost",
        )

    private fun lagRecord(norskident: String) =
        ConsumerRecord(
            "topic",
            0,
            0L,
            1L,
            KafkaOppholdHendelse(
                hendelseId = 1L,
                oppholdId = 1L,
                norskident = norskident,
                type = InstitusjonsoppholdsType.INNMELDING,
                kilde = InstitusjonsoppholdKilde.INST,
            ),
        )

    @Test
    fun `Ugyldig personident i hendelse skal returnere uten å gjøre noe`() {
        val behandlingKlient = lagBehandlingKlient(mockk(relaxed = true))

        runBlocking { behandlingKlient.haandterHendelse(lagRecord("ikke-et-gyldig-fnr")) }

        coVerify(exactly = 0) { institusjonsoppholdKlient.hentDataForHendelse(any()) }
    }

    @Test
    fun `Gyldig personident i hendelse skal hente data og sende til behandling`() {
        val mockEngine =
            MockEngine {
                respond(
                    content = "{}",
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString())),
                )
            }
        val behandlingHttpClient =
            HttpClient(mockEngine) {
                install(ContentNegotiation) { jackson() }
            }
        val behandlingKlient = lagBehandlingKlient(behandlingHttpClient)

        coEvery { institusjonsoppholdKlient.hentDataForHendelse(1L) } returns
            InstitusjonsoppholdEkstern(
                oppholdId = 1L,
                tssEksternId = "123",
                startdato = LocalDate.now(),
            )

        runBlocking { behandlingKlient.haandterHendelse(lagRecord("10418305857")) }

        coVerify(exactly = 1) { institusjonsoppholdKlient.hentDataForHendelse(1L) }
    }
}
