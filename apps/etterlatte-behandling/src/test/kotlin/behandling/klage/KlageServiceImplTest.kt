package no.nav.etterlatte.behandling.klage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.BehandlingIntegrationTest
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.token.Saksbehandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KlageServiceImplTest : BehandlingIntegrationTest() {
    private lateinit var service: KlageService
    private lateinit var sakDao: SakDao
    private lateinit var oppgaveService: OppgaveService

    private val saksbehandlerIdent = "SaraSak"
    private val saksbehandler = Saksbehandler("token", saksbehandlerIdent, null)
    private val klagerFnr = GrunnlagTestData().gjenlevende.foedselsnummer.value
    private val enhet = "1337"

    companion object {
        @RegisterExtension
        private val dbExtension = DatabaseExtension()
    }

    @BeforeAll
    fun start() =
        startServer(
            featureToggleService =
                mockk<FeatureToggleService> {
                    every { isEnabled(any(), any()) } returns true
                },
        )

    @BeforeEach
    fun setUp() {
        sakDao = applicationContext.sakDao
        service = applicationContext.klageService
        oppgaveService = applicationContext.oppgaveService
    }

    @AfterEach
    fun tearDown() {
        dbExtension.resetDb()
    }

    @Test
    fun `hentKlage gir null naar klagen ikke finnes`() {
        assertNull(service.hentKlage(UUID.randomUUID()))
    }

    @Test
    fun `avbryt klage avbryter både klagen og oppgaven`() {
        val sak = oppprettOmsSak()
        val klage = opprettKlage(sak)

        service.avbrytKlage(klage.id, AarsakTilAvbrytelse.FEILREGISTRERT, "Fordi jeg vil", saksbehandler)

        val hentetKlage = requireNotNull(service.hentKlage(klage.id))
        with(hentetKlage) {
            status shouldBe KlageStatus.AVBRUTT
            aarsakTilAvbrytelse shouldBe AarsakTilAvbrytelse.FEILREGISTRERT
        }

        val hendelserISak = applicationContext.hendelseDao.hentHendelserISak(sak.id)
        hendelserISak.size shouldBe 2
        hendelserISak.first().hendelse shouldBe "KLAGE:OPPRETTET"
        hendelserISak.last().let {
            it.behandlingId shouldBe klage.id
            it.hendelse shouldBe "KLAGE:AVBRUTT"
            it.ident shouldBe saksbehandlerIdent
            it.identType shouldBe "SAKSBEHANDLER"
            it.inntruffet shouldNotBe null
            it.kommentar shouldBe "Fordi jeg vil"
            it.opprettet shouldNotBe null
            it.sakId shouldBe klage.sak.id
            it.valgtBegrunnelse shouldBe "FEILREGISTRERT"
            it.valgtBegrunnelse shouldBe AarsakTilAvbrytelse.FEILREGISTRERT.name
            it.vedtakId shouldBe null
        }
    }

    @ParameterizedTest
    @EnumSource(
        KlageStatus::class,
        names = [
            "OPPRETTET",
            "FORMKRAV_OPPFYLT",
            "FORMKRAV_IKKE_OPPFYLT",
            "UTFALL_VURDERT",
            "FATTET_VEDTAK",
            "RETURNERT",
        ],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `avbryt klage feiler hvis ulovlig status`(statusUnderTest: KlageStatus) {
        val sak = oppprettOmsSak()
        val klage = opprettKlage(sak, status = statusUnderTest)

        shouldThrow<IkkeTillattException> {
            service.avbrytKlage(
                klage.id,
                AarsakTilAvbrytelse.FEILREGISTRERT,
                "Fordi jeg vil",
                saksbehandler,
            )
        }
    }

    @Test
    fun `lagreInitieltUtfallMedBegrunnelseAvKlage gaar bra`() {
        val sak = oppprettOmsSak()
        val klage = service.opprettKlage(sak.id, InnkommendeKlage(LocalDate.now(), "", ""))
        service.lagreFormkravIKlage(
            klage.id,
            formkrav(),
            saksbehandler,
        )
        service.lagreInitieltUtfallMedBegrunnelseAvKlage(
            klageId = klage.id,
            utfall =
                InitieltUtfallMedBegrunnelseDto(
                    utfall = KlageUtfall.STADFESTE_VEDTAK,
                    begrunnelse = "Yndig rosmarin",
                ),
            saksbehandler = saksbehandler,
        )
        val oppdatert = service.hentKlage(klage.id)
        oppdatert?.initieltUtfall?.utfallMedBegrunnelse?.utfall shouldBe KlageUtfall.STADFESTE_VEDTAK
        oppdatert?.initieltUtfall?.utfallMedBegrunnelse?.begrunnelse shouldBe "Yndig rosmarin"
        oppdatert?.initieltUtfall?.tidspunkt shouldNotBe null
        oppdatert?.initieltUtfall?.saksbehandler shouldBe saksbehandler.ident
    }

    private fun formkrav() =
        Formkrav(
            vedtaketKlagenGjelder =
                VedtaketKlagenGjelder(
                    behandlingId = UUID.randomUUID().toString(),
                    datoAttestert =
                        ZonedDateTime.of(
                            LocalDate.of(2023, 10, 10),
                            LocalTime.MIDNIGHT,
                            ZoneId.systemDefault(),
                        ),
                    vedtakType = VedtakType.INNVILGELSE,
                    id = "123",
                ),
            erKlagerPartISaken = JaNei.JA,
            erKlagenSignert = JaNei.JA,
            gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
            erKlagenFramsattInnenFrist = JaNei.JA,
            erFormkraveneOppfylt = JaNei.JA,
            begrunnelse = "Jeg er enig",
        )

    private fun oppprettOmsSak() = sakDao.opprettSak(klagerFnr, SakType.OMSTILLINGSSTOENAD, enhet)

    private fun opprettKlage(
        sak: Sak,
        status: KlageStatus? = null,
    ): Klage {
        val klage = service.opprettKlage(sak.id, InnkommendeKlage(LocalDate.now(), "", ""))
        tildelOppgave(klage)
        return if (status != null) {
            klage.copy(status = status).also { applicationContext.klageDao.lagreKlage(it) }
        } else {
            klage
        }
    }

    private fun tildelOppgave(klage: Klage) {
        oppgaveService.hentOppgaverForReferanse(klage.id.toString()).single().let {
            oppgaveService.tildelSaksbehandler(it.id, saksbehandler.ident)
        }
    }
}
