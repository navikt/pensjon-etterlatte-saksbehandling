package no.nav.etterlatte.joarkhendelser

import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.joarkhendelser.behandling.BehandlingKlient
import no.nav.etterlatte.joarkhendelser.behandling.BehandlingService
import no.nav.etterlatte.joarkhendelser.joark.Bruker
import no.nav.etterlatte.joarkhendelser.joark.BrukerIdType
import no.nav.etterlatte.joarkhendelser.joark.DokumentInfo
import no.nav.etterlatte.joarkhendelser.joark.Fagsak
import no.nav.etterlatte.joarkhendelser.joark.HendelseType
import no.nav.etterlatte.joarkhendelser.joark.Journalpost
import no.nav.etterlatte.joarkhendelser.joark.JournalpostResponse
import no.nav.etterlatte.joarkhendelser.joark.JournalpostStatus
import no.nav.etterlatte.joarkhendelser.joark.Journalstatus
import no.nav.etterlatte.joarkhendelser.joark.Kanal
import no.nav.etterlatte.joarkhendelser.joark.SafKlient
import no.nav.etterlatte.joarkhendelser.oppgave.OppgaveKlient
import no.nav.etterlatte.joarkhendelser.pdl.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.NavPersonIdent
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JoarkHendelseHandlerTest {
    private val behandlingKlientMock = mockk<BehandlingKlient>(relaxed = true)
    private val safKlientMock = mockk<SafKlient>()
    private val oppgaveKlient = mockk<OppgaveKlient>()
    private val pdlTjenesterKlientMock = mockk<PdlTjenesterKlient>()

    private lateinit var sut: JoarkHendelseHandler

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        behandlingKlientMock.also {
            coEvery { it.hentEllerOpprettSak(any(), any()) }.returns(SakId(101))
        }
        sut =
            JoarkHendelseHandler(
                BehandlingService(behandlingKlientMock),
                safKlientMock,
                oppgaveKlient,
                pdlTjenesterKlientMock,
            )
    }

    @AfterEach
    fun afterEach() {
        confirmVerified(behandlingKlientMock, safKlientMock, pdlTjenesterKlientMock)
    }

    @Nested
    @DisplayName("Gyldige hendelser som skal behandles")
    inner class HendelserSkalBehandles {
        @ParameterizedTest
        @EnumSource(SakType::class)
        fun `Journalpost mottatt skal behandles hvis ikke ferdigstilt`(sakType: SakType) {
            val journalpostId = Random.nextLong()
            val ident = "09498230323"
            val journalpost =
                opprettJournalpost(journalpostId, sakType = sakType, bruker = Bruker(ident, BrukerIdType.FNR))

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        journalpost,
                    ),
                )
            coEvery { pdlTjenesterKlientMock.hentPdlIdentifikator(any()) } returns
                PdlIdentifikator.FolkeregisterIdent(
                    Folkeregisteridentifikator.of(ident),
                )
            val hendelse = opprettHendelse(journalpostId, sakType.tema, HendelseType.JOURNALPOST_MOTTATT)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlTjenesterKlientMock.hentPdlIdentifikator(ident)
                behandlingKlientMock.hentEllerOpprettSak(ident, sakType)
                behandlingKlientMock.opprettOppgave(any(), any(), journalpostId.toString())
            }
        }

        @Test
        fun `Hvis bruker har NDIP så skal det opprettes manuel journalføringsoppgave`() {
            val journalpostId = Random.nextLong()
            val journalpost = opprettJournalpost(journalpostId)

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        journalpost,
                    ),
                )

            coEvery { pdlTjenesterKlientMock.hentPdlIdentifikator(any()) } returns
                PdlIdentifikator.Npid(
                    NavPersonIdent("01309000000"),
                )
            coEvery { oppgaveKlient.opprettManuellJournalfoeringsoppgave(any(), any()) } returns Unit

            val hendelse = opprettHendelse(journalpostId, SakType.OMSTILLINGSSTOENAD.tema)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlTjenesterKlientMock.hentPdlIdentifikator(journalpost.bruker!!.id)
                oppgaveKlient.opprettManuellJournalfoeringsoppgave(journalpostId = journalpostId, hendelse.temaNytt)
            }
        }

        @ParameterizedTest
        @EnumSource(SakType::class)
        fun `Journalpost med TEMA_ENDRET skal behandles hvis ikke ferdigstilt`(sakType: SakType) {
            val journalpostId = Random.nextLong()
            val ident = "09498230323"
            val journalpost =
                opprettJournalpost(journalpostId, sakType = sakType, bruker = Bruker(ident, BrukerIdType.FNR))

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        journalpost,
                    ),
                )

            coEvery { pdlTjenesterKlientMock.hentPdlIdentifikator(any()) } returns
                PdlIdentifikator.FolkeregisterIdent(
                    Folkeregisteridentifikator.of(ident),
                )
            val hendelse = opprettHendelse(journalpostId, sakType.tema, HendelseType.TEMA_ENDRET)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlTjenesterKlientMock.hentPdlIdentifikator(ident)
                behandlingKlientMock.hentEllerOpprettSak(ident, sakType)
                behandlingKlientMock.opprettOppgave(any(), any(), journalpostId.toString())
            }
        }

        @ParameterizedTest
        @EnumSource(SakType::class)
        fun `Journalpost med ENDELIG_JOURNALFOERT skal behandles hvis ikke ferdigstilt`(sakType: SakType) {
            val journalpostId = Random.nextLong()
            val ident = "09498230323"
            val journalpost =
                opprettJournalpost(journalpostId, sakType = sakType, bruker = Bruker(ident, BrukerIdType.FNR))

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        journalpost,
                    ),
                )

            coEvery { pdlTjenesterKlientMock.hentPdlIdentifikator(any()) } returns
                PdlIdentifikator.FolkeregisterIdent(
                    Folkeregisteridentifikator.of(ident),
                )
            coEvery { behandlingKlientMock.hentSak(any(), any()) } returns null

            val hendelse = opprettHendelse(journalpostId, sakType.tema, HendelseType.ENDELIG_JOURNALFOERT)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlTjenesterKlientMock.hentPdlIdentifikator(ident)
                behandlingKlientMock.hentSak(ident, sakType)
                behandlingKlientMock.hentEllerOpprettSak(ident, sakType)
                behandlingKlientMock.opprettOppgave(any(), any(), journalpostId.toString())
            }
        }

        @ParameterizedTest
        @EnumSource(SakType::class)
        fun `Journalpost med JOURNALPOST_UTGAATT skal avbryte tilhørende oppgaver`(sakType: SakType) {
            val journalpostId = Random.nextLong()
            val ident = "09498230323"
            val journalpost =
                opprettJournalpost(journalpostId, sakType = sakType, bruker = Bruker(ident, BrukerIdType.FNR))

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        journalpost,
                    ),
                )

            coEvery { pdlTjenesterKlientMock.hentPdlIdentifikator(any()) } returns
                PdlIdentifikator.FolkeregisterIdent(
                    Folkeregisteridentifikator.of(ident),
                )

            val hendelse = opprettHendelse(journalpostId, sakType.tema, HendelseType.JOURNALPOST_UTGAATT)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlTjenesterKlientMock.hentPdlIdentifikator(ident)
                behandlingKlientMock.avbrytOppgaver(journalpostId.toString())
            }
        }
    }

    @Nested
    @DisplayName("Hendelser som har gammelt tema etterlatte skal lukke tidligere oppgaver")
    inner class HendelserSomSkalAvbryteOppgaver {
        @ParameterizedTest
        @EnumSource(SakType::class)
        fun `Skal lukke tidligere oppgaver tilknyttet journalposten`(sakType: SakType) {
            val journalpostId = Random.nextLong()
            val ident = "09498230323"
            val journalpost =
                opprettJournalpost(journalpostId, sakType = sakType, bruker = Bruker(ident, BrukerIdType.FNR))

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        journalpost,
                    ),
                )
            val hendelse =
                opprettHendelse(
                    journalpostId = journalpostId,
                    temaNytt = "GEN",
                    temaGammelt = sakType.tema,
                    hendelsesType = HendelseType.JOURNALPOST_UTGAATT,
                )

            runBlocking {
                sut.haandterHendelse(hendelse)
            }
            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                behandlingKlientMock.avbrytOppgaver(journalpostId.toString())
            }
            coVerify(exactly = 0) {
                pdlTjenesterKlientMock.hentPdlIdentifikator(ident)
                behandlingKlientMock.hentEllerOpprettSak(ident, sakType)
                behandlingKlientMock.opprettOppgave(any(), any(), journalpostId.toString())
            }
        }
    }

    @Nested
    @DisplayName("Hendelser som ikke skal behandles")
    inner class HendelserSomIkkeSkalBehandles {
        @Test
        fun `Journalpost finnes ikke`() {
            val journalpostId = Random.nextLong()

            coEvery { safKlientMock.hentJournalpost(any()) } returns JournalpostResponse()

            val hendelse = opprettHendelse(journalpostId, SakType.OMSTILLINGSSTOENAD.tema)

            runBlocking {
                assertThrows<NullPointerException> {
                    sut.haandterHendelse(hendelse)
                }
            }

            coVerify(exactly = 1) { safKlientMock.hentJournalpost(journalpostId) }
            coVerify {
                behandlingKlientMock wasNot Called
                pdlTjenesterKlientMock wasNot Called
            }
        }

        @Test
        fun `Journalpost er allerede ferdigstilt`() {
            val journalpostId = Random.nextLong()

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        opprettJournalpost(journalpostId, status = Journalstatus.FERDIGSTILT),
                    ),
                )

            val hendelse = opprettHendelse(journalpostId, SakType.OMSTILLINGSSTOENAD.tema)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) { safKlientMock.hentJournalpost(journalpostId) }
            coVerify {
                behandlingKlientMock wasNot Called
                pdlTjenesterKlientMock wasNot Called
            }
        }

        @Test
        fun `Journalpost mangler bruker`() {
            val journalpostId = Random.nextLong()

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        opprettJournalpost(journalpostId, status = Journalstatus.MOTTATT, bruker = null),
                    ),
                )

            coEvery { oppgaveKlient.opprettManuellJournalfoeringsoppgave(any(), any()) } just Runs

            val hendelse = opprettHendelse(journalpostId, SakType.OMSTILLINGSSTOENAD.tema)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                oppgaveKlient.opprettManuellJournalfoeringsoppgave(journalpostId, "EYO")
            }
            coVerify {
                behandlingKlientMock wasNot Called
                pdlTjenesterKlientMock wasNot Called
            }
        }

        /**
         * TODO: Må avklare om vi noen gang skal behandle eller i det hele tatt vil motta noe hvor bruker er ORGNR.
         **/
        @Test
        fun `Journalpost tilhoerer organisasjon`() {
            val journalpostId = Random.nextLong()

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        opprettJournalpost(
                            journalpostId,
                            status = Journalstatus.MOTTATT,
                            bruker = Bruker("123456789", BrukerIdType.ORGNR),
                        ),
                    ),
                )

            val hendelse = opprettHendelse(journalpostId, SakType.OMSTILLINGSSTOENAD.tema)

            runBlocking {
                assertThrows<IllegalStateException> {
                    sut.haandterHendelse(hendelse)
                }
            }

            coVerify(exactly = 1) { safKlientMock.hentJournalpost(journalpostId) }
            coVerify {
                behandlingKlientMock wasNot Called
                pdlTjenesterKlientMock wasNot Called
            }
        }

        @Test
        fun `Finner ingen brukerident i PDL`() {
            val journalpostId = Random.nextLong()
            val journalpost = opprettJournalpost(journalpostId)

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        journalpost,
                    ),
                )

            coEvery { pdlTjenesterKlientMock.hentPdlIdentifikator(any()) } returns null
            val hendelse = opprettHendelse(journalpostId, SakType.OMSTILLINGSSTOENAD.tema)

            runBlocking {
                assertThrows<IllegalStateException> {
                    sut.haandterHendelse(hendelse)
                }
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlTjenesterKlientMock.hentPdlIdentifikator(journalpost.bruker!!.id)
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
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        journalpost,
                    ),
                )

            coEvery { pdlTjenesterKlientMock.hentPdlIdentifikator(any()) } returns
                PdlIdentifikator.FolkeregisterIdent(
                    Folkeregisteridentifikator.of("09498230323"),
                )

            val hendelse = opprettHendelse(journalpostId, "EYO", hendelsesType = "UKJENT TYPE")

            runBlocking {
                assertThrows<IllegalArgumentException> {
                    sut.haandterHendelse(hendelse)
                }
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlTjenesterKlientMock.hentPdlIdentifikator(journalpost.bruker!!.id)
            }
            coVerify {
                behandlingKlientMock wasNot Called
            }
        }

        @ParameterizedTest
        @EnumSource(SakType::class)
        fun `Journalpost med ENDELIG_JOURNALFOERT skal ikke behandles hvis sakId finnes og status er ferdigstilt`(sakType: SakType) {
            val journalpostId = Random.nextLong()
            val ident = "09498230323"
            val journalpost =
                opprettJournalpost(journalpostId, sakType = sakType, bruker = Bruker(ident, BrukerIdType.FNR))

            coEvery { safKlientMock.hentJournalpost(any()) } returns
                JournalpostResponse(
                    JournalpostResponse.ResponseData(
                        journalpost,
                    ),
                )

            coEvery { pdlTjenesterKlientMock.hentPdlIdentifikator(any()) } returns
                PdlIdentifikator.FolkeregisterIdent(
                    Folkeregisteridentifikator.of(ident),
                )
            coEvery { behandlingKlientMock.hentSak(any(), sakType) } returns 1L

            val hendelse = opprettHendelse(journalpostId, sakType.tema, HendelseType.ENDELIG_JOURNALFOERT)

            runBlocking {
                sut.haandterHendelse(hendelse)
            }

            coVerify(exactly = 1) {
                safKlientMock.hentJournalpost(journalpostId)
                pdlTjenesterKlientMock.hentPdlIdentifikator(ident)
                behandlingKlientMock.hentSak(ident, sakType)
            }
        }
    }

    private fun opprettHendelse(
        journalpostId: Long,
        temaNytt: String,
        hendelsesType: String = HendelseType.JOURNALPOST_MOTTATT,
        journalpostStatus: String = JournalpostStatus.MOTTATT,
        temaGammelt: String? = null,
    ): JournalfoeringHendelseRecord =
        JournalfoeringHendelseRecord(
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

    private fun opprettJournalpost(
        journalpostId: Long,
        status: Journalstatus = Journalstatus.MOTTATT,
        sakType: SakType = SakType.OMSTILLINGSSTOENAD,
        bruker: Bruker? = Bruker("ident", BrukerIdType.FNR),
        journalfoerendeEnhet: String? = null,
        dokumenter: List<DokumentInfo> = emptyList(),
    ) = Journalpost(
        journalpostId = journalpostId.toString(),
        bruker = bruker,
        tittel = "Tittel",
        journalstatus = status,
        sak = Fagsak("1", "EY", "FAGSAK", sakType.tema),
        kanal = Kanal.SKAN_IM,
        journalfoerendeEnhet = journalfoerendeEnhet,
        dokumenter = dokumenter,
    )
}
