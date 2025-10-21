package no.nav.etterlatte.statistikk.database

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.equals.shouldBeEqual
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerForbehandlingStatus
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerHendelseType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.InntektSummert
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.Inntektsmaaned
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.PensjonsgivendeInntekt
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkattStatistikkDto
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.SummerteInntekterAOrdningenStatistikkDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EtteroppgjoerRepositoryTest(
    private val dataSource: DataSource,
) {
    @Test
    fun `kan lagre og hente ut etteroppgjoer-rad`() {
        val rad =
            EtteroppgjoerRad(
                id = -1,
                forbehandlingId = UUID.randomUUID(),
                sakId = SakId(1L),
                aar = 2024,
                hendelse = EtteroppgjoerHendelseType.OPPRETTET,
                forbehandlingStatus = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                opprettet = Tidspunkt.now(),
                maanederYtelse = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
                tekniskTid = Tidspunkt.now(),
                utbetaltStoenad = 200000,
                nyBruttoStoenad = 200000,
                differanse = 0,
                rettsgebyr = 1234,
                rettsgebyrGyldigFra = LocalDate.of(2024, 1, 1),
                tilbakekrevingGrense = 40.0,
                etterbetalingGrense = 20.0,
                resultatType = EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING,
                summerteInntekter =
                    SummerteInntekterAOrdningenStatistikkDto(
                        afp = InntektSummert("Filter", listOf(Inntektsmaaned(YearMonth.now(), BigDecimal(123)))),
                        loenn = InntektSummert("Filter", listOf(Inntektsmaaned(YearMonth.now(), BigDecimal(123)))),
                        oms = InntektSummert("Filter", listOf(Inntektsmaaned(YearMonth.now(), BigDecimal(123)))),
                        tidspunktBeregnet = Tidspunkt.now(),
                    ),
                pensjonsgivendeInntekt =
                    PensjonsgivendeInntektFraSkattStatistikkDto(
                        inntektsaar = 2024,
                        inntekter =
                            listOf(
                                PensjonsgivendeInntekt(
                                    inntektsaar = 2024,
                                    skatteordning = "skatteordning",
                                    loensinntekt = 1,
                                    naeringsinntekt = 1,
                                    fiskeFangstFamiliebarnehage = 1,
                                ),
                            ),
                    ),
                tilknyttetRevurdering = true
            )
        val repo = EtteroppgjoerRepository(dataSource)
        repo.lagreEtteroppgjoerRad(rad)
        val hentetRad = repo.hentEtteroppgjoerRaderForForbehandling(rad.forbehandlingId).single()
        hentetRad.shouldBeEqualToIgnoringFields(rad, EtteroppgjoerRad::id)
        val hentetRadMedId = repo.hentEtteroppgjoerRad(hentetRad.id)
        hentetRad shouldBeEqual hentetRadMedId
    }

    @Test
    fun `kan lagre og hente ut etteroppgjoer-rad uten resultater lagret riktig`() {
        val rad =
            EtteroppgjoerRad(
                id = -1,
                forbehandlingId = UUID.randomUUID(),
                sakId = SakId(1L),
                aar = 2024,
                hendelse = EtteroppgjoerHendelseType.OPPRETTET,
                forbehandlingStatus = EtteroppgjoerForbehandlingStatus.OPPRETTET,
                opprettet = Tidspunkt.now(),
                maanederYtelse = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
                tekniskTid = Tidspunkt.now(),
                utbetaltStoenad = null,
                nyBruttoStoenad = null,
                differanse = null,
                rettsgebyr = null,
                rettsgebyrGyldigFra = null,
                tilbakekrevingGrense = null,
                etterbetalingGrense = null,
                resultatType = null,
                tilknyttetRevurdering = null
            )
        val repo = EtteroppgjoerRepository(dataSource)
        repo.lagreEtteroppgjoerRad(rad)
        val hentetRad = repo.hentEtteroppgjoerRaderForForbehandling(rad.forbehandlingId).single()
        hentetRad.shouldBeEqualToIgnoringFields(rad, EtteroppgjoerRad::id)
        val hentetRadMedId = repo.hentEtteroppgjoerRad(hentetRad.id)
        hentetRad shouldBeEqual hentetRadMedId
    }

    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }
}
