package no.nav.etterlatte

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.NotatService
import no.nav.etterlatte.brev.dokarkiv.DokarkivService
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.hentinformasjon.grunnlag.GrunnlagService
import no.nav.etterlatte.brev.notat.NotatMal
import no.nav.etterlatte.brev.notat.NotatRepository
import no.nav.etterlatte.brev.notat.StrukturertNotat
import no.nav.etterlatte.brev.pdfgen.PdfGeneratorKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.FormkravMedBeslutter
import no.nav.etterlatte.libs.common.behandling.InnstillingTilKabal
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.KabalHjemmel
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageOversendelsebrev
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZoneId
import javax.sql.DataSource
import kotlin.random.Random

@ExtendWith(GenerellDatabaseExtension::class)
class NotatServiceTest(
    dataSource: DataSource,
) {
    private val notatRepository = spyk(NotatRepository(dataSource))
    private val pdfGeneratorKlient = mockk<PdfGeneratorKlient>()
    private val dokarkivService = mockk<DokarkivService>()
    private val grunnlagService = mockk<GrunnlagService>()

    private val notatService =
        NotatService(
            notatRepository,
            pdfGeneratorKlient,
            dokarkivService,
            grunnlagService,
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
    }

    @Test
    fun `Generer PDF for klageblankett (forhåndsvisning)`() {
        val sakId = Random.nextLong()
        val klage = klageForInnstilling(sakId)

        coEvery {
            grunnlagService.hentGrunnlagForSak(
                sakId,
                any(),
            )
        } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { pdfGeneratorKlient.genererPdf(any(), any(), any()) } returns "pdf".toByteArray()

        runBlocking {
            notatService.genererPdf(StrukturertNotat.KlageBlankett(klage), simpleSaksbehandler())
        }

        coVerify(exactly = 1) { grunnlagService.hentGrunnlagForSak(sakId, any()) }
        coVerify(exactly = 1) { pdfGeneratorKlient.genererPdf(any(), any(), NotatMal.KLAGE_OVERSENDELSE_BLANKETT) }
    }

    @Test
    fun `journalfoerNotatISak oppretter brev og innhold, og journalfører mot dokarkiv`() {
        val sakId = Random.nextLong()
        val klage = klageForInnstilling(sakId)

        coEvery {
            grunnlagService.hentGrunnlagForSak(
                sakId,
                any(),
            )
        } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { pdfGeneratorKlient.genererPdf(any(), any(), any()) } returns "pdf".toByteArray()
        coEvery { dokarkivService.journalfoer(any()) } returns OpprettJournalpostResponse("123", true)

        runBlocking {
            notatService.journalfoerNotatISak(
                StrukturertNotat.KlageBlankett(klage),
                simpleSaksbehandler(),
            )
        }

        coVerify(exactly = 1) {
            grunnlagService.hentGrunnlagForSak(sakId, any())
            pdfGeneratorKlient.genererPdf(any(), any(), NotatMal.KLAGE_OVERSENDELSE_BLANKETT)
            dokarkivService.journalfoer(any())

            notatRepository.hentForReferanse(klage.id.toString())
            notatRepository.opprett(any(), any())
            notatRepository.hent(any())

            notatRepository.settJournalfoert(any(), any(), any())
        }
    }

    @Test
    fun `hvis journalføring feiler settes brevet til slettet`() {
        val sakId = Random.nextLong()
        val klage = klageForInnstilling(sakId)

        coEvery {
            grunnlagService.hentGrunnlagForSak(
                sakId,
                any(),
            )
        } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { pdfGeneratorKlient.genererPdf(any(), any(), any()) } returns "pdf".toByteArray()
        coEvery { dokarkivService.journalfoer(any()) } throws Exception()

        assertThrows<Exception> {
            runBlocking {
                notatService.journalfoerNotatISak(StrukturertNotat.KlageBlankett(klage), simpleSaksbehandler())
            }
        }

        notatRepository.hentForReferanse(klage.id.toString())

        coVerify(exactly = 1) {
            grunnlagService.hentGrunnlagForSak(sakId, any())
            pdfGeneratorKlient.genererPdf(any(), any(), NotatMal.KLAGE_OVERSENDELSE_BLANKETT)
            dokarkivService.journalfoer(any())
            notatRepository.slett(any())
        }

        coVerify(exactly = 0) {
            notatRepository.settJournalfoert(any(), any(), any())
        }
    }
}

private fun klageForInnstilling(sakId: SakId): Klage =
    Klage
        .ny(
            sak =
                Sak(
                    ident = SOEKER_FOEDSELSNUMMER.value,
                    sakType = SakType.BARNEPENSJON,
                    id = sakId,
                    enhet = Enheter.PORSGRUNN.enhetNr,
                ),
            innkommendeDokument = null,
        ).copy(
            utfall =
                KlageUtfallMedData.StadfesteVedtak(
                    innstilling =
                        InnstillingTilKabal(
                            lovhjemmel = KabalHjemmel.FTRL_18_4,
                            internKommentar = null,
                            brev = KlageOversendelsebrev(brevId = 123L),
                            innstillingTekst = "Hello",
                        ),
                    saksbehandler =
                        Grunnlagsopplysning.Saksbehandler(
                            ident = "",
                            tidspunkt = Tidspunkt.now(),
                        ),
                ),
            formkrav =
                FormkravMedBeslutter(
                    formkrav =
                        Formkrav(
                            vedtaketKlagenGjelder =
                                VedtaketKlagenGjelder(
                                    id = "",
                                    behandlingId = "",
                                    datoAttestert = LocalDate.now().atStartOfDay(ZoneId.systemDefault()),
                                    vedtakType = VedtakType.INNVILGELSE,
                                ),
                            erKlagerPartISaken = JaNei.JA,
                            erKlagenSignert = JaNei.JA,
                            gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
                            erKlagenFramsattInnenFrist = JaNei.JA,
                            erFormkraveneOppfylt = JaNei.JA,
                            begrunnelse = null,
                        ),
                    saksbehandler = Grunnlagsopplysning.automatiskSaksbehandler,
                ),
        )
