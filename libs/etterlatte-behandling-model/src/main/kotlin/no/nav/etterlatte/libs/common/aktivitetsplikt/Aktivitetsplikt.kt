package no.nav.etterlatte.libs.common.aktivitetsplikt

import java.time.LocalDate
import java.time.YearMonth

data class AktivitetspliktDto(
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    // TODO: Denne har vi et forhold til når vi lager oppgaver / revurderinger, men vi trenger også å vite om den
    //   for statistikk sin del. Det er nok hensiktsmessig å løfte denne mer eksplisitt fram, men er litt usikker på
    //   hvordan vi bør løse det på lang sikt. Enn så lenge kan den utledes fra grunnlag, men det er litt sårbart
    val avdoedDoedsmaaned: YearMonth,
    val aktivitetsgrad: List<AktivitetspliktAktivitetsgradDto>,
    val unntak: List<UnntakFraAktivitetDto>,
    val brukersAktivitet: List<AktivitetDto>,
)

data class AktivitetspliktAktivitetsgradDto(
    val vurdering: VurdertAktivitetsgrad,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

enum class VurdertAktivitetsgrad {
    AKTIVITET_UNDER_50,
    AKTIVITET_OVER_50,
    AKTIVITET_100,
}

data class UnntakFraAktivitetDto(
    val unntak: UnntakFraAktivitetsplikt,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

enum class UnntakFraAktivitetsplikt {
    OMSORG_BARN_UNDER_ETT_AAR,
    OMSORG_BARN_SYKDOM,
    MANGLENDE_TILSYNSORDNING_SYKDOM,
    SYKDOM_ELLER_REDUSERT_ARBEIDSEVNE,
    GRADERT_UFOERETRYGD,
    MIDLERTIDIG_SYKDOM,
    FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT,
}

data class AktivitetDto(
    val typeAktivitet: AktivitetType,
    val fom: LocalDate,
    val tom: LocalDate?,
)

enum class AktivitetType {
    ARBEIDSTAKER,
    SELVSTENDIG_NAERINGSDRIVENDE,
    ETABLERER_VIRKSOMHET,
    ARBEIDSSOEKER,
    UTDANNING,
}
