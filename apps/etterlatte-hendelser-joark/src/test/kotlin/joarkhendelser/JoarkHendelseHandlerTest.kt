package joarkhendelser

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import joarkhendelser.behandling.BehandlingKlient
import joarkhendelser.joark.SafKlient
import joarkhendelser.pdl.PdlKlient
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.joarkhendelser.JoarkHendelseHandler
import no.nav.etterlatte.joarkhendelser.joark.AvsenderMottaker
import no.nav.etterlatte.joarkhendelser.joark.Bruker
import no.nav.etterlatte.joarkhendelser.joark.BrukerIdType
import no.nav.etterlatte.joarkhendelser.joark.Dokument
import no.nav.etterlatte.joarkhendelser.joark.HendelseType
import no.nav.etterlatte.joarkhendelser.joark.HentJournalpostResult
import no.nav.etterlatte.joarkhendelser.joark.Journalpost
import no.nav.etterlatte.joarkhendelser.joark.JournalpostStatus
import no.nav.etterlatte.joarkhendelser.joark.Journalstatus
import no.nav.etterlatte.libs.common.person.NavPersonIdent
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JoarkHendelseHandlerTest {
    private val behandlingKlientMock = mockk<BehandlingKlient>()
    private val safKlientMock = mockk<SafKlient>()
    private val pdlKlientMock = mockk<PdlKlient>()

    private val sut = JoarkHendelseHandler(behandlingKlientMock, safKlientMock, pdlKlientMock)

    @AfterEach
    fun afterEach() {
        confirmVerified(behandlingKlientMock, safKlientMock, pdlKlientMock)
        clearAllMocks()
    }

    @Nested
    @DisplayName("Hendelser som ikke skal behandles")
    inner class HendelserSomIkkeSkalBehandles {
        @Test
        fun `Journalpost finnes ikke`() {
            val journalpostId = Random.nextLong()

            coEvery { safKlientMock.hentJournalpost(any()) } returns HentJournalpostResult(null, null)

            val hendelse = opprettHendelse(journalpostId)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) { safKlientMock.hentJournalpost(journalpostId) }
            coVerify {
                behandlingKlientMock wasNot Called
                pdlKlientMock wasNot Called
            }
        }

        @Test
        fun `Journalpost er allerede ferdigstilt`() {
            val journalpostId = Random.nextLong()

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                HentJournalpostResult(
                    opprettJournalpost(journalpostId, status = Journalstatus.FERDIGSTILT),
                    null,
                )

            val hendelse = opprettHendelse(journalpostId)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) { safKlientMock.hentJournalpost(journalpostId) }
            coVerify {
                behandlingKlientMock wasNot Called
                pdlKlientMock wasNot Called
            }
        }

        @Test
        fun `Journalpost mangler bruker`() {
            val journalpostId = Random.nextLong()

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                HentJournalpostResult(
                    opprettJournalpost(journalpostId, status = Journalstatus.FERDIGSTILT, bruker = null),
                    null,
                )

            val hendelse = opprettHendelse(journalpostId)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) { safKlientMock.hentJournalpost(journalpostId) }
            coVerify {
                behandlingKlientMock wasNot Called
                pdlKlientMock wasNot Called
            }
        }

        /**
         * TODO: Må avklare om vi noen gang skal behandle eller i det hele tatt vil motta noe hvor bruker er ORGNR.
         **/
        @Test
        fun `Journalpost tilhoerer organisasjon`() {
            val journalpostId = Random.nextLong()

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                HentJournalpostResult(
                    opprettJournalpost(
                        journalpostId,
                        status = Journalstatus.FERDIGSTILT,
                        bruker = Bruker("123456789", BrukerIdType.ORGNR),
                    ),
                    null,
                )

            val hendelse = opprettHendelse(journalpostId)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) { safKlientMock.hentJournalpost(journalpostId) }
            coVerify {
                behandlingKlientMock wasNot Called
                pdlKlientMock wasNot Called
            }
        }

        @Test
        fun `Bruker har kun NPID`() {
            val journalpostId = Random.nextLong()
            val journalpost = opprettJournalpost(journalpostId)

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                HentJournalpostResult(
                    journalpost,
                    null,
                )
            coEvery { pdlKlientMock.hentPdlIdentifikator(any()) } returns
                PdlIdentifikator.Npid(
                    NavPersonIdent("01309000000"),
                )

            val hendelse = opprettHendelse(journalpostId)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlKlientMock.hentPdlIdentifikator(journalpost.bruker!!.id)
            }
            coVerify {
                behandlingKlientMock wasNot Called
            }
        }

        @Test
        fun `Finner ingen brukerident i PDL`() {
            val journalpostId = Random.nextLong()
            val journalpost = opprettJournalpost(journalpostId)

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                HentJournalpostResult(
                    journalpost,
                    null,
                )
            coEvery { pdlKlientMock.hentPdlIdentifikator(any()) } returns null
            val hendelse = opprettHendelse(journalpostId)

            runBlocking {
                assertThrows<IllegalArgumentException> {
                    sut.haandterHendelse(hendelse)
                }
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlKlientMock.hentPdlIdentifikator(journalpost.bruker!!.id)
            }
            coVerify {
                behandlingKlientMock wasNot Called
            }
        }

        @Test
        fun `Ukjent hendelseType kan ikke behandles`() {
            val journalpostId = Random.nextLong()
            val journalpost = opprettJournalpost(journalpostId)

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                HentJournalpostResult(
                    journalpost,
                    null,
                )
            coEvery { pdlKlientMock.hentPdlIdentifikator(any()) } returns null
            val hendelse = opprettHendelse(journalpostId, hendelsesType = "UKJENT TYPE")

            runBlocking {
                assertThrows<IllegalArgumentException> {
                    sut.haandterHendelse(hendelse)
                }
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlKlientMock.hentPdlIdentifikator(journalpost.bruker!!.id)
            }
            coVerify {
                behandlingKlientMock wasNot Called
            }
        }
    }

    private fun opprettHendelse(
        journalpostId: Long,
        hendelsesType: String = HendelseType.JOURNALPOST_MOTTATT,
        journalpostStatus: String = JournalpostStatus.MOTTATT,
        temaGammelt: String? = null,
        temaNytt: String = "EYO",
    ): JournalfoeringHendelseRecord {
        return JournalfoeringHendelseRecord(
            UUID.randomUUID().toString(),
            1,
            hendelsesType,
            journalpostId,
            journalpostStatus,
            temaGammelt,
            temaNytt,
            "NAV_NO",
            "kanalReferanseId",
            "behandlingstema",
        )
    }

    private fun opprettJournalpost(
        journalpostId: Long,
        status: Journalstatus = Journalstatus.MOTTATT,
        bruker: Bruker? = Bruker("ident", BrukerIdType.FNR),
    ) = Journalpost(
        journalpostId = journalpostId.toString(),
        bruker = bruker,
        tittel = "Tittel",
        journalposttype = "journalposttype",
        journalstatus = status,
        dokument = listOf(Dokument("dokumentInfoId", "tittel", emptyList())),
        avsenderMottaker = AvsenderMottaker(bruker?.id, "Fornavn Etternavn", true),
        kanal = "kanal",
        datoOpprettet = "datoOpprettet",
    )
}
