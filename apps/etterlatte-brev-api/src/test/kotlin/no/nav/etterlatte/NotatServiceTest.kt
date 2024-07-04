package no.nav.etterlatte

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.NotatService
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.dokarkiv.DokarkivKlient
import no.nav.etterlatte.brev.dokarkiv.OpprettJournalpostResponse
import no.nav.etterlatte.brev.hentinformasjon.GrunnlagService
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.notat.StrukturertBrev
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
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.Telefonnummer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.ZoneId
import javax.sql.DataSource

@ExtendWith(GenerellDatabaseExtension::class)
class NotatServiceTest(
    dataSource: DataSource,
) {
    private val brevRepository: BrevRepository = BrevRepository(dataSource)
    private val adresseService: AdresseService = mockk()
    private val brevbakerService: BrevbakerService = mockk()
    private val grunnlagService: GrunnlagService = mockk()
    private val dokarkivKlient: DokarkivKlient = mockk()

    private val notatService =
        NotatService(
            brevRepository = brevRepository,
            adresseService = adresseService,
            brevbakerService = brevbakerService,
            grunnlagService = grunnlagService,
            dokarkivKlient = dokarkivKlient,
        )

    @Test
    fun `journalfoerNotatISak oppretter brev og innhold, og journalfører mot dokarkiv`() {
        val sakId = 1L
        val klage = klageForInnstilling(sakId)

        coEvery {
            adresseService.hentAvsender(any())
        } returns
            Avsender(
                kontor = "Nav",
                telefonnummer = Telefonnummer(""),
                saksbehandler = "Saks Behandler",
                attestant = null,
            )

        val dummyPdf = Pdf("Hello world".toByteArray())
        coEvery { grunnlagService.hentGrunnlagForSak(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { brevbakerService.genererPdf(any(), any()) } returns dummyPdf
        coEvery { dokarkivKlient.opprettJournalpost(any(), any()) } returns
            OpprettJournalpostResponse(
                "123",
                true,
            )

        runBlocking {
            notatService.journalfoerNotatISak(
                sakId = sakId,
                notatData =
                    StrukturertBrev.KlageBlankett(
                        klage = klage,
                    ),
                bruker = Saksbehandler("", "Z999999", null),
            )
        }

        val notatForKlage = brevRepository.hentBrevForBehandling(klage.id, Brevtype.NOTAT)

        Assertions.assertEquals(1, notatForKlage.size)

        val oversendelseNotat = notatForKlage.first()
        Assertions.assertEquals(Status.JOURNALFOERT, oversendelseNotat.status)
        val pdfinnhold = brevRepository.hentPdf(oversendelseNotat.id)
        Assertions.assertArrayEquals(dummyPdf.bytes, pdfinnhold?.bytes)
    }

    @Test
    fun `hvis journalføring feiler settes brevet til slettet`() {
        val sakId = 3L
        val klage = klageForInnstilling(sakId)

        coEvery {
            adresseService.hentAvsender(any())
        } returns
            Avsender(
                kontor = "Nav",
                telefonnummer = Telefonnummer(""),
                saksbehandler = "Saks Behandler",
                attestant = null,
            )

        val dummyPdf = Pdf("Hello world".toByteArray())
        coEvery { grunnlagService.hentGrunnlagForSak(any(), any()) } returns GrunnlagTestData().hentOpplysningsgrunnlag()
        coEvery { brevbakerService.genererPdf(any(), any()) } returns dummyPdf
        coEvery { dokarkivKlient.opprettJournalpost(any(), any()) } throws Exception("Å nei")

        try {
            runBlocking {
                notatService.journalfoerNotatISak(
                    sakId = sakId,
                    notatData =
                        StrukturertBrev.KlageBlankett(
                            klage = klage,
                        ),
                    bruker = Saksbehandler("", "Z999999", null),
                )
            }
        } catch (_: Exception) {
        }

        val notaterForKlage = brevRepository.hentBrevForBehandling(klage.id, Brevtype.NOTAT)
        Assertions.assertTrue(notaterForKlage.isEmpty())
    }
}

private fun klageForInnstilling(sakId: Long): Klage =
    Klage
        .ny(
            sak =
                Sak(
                    ident = SOEKER_FOEDSELSNUMMER.value,
                    sakType = SakType.BARNEPENSJON,
                    id = sakId,
                    enhet = "4808",
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
