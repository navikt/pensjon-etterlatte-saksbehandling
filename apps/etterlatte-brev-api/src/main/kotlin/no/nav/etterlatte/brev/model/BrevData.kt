package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

abstract class BrevData {
    inline fun <reified T : RevurderingInfo> valider(
        behandling: Behandling,
        revurderingAarsak: RevurderingAarsak,
    ): T {
        val lesbartnavn = revurderingAarsak.name.lowercase()
        if (behandling.revurderingsaarsak != revurderingAarsak) {
            throw IllegalArgumentException(
                "Kan ikke opprette et revurderingsbrev for $lesbartnavn når " +
                    "revurderingsårsak er ${behandling.revurderingsaarsak}",
            )
        }
        if (behandling.revurderingInfo !is T) {
            throw IllegalArgumentException(
                "Kan ikke opprette et revurderingsbrev for $lesbartnavn når " +
                    "revurderingsinfo ikke er $lesbartnavn",
            )
        }
        return behandling.revurderingInfo
    }
}

object AvslagBrevData : BrevData() {
    // TODO: denne skal ikke ha hele behandlingen inn
    fun fra(behandling: Behandling): AvslagBrevData = AvslagBrevData
}

abstract class EndringBrevData : BrevData()

abstract class OpphoerBrevData : BrevData()

data class ManueltBrevData(val innhold: List<Slate.Element>) : BrevData()

data class EtterbetalingDTO(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
)

data class Beregningsinfo(
    val innhold: List<Slate.Element>,
    val grunnbeloep: Kroner,
    val beregningsperioder: List<NyBeregningsperiode>,
    val trygdetidsperioder: List<Trygdetidsperiode>,
)

data class NyBeregningsperiode(
    val inntekt: Kroner,
    val trygdetid: Int,
    val stoenadFoerReduksjon: Kroner,
    var utbetaltBeloep: Kroner,
)
