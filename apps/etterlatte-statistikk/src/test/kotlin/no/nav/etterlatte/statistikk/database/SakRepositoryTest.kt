package no.nav.etterlatte.statistikk.database

import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.etterlatte.behandling.tilSakId
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.statistikk.domain.AvkortetYtelse
import no.nav.etterlatte.statistikk.domain.Avkorting
import no.nav.etterlatte.statistikk.domain.AvkortingGrunnlag
import no.nav.etterlatte.statistikk.domain.BehandlingMetode
import no.nav.etterlatte.statistikk.domain.Beregning
import no.nav.etterlatte.statistikk.domain.Beregningstype
import no.nav.etterlatte.statistikk.domain.SakRad
import no.nav.etterlatte.statistikk.domain.SakUtland
import no.nav.etterlatte.statistikk.domain.SakYtelsesgruppe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SakRepositoryTest(
    private val dataSource: DataSource,
) {
    val mockBeregning =
        Beregning(
            beregningId = UUID.randomUUID(),
            behandlingId = UUID.randomUUID(),
            type = Beregningstype.BP,
            beregnetDato = Tidspunkt.now(),
            beregningsperioder = listOf(),
        )

    val mockAvkorting =
        Avkorting(
            listOf(
                AvkortingGrunnlag(
                    fom = YearMonth.now(),
                    tom = null,
                    aarsinntekt = 100,
                    fratrekkInnAar = 40,
                    relevanteMaanederInnAar = 2,
                    spesifikasjon = "",
                ),
            ),
            listOf(
                AvkortetYtelse(
                    fom = YearMonth.now(),
                    tom = null,
                    ytelseFoerAvkorting = 200,
                    avkortingsbeloep = 50,
                    ytelseEtterAvkorting = 150,
                    restanse = 0,
                    sanksjonertYtelse = null,
                ),
            ),
        )

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    @Test
    fun testSakRepo() {
        val repo = SakRepository.using(dataSource)
        val relatertId = UUID.randomUUID().toString()

        val lagretRad =
            repo.lagreRad(
                lagSak(
                    beregning = mockBeregning,
                    avkorting = mockAvkorting,
                    relatertId = relatertId,
                ),
            )

        lagretRad shouldNotBe null
        lagretRad?.asClue { rad ->
            rad shouldBe repo.hentRader()[0]
            rad.beregning shouldBe mockBeregning
            rad.avkorting shouldBe mockAvkorting
            rad.relatertTil shouldBe relatertId
        }
    }

    @Test
    fun `sakRepository lagrer ned og henter ut null for beregning riktig`() {
        val repo = SakRepository.using(dataSource)
        val lagretRad =
            repo.lagreRad(lagSak())
        lagretRad shouldNotBe null
        lagretRad?.asClue { rad ->
            rad.beregning shouldBe null
            rad.avkorting shouldBe null
            rad.relatertTil shouldBe null
        }
        repo.hentRader().asClue { rader ->
            rader[0] shouldNotBe null
            rader[0].beregning shouldBe null
            rader[0].avkorting shouldBe null
            rader[0].relatertTil shouldBe null
        }
    }

    @Test
    fun `sakRepository henter siste rad for en sak`() {
        val repo = SakRepository.using(dataSource)
        val behandling = UUID.randomUUID()
        repo.lagreRad(lagSak(referanseId = behandling, tekniskTidspunkt = Tidspunkt.now().minus(1L, DAYS)))
        repo.lagreRad(lagSak(referanseId = behandling, tekniskTidspunkt = Tidspunkt.now().minus(2L, DAYS)))
        val nyligste = repo.lagreRad(lagSak(referanseId = behandling, tekniskTidspunkt = Tidspunkt.now().minus(3L, DAYS)))

        repo.hentSisteRad(behandling) shouldBe nyligste
    }

    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()

        fun lagSak(
            referanseId: UUID = UUID.randomUUID(),
            beregning: Beregning? = null,
            avkorting: Avkorting? = null,
            relatertId: String? = null,
            tekniskTidspunkt: Tidspunkt = Tidspunkt.now(),
        ) = SakRad(
            id = -2,
            referanseId = referanseId,
            sakId = tilSakId(1337),
            mottattTidspunkt = Tidspunkt.now(),
            registrertTidspunkt = Tidspunkt.now(),
            ferdigbehandletTidspunkt = null,
            vedtakTidspunkt = null,
            type = BehandlingType.FØRSTEGANGSBEHANDLING.name,
            status = VedtakKafkaHendelseHendelseType.IVERKSATT.name,
            resultat = null,
            resultatBegrunnelse = "for en begrunnelse",
            behandlingMetode = BehandlingMetode.MANUELL,
            opprettetAv = "test",
            ansvarligBeslutter = "test testesen",
            aktorId = "12345678911",
            datoFoersteUtbetaling = LocalDate.now(),
            tekniskTid = tekniskTidspunkt,
            sakYtelse = "En ytelse",
            vedtakLoependeFom = LocalDate.now(),
            vedtakLoependeTom = LocalDate.now().plusYears(3),
            saksbehandler = "en saksbehandler",
            ansvarligEnhet = Enheter.defaultEnhet.enhetNr,
            soeknadFormat = null,
            sakUtland = SakUtland.NASJONAL,
            beregning = beregning,
            avkorting = avkorting,
            sakYtelsesgruppe = SakYtelsesgruppe.EN_AVDOED_FORELDER,
            avdoedeForeldre = emptyList(),
            revurderingAarsak = "MIGRERING",
            kilde = Vedtaksloesning.GJENNY,
            pesysId = 123L,
            relatertTil = relatertId,
        )
    }
}
