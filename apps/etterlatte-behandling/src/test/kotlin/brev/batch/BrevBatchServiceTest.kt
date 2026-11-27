package no.nav.etterlatte.brev.batch

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.etterlatte.brev.BrevBatchRequest
import no.nav.etterlatte.brev.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.ktor.token.systembruker
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.ManglerTilgang
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.Claims
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

internal fun batchRequest(ids: List<Long> = listOf(1)) =
    BrevBatchRequest(
        ids.map(::SakId),
        "  Viktig informasjon  ",
        Slate(listOf(Slate.Element(Slate.ElementType.PARAGRAPH, listOf(Slate.InnerElement(text = "Teksten bevares nøyaktig."))))),
        Spraak.NB,
    )

class BrevBatchServiceTest {
    @Test
    fun `validerer unike positive saker og bevarer tekst`() {
        val request = batchRequest((1L..1000).toList() + listOf(1, 1000))
        val resultat = BrevBatchService.normaliser(request)
        assertEquals(1000, resultat.sakIder.size)
        assertEquals(request.tittel, resultat.tittel)
        assertEquals(request.innhold, resultat.innhold)
        listOf(emptyList(), listOf(0L), listOf(-1L), (1L..1001).toList()).forEach {
            assertThrows(UgyldigForespoerselException::class.java) { BrevBatchService.normaliser(batchRequest(it)) }
        }
    }

    @Test
    fun `fingeravtrykk er deterministisk uten tekstnormalisering`() {
        val a = BrevBatchService.normaliser(batchRequest(listOf(2, 1, 2)))
        val b = BrevBatchService.normaliser(batchRequest(listOf(1, 2)))
        assertEquals(BrevBatchService.fingeravtrykk(a), BrevBatchService.fingeravtrykk(b))
        assertNotEquals(BrevBatchService.fingeravtrykk(a), BrevBatchService.fingeravtrykk(a.copy(tittel = a.tittel.trim())))
    }

    @Test
    fun `tom tittel innhold og placeholders avvises`() {
        val request = batchRequest()
        listOf(
            request.copy(tittel = " "),
            request.copy(innhold = Slate()),
            request.copy(
                innhold =
                    Slate(
                        listOf(Slate.Element(Slate.ElementType.PARAGRAPH, listOf(Slate.InnerElement(text = "tekst", placeholder = true)))),
                    ),
            ),
        ).forEach {
            assertThrows(UgyldigForespoerselException::class.java) { BrevBatchService.normaliser(it) }
        }
    }

    @Test
    fun `bare konfigurert systembruker har lese og skrivetilgang`() {
        val dao = mockk<BrevBatchDao>()
        val service = BrevBatchService(dao, BrevBatchConfig { mapOf("BREV_BATCH_TILLATT_INNSENDER" to "godkjent") })
        val id = UUID.randomUUID()
        val tillatt = systembruker(mapOf(Claims.azp_name to "godkjent"))
        listOf(simpleSaksbehandler("godkjent"), systembruker()).forEach {
            assertThrows(ManglerTilgang::class.java) { service.opprett(it, id, batchRequest()) }
            assertThrows(ManglerTilgang::class.java) { service.hent(it, id) }
            assertThrows(ManglerTilgang::class.java) { service.hentSaker(it, id, 0, 100) }
        }
        assertThrows(ForespoerselException::class.java) { service.opprett(tillatt, id, batchRequest()) }
        every { dao.hent(id, "godkjent") } returns null
        assertThrows(GenerellIkkeFunnetException::class.java) { service.hent(tillatt, id) }
        verify(exactly = 0) { dao.opprett(any(), any(), any(), any()) }
        val lukket = BrevBatchService(dao, BrevBatchConfig { emptyMap() })
        assertThrows(ManglerTilgang::class.java) { lukket.autoriser(tillatt) }
    }
}
