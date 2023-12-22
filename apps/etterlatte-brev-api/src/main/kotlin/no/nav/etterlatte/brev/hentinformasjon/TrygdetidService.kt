package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.TrygdetidType
import org.slf4j.LoggerFactory
import java.util.UUID

class TrygdetidService(private val trygdetidKlient: TrygdetidKlient) {
    private val logger = LoggerFactory.getLogger(this::class.java)

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
        val (anvendtTrygdetid, prorataBroek) = hentBenyttetTrygdetidOgProratabroek(foersteBeregningsperiode)

        val trygdetidgrunnlagForAnvendtTrygdetid =
            trygdetiderIBehandling.find { it.ident == foersteBeregningsperiode.trygdetidForIdent }
                ?: trygdetiderIBehandling.first().also {
                    logger.error(
                        "Fant ikke riktig trygdetid for identen som er brukt i beregning, benytter den" +
                            " første av den vi fant i brevet, med id=${it.id} for behandlingId=${it.behandlingId}." +
                            " Vi fant total ${trygdetiderIBehandling.size} trygdetider for behandlingen",
                    )
                }

        if (trygdetidgrunnlagForAnvendtTrygdetid.beregnetTrygdetid?.resultat?.overstyrt == true) {
            // vi har en overstyrt trygdetid fra pesys, og vi kan dermed ikke gi ut noe detaljert grunnlag på hvordan
            // vi har kommet frem til trygdetiden
            return Trygdetid(
                aarTrygdetid = anvendtTrygdetid,
                prorataBroek = prorataBroek,
                maanederTrygdetid = 0,
                perioder = listOf(),
                overstyrt = true,
            )
        }

        val trygdetidsperioder =
            finnTrygdetidsperioderForTabell(
                trygdetidgrunnlagForAnvendtTrygdetid.trygdetidGrunnlag,
                foersteBeregningsperiode.beregningsMetode,
            )

        return Trygdetid(
            aarTrygdetid = anvendtTrygdetid,
            maanederTrygdetid = 0,
            prorataBroek = prorataBroek,
            perioder = trygdetidsperioder,
            overstyrt = false,
        )
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
