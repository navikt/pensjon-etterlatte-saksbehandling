package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

class TrygdetidService(private val trygdetidKlient: TrygdetidKlient) {
    suspend fun finnTrygdetidsgrunnlag(
        behandlingId: UUID,
        beregning: BeregningDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        val trygdetiderIBehandling: List<TrygdetidDto> =
            trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo)
        if (trygdetiderIBehandling.isEmpty()) {
            return null
        }

        // Trygdetid anvendt kan variere mellom beregningsperiodene, i tilfeller med mer enn en avdød.
        // Dette spesialtilfellet er litt uheldig for oss, men tilnærmingen er
        // foreløpig å bruke første beregningsperiode som grunnlag. Da kan man håndtere skiller i trygdetid vi
        // dokumenterer i brevet ved å først innvilge, og så gjøre en revurdering.
        // Hvis vi har lyst til å støtte mer komplekse førstegangsinnvilgelser / behandlinger som går over
        // over flere perioder med ulike trygdetider må man håndtere dette i brevet, men det er ikke noe som er
        // lagt opp til annet enn en eventuell redigering av fritekst i trygdetidsvedlegget.
        // det vil uansett bli riktig for alle saker der vi kun benytter en trygdetid, som er overveiende flesteparten
        val foersteBeregningsperiode = beregning.beregningsperioder.minBy { it.datoFOM }
        val anvendtTrygdetid = foersteBeregningsperiode.trygdetid

        val trygdetidgrunnlagForAnvendtTrygdetid =
            trygdetiderIBehandling.find { it.ident == foersteBeregningsperiode.trygdetidForIdent }
                ?: trygdetiderIBehandling.first().also {
                    "Fant ikke riktig trygdetid for identen som er brukt i beregning"
                }

        if (trygdetidgrunnlagForAnvendtTrygdetid.beregnetTrygdetid?.resultat?.overstyrt == true) {
            // vi har en overstyrt trygdetid fra pesys, og vi kan dermed ikke gi ut noe detaljert grunnlag på hvordan
            // vi har kommet frem til trygdetiden
            return Trygdetid(
                aarTrygdetid = anvendtTrygdetid,
                maanederTrygdetid = 0,
                perioder = listOf(),
                overstyrt = true,
            )
        }

        val trygdetidsperioder =
            finnTrygdetidsperioderForTabell(trygdetidgrunnlagForAnvendtTrygdetid.trygdetidGrunnlag, anvendtTrygdetid)

        return Trygdetid(
            aarTrygdetid = anvendtTrygdetid,
            maanederTrygdetid = 0,
            perioder = trygdetidsperioder,
            overstyrt = false,
        )
    }

    private fun finnTrygdetidsperioderForTabell(
        trygdetidsgrunnlag: List<TrygdetidGrunnlagDto>,
        anvendtTrygdetid: Int,
    ): List<Trygdetidsperiode> {
        val harKunNorskeTrygdetidsperioder = trygdetidsgrunnlag.all { !it.prorata || it.bosted == "NOR" }

        // Vi skal kun vise tabell hvis den har relevante perioder for bruker, dvs. vi har med avtaleland utenfor Norge
        // eller vi har anvendt trygdetid < 40. Hvis ikke (kun norske og anvendt trygdetid = 40) er det unødvendig
        if (harKunNorskeTrygdetidsperioder && anvendtTrygdetid == 40) {
            return emptyList()
        }

        return trygdetidsgrunnlag
            .filter { it.prorata } // Vi skal kun ha med de som er avtaleland, dvs. er med i prorata
            .map { grunnlag ->
                Trygdetidsperiode(
                    datoFOM = grunnlag.periodeFra,
                    datoTOM = grunnlag.periodeTil,
                    land = grunnlag.bosted,
                    opptjeningsperiode = grunnlag.beregnet,
                )
            }
    }
}
