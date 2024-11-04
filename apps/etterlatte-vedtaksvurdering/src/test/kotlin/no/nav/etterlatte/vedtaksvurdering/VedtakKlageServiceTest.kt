package no.nav.etterlatte.vedtaksvurdering

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.libs.common.behandling.GrunnForOmgjoering
import no.nav.etterlatte.libs.common.behandling.InnstillingTilKabal
import no.nav.etterlatte.libs.common.behandling.KabalHjemmel
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageOmgjoering
import no.nav.etterlatte.libs.common.behandling.KlageOversendelsebrev
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.KlageVedtak
import no.nav.etterlatte.libs.common.behandling.KlageVedtaksbrev
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.dbutils.Tidspunkt
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.VedtakKlageService
import no.nav.etterlatte.vedtaksvurdering.database.DatabaseExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(DatabaseExtension::class)
internal class VedtakKlageServiceTest(
    dataSource: DataSource,
) {
    private val vedtaksvurderingRepository = VedtaksvurderingRepository(dataSource)
    private val vedtaksvurderingRapidService = mockk<VedtaksvurderingRapidService>()
    private val vedtakKlageService = VedtakKlageService(vedtaksvurderingRepository, vedtaksvurderingRapidService)

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        every { vedtaksvurderingRapidService.sendToRapid(any()) } returns Unit
    }

    @Test
    fun `opprettEllerOppdaterVedtakOmAvvisning skal opprette vedtak når det ikke finnes fra før`() {
        val klage = klage()
        val vedtakId = vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage).id

        val vedtak = vedtaksvurderingRepository.hentVedtak(vedtakId) ?: throw RuntimeException("Vedtak ikke funnet")

        vedtak.soeker.value shouldBe klage.sak.ident
        vedtak.behandlingId shouldBe klage.id
        vedtak.sakId shouldBe klage.sak.id
        vedtak.sakType shouldBe klage.sak.sakType
        vedtak.type shouldBe VedtakType.AVVIST_KLAGE
        vedtakInnholdToKlage(vedtak) shouldBeEqual klage
    }

    @Test
    fun `opprettEllerOppdaterVedtakOmAvvisning skal oppdatere vedtak når det finnes fra før`() {
        val klage = klage(utfall = utfallOmgjoering())

        val vedtakId =
            vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage).id

        val vedtak = vedtaksvurderingRepository.hentVedtak(vedtakId) ?: throw RuntimeException("Vedtak ikke funnet")

        vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(
            klage.copy(utfall = utfallStadfesteVedtak()),
        )

        val oppdatert = vedtaksvurderingRepository.hentVedtak(vedtakId) ?: throw RuntimeException("Vedtak ikke funnet")

        oppdatert.shouldBeEqualToIgnoringFields(vedtak, Vedtak::innhold)
    }

    @Test
    fun `fattVedtak skal oppdatere og fatte vedtak når det finnes fra før`() {
        val klage = klage()
        val klageMedUtfall = klage.copy(utfall = utfallAvvist())

        vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage)

        val vedtakId = vedtakKlageService.fattVedtak(klageMedUtfall, saksbehandler).id

        with(vedtaksvurderingRepository.hentVedtak(vedtakId)!!) {
            vedtakFattet!!.ansvarligSaksbehandler shouldBe saksbehandler.ident
            vedtakFattet!!.ansvarligEnhet shouldBe klage.sak.enhet
            vedtakFattet!!.tidspunkt shouldNotBe null
            vedtakInnholdToKlage(this) shouldBeEqual klageMedUtfall
        }
        verify {
            vedtaksvurderingRapidService.sendToRapid(
                withArg {
                    it.rapidInfo1.vedtakhendelse shouldBe VedtakKafkaHendelseHendelseType.FATTET
                },
            )
        }
    }

    @Test
    fun `attesterVedtak skal attestere vedtak og returnere vedtaket`() {
        val klage = klage()
        vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage)
        val vedtakId = vedtakKlageService.fattVedtak(klage, saksbehandler).id
        vedtakKlageService.attesterVedtak(klage, attestant)

        with(vedtaksvurderingRepository.hentVedtak(vedtakId)!!) {
            id shouldBe vedtakId
            attestasjon!!.attestant shouldBe attestant.ident
            attestasjon!!.tidspunkt shouldNotBe null
            attestasjon!!.attesterendeEnhet shouldBe klage.sak.enhet
        }
        verify(exactly = 1) {
            vedtaksvurderingRapidService.sendToRapid(
                withArg {
                    it.rapidInfo1.vedtakhendelse shouldBe VedtakKafkaHendelseHendelseType.FATTET
                    it.rapidInfo2 shouldBe null
                },
            )
            vedtaksvurderingRapidService.sendToRapid(
                withArg {
                    it.rapidInfo1.vedtakhendelse shouldBe VedtakKafkaHendelseHendelseType.ATTESTERT
                    it.rapidInfo2 shouldBe null
                },
            )
        }
    }

    @Test
    fun `attesterVedtak skal ikke attestere hvis feil status`() {
        val klage = klage()
        vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage)
        vedtakKlageService.fattVedtak(klage, saksbehandler)
        vedtaksvurderingRepository.iverksattVedtak(klage.id)

        assertThrows<VedtakTilstandException> {
            vedtakKlageService.attesterVedtak(klage, attestant)
        }
        val slot = slot<VedtakOgRapid>()
        verify(exactly = 1) { vedtaksvurderingRapidService.sendToRapid(capture(slot)) }
        slot.captured.rapidInfo1.vedtakhendelse shouldBe VedtakKafkaHendelseHendelseType.FATTET
        slot.captured.rapidInfo2 shouldBe null
    }

    @Test
    fun `attesterVedtak skal ikke attestere hvis attestant ogsaa er saksbehandler`() {
        val klage = klage()
        vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage)
        vedtakKlageService.fattVedtak(klage, saksbehandler)
        assertThrows<UgyldigAttestantException> {
            vedtakKlageService.attesterVedtak(klage, saksbehandler)
        }
        val slot = slot<VedtakOgRapid>()
        verify(exactly = 1) { vedtaksvurderingRapidService.sendToRapid(capture(slot)) }
        slot.captured.rapidInfo1.vedtakhendelse shouldBe VedtakKafkaHendelseHendelseType.FATTET
        slot.captured.rapidInfo2 shouldBe null
    }

    @Test
    fun `underkjennVedtak skal underkjenne vedtak`() {
        val klage = klage()
        val vedtak: Vedtak = vedtakKlageService.opprettEllerOppdaterVedtakOmAvvisning(klage)
        vedtakKlageService.fattVedtak(klage, saksbehandler)

        vedtakKlageService.underkjennVedtak(klage.id).id shouldBe vedtak.id

        verify(exactly = 1) {
            vedtaksvurderingRapidService.sendToRapid(
                withArg {
                    it.rapidInfo1.vedtakhendelse shouldBe VedtakKafkaHendelseHendelseType.FATTET
                    it.rapidInfo2 shouldBe null
                },
            )
            vedtaksvurderingRapidService.sendToRapid(
                withArg {
                    it.rapidInfo1.vedtakhendelse shouldBe VedtakKafkaHendelseHendelseType.UNDERKJENT
                    it.rapidInfo2 shouldBe null
                },
            )
        }
    }

    @Test
    fun `underkjennVedtak skal ikke underkjenne hvis feil status`() {
        val klageId = UUID.randomUUID()
        vedtaksvurderingRepository.opprettVedtak(opprettVedtak(behandlingId = klageId))

        assertThrows<VedtakTilstandException> {
            vedtakKlageService.underkjennVedtak(klageId)
        }
    }

    private fun utfallStadfesteVedtak() =
        KlageUtfallMedData.StadfesteVedtak(
            InnstillingTilKabal(
                lovhjemmel = KabalHjemmel.FTRL_18_4,
                internKommentar = null,
                brev = KlageOversendelsebrev(brevId = 123L),
                innstillingTekst = "Hello",
            ),
            Grunnlagsopplysning.Saksbehandler(
                ident = "TORE",
                tidspunkt = Tidspunkt.now(),
            ),
        )

    private fun utfallOmgjoering() =
        KlageUtfallMedData.Omgjoering(
            KlageOmgjoering(GrunnForOmgjoering.ANNET, "Hello"),
            Grunnlagsopplysning.Saksbehandler(
                ident = "TORE",
                tidspunkt = Tidspunkt.now(),
            ),
        )

    private fun utfallAvvist() =
        KlageUtfallMedData.Avvist(
            saksbehandler =
                Grunnlagsopplysning.Saksbehandler(
                    ident = "TORE",
                    tidspunkt = Tidspunkt.now(),
                ),
            vedtak = KlageVedtak(1L),
            brev = KlageVedtaksbrev(brevId = 1L),
        )

    private fun klage(utfall: KlageUtfallMedData? = null): Klage =
        Klage(
            UUID.randomUUID(),
            Sak(FNR_1, SakType.BARNEPENSJON, sakId1, ENHET_1),
            Tidspunkt.now(),
            KlageStatus.OPPRETTET,
            kabalResultat = null,
            kabalStatus = null,
            formkrav = null,
            innkommendeDokument = null,
            resultat = null,
            utfall = utfall,
            aarsakTilAvbrytelse = null,
            initieltUtfall = null,
        )

    private fun vedtakInnholdToKlage(vedtak: Vedtak) = deserialize<Klage>((vedtak.innhold as VedtakInnhold.Klage).klage.toJson())
}
