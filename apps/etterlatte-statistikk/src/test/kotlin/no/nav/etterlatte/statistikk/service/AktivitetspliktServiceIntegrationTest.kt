package no.nav.etterlatte.statistikk.service

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetType
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktAktivitetsgradDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.AktivitetspliktDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetDto
import no.nav.etterlatte.libs.common.aktivitetsplikt.UnntakFraAktivitetsplikt
import no.nav.etterlatte.libs.common.aktivitetsplikt.VurdertAktivitetsgrad
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.statistikk.StatistikkDatabaseExtension
import no.nav.etterlatte.statistikk.database.AktivitetsgradPeriode
import no.nav.etterlatte.statistikk.database.AktivitetspliktRepo
import no.nav.etterlatte.statistikk.database.VurdertAktivitet
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(StatistikkDatabaseExtension::class)
class AktivitetspliktServiceIntegrationTest(
    dataSource: DataSource,
) {
    private val repo = AktivitetspliktRepo(dataSource)
    private val aktivitetspliktService = AktivitetspliktService(repo)

    @Test
    fun `oppdaterVurderingAktivitetsplikt lagrer ned ny versjon av aktivitetsplikt`() {
        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(aktivitetspliktDto())

        val lagretAktivitet = aktivitetspliktService.hentAktivitet(1L, YearMonth.now())
        lagretAktivitet?.sakId shouldBe 1L
        lagretAktivitet?.harVarigUnntak shouldBe false
        lagretAktivitet?.aktivitetsgrad shouldBe emptyList()

        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(
            aktivitetspliktDto(
                aktivitetsgrad =
                    listOf(
                        AktivitetspliktAktivitetsgradDto(
                            vurdering = VurdertAktivitetsgrad.AKTIVITET_100,
                            fom = null,
                            tom = null,
                        ),
                    ),
            ),
        )

        val oppdatertLagretAktivitet = aktivitetspliktService.hentAktivitet(1L, YearMonth.now())
        oppdatertLagretAktivitet?.aktivitetsgrad shouldBe
            listOf(
                AktivitetsgradPeriode(
                    vurdering = VurdertAktivitet.HUNDRE_PROSENT,
                    fom = null,
                    tom = null,
                ),
            )
    }

    @Test
    fun `hentAktivitet henter siste registrert innenfor maaned registret`() {
        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(
            aktivitetspliktDto(
                avdoedDoedsmaaned = YearMonth.of(2024, Month.JANUARY),
            ),
            Tidspunkt.ofNorskTidssone(
                LocalDate.now().minusMonths(1L),
                LocalTime.MIN,
            ),
        )
        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(
            aktivitetspliktDto(
                avdoedDoedsmaaned = YearMonth.of(2024, Month.FEBRUARY),
            ),
        )

        aktivitetspliktService.hentAktivitet(1L, YearMonth.now())?.avdoedDoedsmaaned shouldBe
            YearMonth.of(
                2024,
                Month.FEBRUARY,
            )
        aktivitetspliktService
            .hentAktivitet(
                1L,
                YearMonth.now().minusMonths(1),
            )?.avdoedDoedsmaaned shouldBe YearMonth.of(2024, Month.JANUARY)
        aktivitetspliktService.hentAktivitet(1L, YearMonth.now().minusMonths(2)) shouldBe null
    }

    @Test
    fun `mapAktivitetForSaker returnerer mappet statistikk for riktig måned for angitte saker`() {
        val sakMedVarigUnntakId = 1L
        val sakMedIkkeOppfyltAktivitet = 2L
        val sakMedOppfyltAktivitet = 3L
        val sakMedMidlertidigUnntakId = 4L
        val sakMedIngenRegistrertAktivitet = 5L

        val alleSakene =
            listOf(
                sakMedOppfyltAktivitet,
                sakMedIngenRegistrertAktivitet,
                sakMedVarigUnntakId,
                sakMedIkkeOppfyltAktivitet,
                sakMedMidlertidigUnntakId,
            )

        val tidspunktRegistrertJuni = Tidspunkt.ofNorskTidssone(YearMonth.of(2024, Month.JUNE).atDay(1), LocalTime.MIN)

        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(
            aktivitetspliktDto(
                sakId = sakMedVarigUnntakId,
                avdoedDoedsmaaned = YearMonth.of(2023, Month.DECEMBER),
                unntak =
                    listOf(
                        UnntakFraAktivitetDto(
                            unntak = UnntakFraAktivitetsplikt.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT,
                            fom = null,
                            tom = null,
                        ),
                    ),
            ),
            tidspunktRegistrertJuni,
        )

        // Oppdatering måned etter vi har statistikk
        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(
            aktivitetspliktDto(
                sakId = sakMedVarigUnntakId,
                avdoedDoedsmaaned = YearMonth.of(2023, Month.DECEMBER),
            ),
            Tidspunkt.ofNorskTidssone(YearMonth.of(2024, Month.AUGUST).atDay(1), LocalTime.MIN),
        )

        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(
            aktivitetspliktDto(
                sakId = sakMedIkkeOppfyltAktivitet,
                avdoedDoedsmaaned = YearMonth.of(2023, Month.DECEMBER),
                aktivitetsgrad =
                    listOf(
                        AktivitetspliktAktivitetsgradDto(
                            vurdering = VurdertAktivitetsgrad.AKTIVITET_UNDER_50,
                            fom = null,
                            tom = null,
                        ),
                    ),
            ),
            tidspunktRegistrertJuni,
        )

        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(
            aktivitetspliktDto(
                sakId = sakMedOppfyltAktivitet,
                avdoedDoedsmaaned = YearMonth.of(2023, Month.DECEMBER),
                aktivitetsgrad =
                    listOf(
                        AktivitetspliktAktivitetsgradDto(
                            vurdering = VurdertAktivitetsgrad.AKTIVITET_100,
                            fom = null,
                            tom = null,
                        ),
                    ),
                brukersAktivitet =
                    listOf(
                        AktivitetDto(
                            typeAktivitet = AktivitetType.ARBEIDSSOEKER,
                            fom = YearMonth.of(2024, Month.JULY).atDay(1),
                            tom = null,
                        ),
                    ),
            ),
            tidspunktRegistrertJuni,
        )
        aktivitetspliktService.oppdaterVurderingAktivitetsplikt(
            aktivitetspliktDto(
                sakId = sakMedMidlertidigUnntakId,
                avdoedDoedsmaaned = YearMonth.of(2023, Month.DECEMBER),
                unntak =
                    listOf(
                        UnntakFraAktivitetDto(
                            unntak = UnntakFraAktivitetsplikt.SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE,
                            fom = null,
                            tom = null,
                        ),
                    ),
            ),
            tidspunktRegistrertJuni,
        )

        val mappetStatistikkJuli =
            aktivitetspliktService.mapAktivitetForSaker(
                alleSakene,
                YearMonth.of(2024, Month.JULY),
            )

        with(mappetStatistikkJuli) {
            get(sakMedVarigUnntakId)?.shouldBeEqual(
                AktivitetForMaaned(
                    Aktivitetsplikt.VARIG_UNNTAK,
                    null,
                    null,
                ),
            )
            get(sakMedOppfyltAktivitet)?.shouldBeEqual(
                AktivitetForMaaned(
                    Aktivitetsplikt.JA,
                    true,
                    "HUNDRE_PROSENT_ARBEIDSSOEKER",
                ),
            )
            get(sakMedIkkeOppfyltAktivitet)?.shouldBeEqual(
                AktivitetForMaaned(
                    Aktivitetsplikt.JA,
                    false,
                    "UNDER_50_PROSENT",
                ),
            )
            get(sakMedMidlertidigUnntakId)?.shouldBeEqual(
                AktivitetForMaaned(
                    Aktivitetsplikt.JA,
                    true,
                    "UNNTAK_SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE",
                ),
            )
            get(sakMedIngenRegistrertAktivitet) shouldBe null
        }

        val mappetStatistikkJuni =
            aktivitetspliktService.mapAktivitetForSaker(
                alleSakene,
                YearMonth.of(2024, Month.JUNE),
            )
        with(mappetStatistikkJuni) {
            get(sakMedVarigUnntakId)?.shouldBeEqual(
                AktivitetForMaaned.FALLBACK_OMSTILLINGSSTOENAD,
            )
            get(sakMedIkkeOppfyltAktivitet)?.shouldBeEqual(
                AktivitetForMaaned.FALLBACK_OMSTILLINGSSTOENAD,
            )
            get(sakMedMidlertidigUnntakId)?.shouldBeEqual(
                AktivitetForMaaned.FALLBACK_OMSTILLINGSSTOENAD,
            )
            get(sakMedOppfyltAktivitet)?.shouldBeEqual(
                AktivitetForMaaned.FALLBACK_OMSTILLINGSSTOENAD,
            )
            get(sakMedIngenRegistrertAktivitet) shouldBe null
        }
    }
}

fun aktivitetspliktDto(
    sakId: Long = 1L,
    avdoedDoedsmaaned: YearMonth = YearMonth.now().minusMonths(6),
    aktivitetsgrad: List<AktivitetspliktAktivitetsgradDto> = emptyList(),
    unntak: List<UnntakFraAktivitetDto> = emptyList(),
    brukersAktivitet: List<AktivitetDto> = emptyList(),
): AktivitetspliktDto =
    AktivitetspliktDto(
        sakId = sakId,
        avdoedDoedsmaaned = avdoedDoedsmaaned,
        aktivitetsgrad = aktivitetsgrad,
        unntak = unntak,
        brukersAktivitet = brukersAktivitet,
    )
