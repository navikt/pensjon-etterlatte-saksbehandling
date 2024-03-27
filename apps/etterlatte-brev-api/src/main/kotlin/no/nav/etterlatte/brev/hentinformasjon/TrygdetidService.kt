package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.TrygdetidType
import org.slf4j.LoggerFactory
import java.util.UUID

class TrygdetidService(private val trygdetidKlient: TrygdetidKlient, private val beregningKlient: BeregningKlient) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun finnTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        val beregning = beregningKlient.hentBeregning(behandlingId, brukerTokenInfo)!!

        return finnTrygdetidsgrunnlag(behandlingId, beregning, brukerTokenInfo)
    }

    suspend fun finnTrygdetidsgrunnlag(
        behandlingId: UUID,
        beregning: BeregningDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Trygdetid> {
        val trygdetiderIBehandling: List<TrygdetidDto> =
            trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo)
        if (trygdetiderIBehandling.isEmpty()) {
            // return null
            return emptyList()
        }

        // TODO kaste feil hvis mangler elns?
        return trygdetiderIBehandling.map { trygdetid ->

            // Trygdetid anvendt kan variere mellom beregningsperiodene, i tilfeller med mer enn en avdød.
            // Dette spesialtilfellet er litt uheldig for oss, men tilnærmingen er
            // foreløpig å bruke første beregningsperiode som grunnlag. Da kan man håndtere skiller i trygdetid vi
            // dokumenterer i brevet ved å først innvilge, og så gjøre en revurdering.
            // Hvis vi har lyst til å støtte mer komplekse førstegangsinnvilgelser / behandlinger som går over
            // over flere perioder med ulike trygdetider må man håndtere dette i brevet, men det er ikke noe som er
            // lagt opp til annet enn en eventuell redigering av fritekst i trygdetidsvedlegget.
            // det vil uansett bli riktig for alle saker der vi kun benytter en trygdetid, som er overveiende flesteparten
            // TODO doc her fortsatt relevent??
            val foersteBeregningsperiode = beregning.beregningsperioder.first { it.trygdetidForIdent == trygdetid.ident }
            // TODO Vi har avklart at det ikke vil være ulike metoder i de ulike periodene til EN avdød? Så første er trygt å anta?
            val (anvendtTrygdetid, prorataBroek) = hentBenyttetTrygdetidOgProratabroek(foersteBeregningsperiode)

            if (trygdetid.beregnetTrygdetid?.resultat?.overstyrt == true) {
                // TODO Kan dette fjernes?
                // vi har en overstyrt trygdetid fra pesys, og vi kan dermed ikke gi ut noe detaljert grunnlag på hvordan
                // vi har kommet frem til trygdetiden
                Trygdetid(
                    ident = trygdetid.ident,
                    aarTrygdetid = anvendtTrygdetid,
                    prorataBroek = prorataBroek,
                    maanederTrygdetid = 0,
                    perioder = listOf(),
                    overstyrt = true,
                    mindreEnnFireFemtedelerAvOpptjeningstiden =
                        trygdetid.beregnetTrygdetid
                            ?.resultat?.fremtidigTrygdetidNorge?.mindreEnnFireFemtedelerAvOpptjeningstiden ?: false,
                )
            } else {
                val trygdetidsperioder =
                    finnTrygdetidsperioderForTabell(
                        trygdetid.trygdetidGrunnlag,
                        foersteBeregningsperiode.beregningsMetode,
                    )

                Trygdetid(
                    ident = trygdetid.ident,
                    aarTrygdetid = anvendtTrygdetid,
                    maanederTrygdetid = 0,
                    prorataBroek = prorataBroek,
                    perioder = trygdetidsperioder,
                    overstyrt = false,
                    mindreEnnFireFemtedelerAvOpptjeningstiden =
                        trygdetid.beregnetTrygdetid
                            ?.resultat?.fremtidigTrygdetidNorge?.mindreEnnFireFemtedelerAvOpptjeningstiden ?: false,
                )
            }
        }
    }

    private fun finnTrygdetidsperioderForTabell(
        trygdetidsgrunnlag: List<TrygdetidGrunnlagDto>,
        beregningsMetode: BeregningsMetode?,
    ): List<Trygdetidsperiode> {
        return when (beregningsMetode) {
            BeregningsMetode.NASJONAL -> {
                // Kun ta med nasjonale perioder
                trygdetidsgrunnlag
                    .filter { it.bosted == "NOR" }
                    .map(::toTrygdetidsperiode)
            }
            BeregningsMetode.PRORATA -> {
                // Kun ta med de som er avtaleland
                trygdetidsgrunnlag
                    .filter { it.prorata }
                    .map(::toTrygdetidsperiode)
            }
            else -> throw IllegalArgumentException("$beregningsMetode er ikke en gyldig beregningsmetode")
        }
    }

    private fun toTrygdetidsperiode(grunnlag: TrygdetidGrunnlagDto) =
        Trygdetidsperiode(
            datoFOM = grunnlag.periodeFra,
            datoTOM = grunnlag.periodeTil,
            land = grunnlag.bosted,
            opptjeningsperiode = grunnlag.beregnet,
            type = TrygdetidType.valueOf(grunnlag.type),
        )
}
