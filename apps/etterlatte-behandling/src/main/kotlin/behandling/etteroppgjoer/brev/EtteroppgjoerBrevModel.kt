package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.DetaljertForbehandlingDto
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.brev.BrevRedigerbarInnholdData
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevData
import no.nav.etterlatte.brev.model.oms.EtteroppgjoerBrevGrunnlag
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.pensjon.brevbaker.api.model.Kroner

data class EtteroppgjoerBrevRequestData(
    val redigerbar: BrevRedigerbarInnholdData,
    val innhold: BrevFastInnholdData,
    val sak: Sak,
)

object EtteroppgjoerBrevDataMapper {
    fun fra(
        data: DetaljertForbehandlingDto,
        sisteIverksatteBehandling: Behandling,
        pensjonsgivendeInntekt: PensjonsgivendeInntektFraSkatt?,
    ): EtteroppgjoerBrevRequestData {
        krevIkkeNull(data.beregnetEtteroppgjoerResultat) {
            "Beregnet etteroppgjoer resultat er null og kan ikke vises i brev"
        }

        val bosattUtland = sisteIverksatteBehandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND
        val grunnlag = data.faktiskInntekt ?: throw InternfeilException("Etteroppgjør mangler faktisk inntekt og kan ikke vises i brev")

        // TODO: usikker om dette blir rett, følge opp ifm testing
        val norskInntekt = pensjonsgivendeInntekt != null && pensjonsgivendeInntekt.inntekter.isNotEmpty()

        return EtteroppgjoerBrevRequestData(
            redigerbar =
                EtteroppgjoerBrevData.ForhaandsvarselInnhold(
                    bosattUtland = bosattUtland,
                    norskInntekt = norskInntekt,
                    etteroppgjoersAar = data.behandling.aar,
                    rettsgebyrBeloep = Kroner(data.beregnetEtteroppgjoerResultat.grense.rettsgebyr),
                    resultatType = data.beregnetEtteroppgjoerResultat.resultatType,
                    avviksBeloep = Kroner(data.beregnetEtteroppgjoerResultat.differanse.toInt()),
                    sak = sisteIverksatteBehandling.sak,
                ),
            innhold =
                EtteroppgjoerBrevData.Forhaandsvarsel(
                    bosattUtland = bosattUtland,
                    norskInntekt = norskInntekt,
                    etteroppgjoersAar = data.behandling.aar,
                    rettsgebyrBeloep = Kroner(data.beregnetEtteroppgjoerResultat.grense.rettsgebyr),
                    resultatType = data.beregnetEtteroppgjoerResultat.resultatType,
                    inntekt = Kroner(data.beregnetEtteroppgjoerResultat.utbetaltStoenad.toInt()),
                    faktiskInntekt = Kroner(data.beregnetEtteroppgjoerResultat.nyBruttoStoenad.toInt()),
                    avviksBeloep = Kroner(data.beregnetEtteroppgjoerResultat.differanse.toInt()),
                    grunnlag =
                        EtteroppgjoerBrevGrunnlag(
                            fom = grunnlag.fom,
                            tom = grunnlag.tom!!,
                            innvilgedeMaaneder = grunnlag.innvilgaMaaneder,
                            loennsinntekt = Kroner(grunnlag.loennsinntekt),
                            naeringsinntekt = Kroner(grunnlag.naeringsinntekt),
                            afp = Kroner(grunnlag.afp),
                            utlandsinntekt = Kroner(grunnlag.utlandsinntekt),
                        ),
                ),
            sak = sisteIverksatteBehandling.sak,
        )
    }
}
