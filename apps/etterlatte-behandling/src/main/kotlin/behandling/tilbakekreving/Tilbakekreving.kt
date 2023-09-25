package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.KlasseType
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.KravgrunnlagPeriode
import java.time.YearMonth
import java.util.UUID

data class Tilbakekreving(
    val id: UUID,
    val status: TilbakekrevingStatus,
    val sak: Sak,
    val opprettet: Tidspunkt,
    val utbetalinger: List<Utbetaling>,
    val kravgrunnlag: Kravgrunnlag,
) {
    companion object {
        fun ny(
            kravgrunnlag: Kravgrunnlag,
            sak: Sak,
        ) = Tilbakekreving(
            id = UUID.randomUUID(),
            status = TilbakekrevingStatus.OPPRETTET,
            sak = sak,
            opprettet = Tidspunkt.now(),
            utbetalinger = kravgrunnlag.perioder.tilUtbetalinger(sak.sakType),
            kravgrunnlag = kravgrunnlag,
        )
    }
}

data class Utbetaling(
    val maaned: YearMonth,
    val type: String,
    val bruttoUtbetaling: Int,
    val nyBruttoUtbetaling: Int,
    val skatteprosent: Double,
    val beregnetFeilutbetaling: Int?,
    val bruttoTilbakekreving: Int?,
    val nettoTilbakekreving: Int?,
    val skatt: Int?,
    val skyld: String?,
    val resultat: String?,
    val tilbakekrevingsprosent: Int?,
    val rentetillegg: Int?,
)

fun List<KravgrunnlagPeriode>.tilUtbetalinger(sakType: SakType): List<Utbetaling> {
    return flatMap { periode ->
        periode.grunnlagsbeloep.map { grunnlagsbeloep ->
            Utbetaling(
                maaned = periode.periode.fraOgMed,
                type =
                    when (grunnlagsbeloep.type) {
                        KlasseType.FEIL -> "Feilkonto"
                        else -> sakType.name.lowercase().replaceFirstChar(Char::titlecase)
                    },
                bruttoUtbetaling = grunnlagsbeloep.bruttoUtbetaling.toInt(),
                nyBruttoUtbetaling = grunnlagsbeloep.nyBruttoUtbetaling.toInt(),
                skatteprosent = grunnlagsbeloep.skatteProsent.toDouble(),
                beregnetFeilutbetaling = null,
                bruttoTilbakekreving = null,
                nettoTilbakekreving = null,
                skatt = null,
                skyld = grunnlagsbeloep.skyld,
                resultat = grunnlagsbeloep.resultat,
                tilbakekrevingsprosent = null,
                rentetillegg = null,
            )
        }
    }
}

enum class TilbakekrevingStatus {
    OPPRETTET,
    UNDER_ARBEID,
}

class KravgrunnlagHarIkkeEksisterendeSak() : RuntimeException()
