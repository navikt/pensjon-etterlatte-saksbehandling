package no.nav.etterlatte.brev.batch

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BrevBatchSakResultat
import no.nav.etterlatte.brev.BrevBatchSakStatus
import no.nav.etterlatte.brev.BrevBatchSteg
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.InformasjonsbrevUtsending
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus
import no.nav.etterlatte.brev.StrukturertInformasjonsbrevRequest
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.pensjon.brevbaker.api.model.BrevbakerType
import org.junit.jupiter.api.Test
import java.util.UUID

internal fun snapshotRequest() =
    StrukturertInformasjonsbrevRequest(
        Sak("12345678901", SakType.OMSTILLINGSSTOENAD, SakId(1), Enheter.defaultEnhet.enhetNr, null, false),
        Soeker("Test", null, "Person", BrevbakerType.Foedselsnummer("12345678901"), false, false, false, emptyList()),
        null,
        emptyList(),
        null,
        emptyList(),
        batchRequest().tittel,
        batchRequest().innhold,
        batchRequest().spraak,
    )

class BrevBatchJobServiceTest {
    private val dao = mockk<BrevBatchDao>()
    private val mapper = mockk<BrevBatchSnapshot>()
    private val klient = mockk<BrevKlient>()
    private var aktivert = true
    private val config = BrevBatchConfig { mapOf("BREV_BATCH_AKTIVERT" to aktivert.toString()) }
    private val service = BrevBatchJobService(dao, mapper, klient, config)
    private val bruker = HardkodaSystembruker.oppgave

    private fun arbeid(
        steg: BrevBatchSteg = BrevBatchSteg.GRUNNLAG,
        forsoek: Int = 1,
    ) = BrevBatchArbeid(
        BrevBatchSakResultat(UUID.randomUUID(), SakId(1), BrevBatchSakStatus.UNDER_ARBEID, steg, null, forsoek, null),
        batchRequest(),
        if (steg != BrevBatchSteg.GRUNNLAG) snapshotRequest() else null,
        UUID.randomUUID(),
    )

    private fun svar(
        a: BrevBatchArbeid,
        status: InformasjonsbrevUtsendingStatus,
    ) = InformasjonsbrevUtsending(a.resultat.utsendingsId, a.resultat.sakId, 42, status)

    private fun oppdateringer() {
        every { dao.oppdater(any(), any(), any(), any(), any(), any(), any()) } returns true
    }

    @Test
    fun `snapshot lagres foer opprettelse og samme brev sendes`() =
        runBlocking {
            val a = arbeid()
            val snapshot = snapshotRequest()
            oppdateringer()
            every { mapper.bygg(SakId(1), a.request) } returns snapshot
            coEvery { klient.opprettInformasjonsbrev(a.resultat.utsendingsId, snapshot, bruker) } answers {
                verify { dao.oppdater(a, BrevBatchSakStatus.UNDER_ARBEID, BrevBatchSteg.OPPRETT_BREV, any(), any(), snapshot, any()) }
                svar(a, InformasjonsbrevUtsendingStatus.OPPRETTET)
            }
            coEvery { klient.sendInformasjonsbrev(a.resultat.utsendingsId, SakId(1), bruker) } returns
                svar(a, InformasjonsbrevUtsendingStatus.FERDIG)
            service.behandle(a, bruker)
            verify { dao.oppdater(any(), BrevBatchSakStatus.FERDIG, BrevBatchSteg.FERDIG, 42, null, any(), any()) }
        }

    @Test
    fun `omstart leser ferdig brev og sender aldri igjen`() =
        runBlocking {
            val a = arbeid(BrevBatchSteg.SEND_BREV, 4)
            oppdateringer()
            coEvery { klient.hentInformasjonsbrev(a.resultat.utsendingsId, SakId(1), bruker) } returns
                svar(a, InformasjonsbrevUtsendingStatus.FERDIG)
            service.behandle(a, bruker)
            verify { dao.oppdater(any(), BrevBatchSakStatus.FERDIG, BrevBatchSteg.FERDIG, 42, null, any(), any()) }
            coVerify(exactly = 0) { klient.sendInformasjonsbrev(any(), any(), any()) }
        }

    @Test
    fun `send-timeout og omstart med uavklart resultat gir aldri blind retry`() =
        runBlocking {
            val a = arbeid(BrevBatchSteg.OPPRETT_BREV)
            oppdateringer()
            coEvery { klient.hentInformasjonsbrev(any(), any(), any()) } returns svar(a, InformasjonsbrevUtsendingStatus.OPPRETTET)
            coEvery { klient.sendInformasjonsbrev(any(), any(), any()) } throws IllegalStateException("Sensitiv melding skal ikke lagres")
            service.behandle(a, bruker)
            verify {
                dao.oppdater(
                    any(),
                    BrevBatchSakStatus.AVVENTER_AVKLARING,
                    BrevBatchSteg.SEND_BREV,
                    42,
                    "SEND_RESULTAT_UAVKLART",
                    any(),
                    any(),
                )
            }
            service.behandle(a.copy(resultat = a.resultat.copy(steg = BrevBatchSteg.SEND_BREV)), bruker)
            coVerify(exactly = 1) { klient.sendInformasjonsbrev(any(), any(), any()) }
        }

    @Test
    fun `paabegynt eksternt steg blokkerer sending`() =
        runBlocking {
            oppdateringer()
            for (status in listOf(
                InformasjonsbrevUtsendingStatus.JOURNALFOERING_PAABEGYNT,
                InformasjonsbrevUtsendingStatus.DISTRIBUSJON_PAABEGYNT,
            )) {
                val a = arbeid(BrevBatchSteg.OPPRETT_BREV)
                coEvery { klient.hentInformasjonsbrev(any(), any(), any()) } returns svar(a, status)
                service.behandle(a, bruker)
            }
            coVerify(exactly = 0) { klient.sendInformasjonsbrev(any(), any(), any()) }
        }

    @Test
    fun `midlertidig opprettelsesfeil retries begrenset uten nytt snapshot`() =
        runBlocking {
            oppdateringer()
            coEvery { klient.hentInformasjonsbrev(any(), any(), any()) } returns null
            coEvery { klient.opprettInformasjonsbrev(any(), any(), any()) } throws IllegalStateException("timeout")
            service.behandle(arbeid(BrevBatchSteg.OPPRETT_BREV, 1), bruker)
            service.behandle(arbeid(BrevBatchSteg.OPPRETT_BREV, 3), bruker)
            verify { dao.oppdater(any(), BrevBatchSakStatus.KLAR, BrevBatchSteg.OPPRETT_BREV, any(), "MIDLERTIDIG_FEIL", any(), any()) }
            verify { dao.oppdater(any(), BrevBatchSakStatus.FEILA, BrevBatchSteg.OPPRETT_BREV, any(), "MIDLERTIDIG_FEIL", any(), any()) }
            verify(exactly = 0) { mapper.bygg(any(), any()) }
        }

    @Test
    fun `saksfeil stopper ikke neste sak og stoppbryter sjekkes per sak`() =
        runBlocking {
            val a = arbeid()
            val b = arbeid()
            oppdateringer()
            every { dao.reserver() } returnsMany listOf(a, b, null)
            every { mapper.bygg(any(), any()) } throws BrevBatchSakFeil("SPRAAK_AVVIK")
            service.kjoer(bruker)
            verify(
                exactly = 2,
            ) { dao.oppdater(any(), BrevBatchSakStatus.FEILA, BrevBatchSteg.GRUNNLAG, any(), "SPRAAK_AVVIK", any(), any()) }
            aktivert = false
            service.kjoer(bruker)
            verify(exactly = 3) { dao.reserver() }
        }

    @Test
    fun `tapt eierskap stopper foer brev-api kall`() =
        runBlocking {
            val a = arbeid()
            every { mapper.bygg(any(), any()) } returns snapshotRequest()
            every { dao.oppdater(any(), any(), any(), any(), any(), any(), any()) } returns false
            service.behandle(a, bruker)
            coVerify(exactly = 0) { klient.opprettInformasjonsbrev(any(), any(), any()) }
            coVerify(exactly = 0) { klient.sendInformasjonsbrev(any(), any(), any()) }
        }
}
