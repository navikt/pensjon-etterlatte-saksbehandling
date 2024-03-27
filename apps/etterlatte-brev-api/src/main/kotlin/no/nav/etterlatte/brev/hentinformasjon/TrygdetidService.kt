package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
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
        val beregning = requireNotNull(beregningKlient.hentBeregning(behandlingId, brukerTokenInfo))
        return trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo).map { trygdetid ->
            // Trygdetid, proratabrøk og beregningsmetode brukt for en avdød vil alltid være det samme
            val beregningsperiode = beregning.beregningsperioder.first { it.trygdetidForIdent == trygdetid.ident }
            val (anvendtTrygdetid, prorataBroek) = hentBenyttetTrygdetidOgProratabroek(beregningsperiode)
            Trygdetid(
                ident = trygdetid.ident,
                aarTrygdetid = anvendtTrygdetid,
                maanederTrygdetid = 0,
                prorataBroek = prorataBroek,
                perioder =
                    finnTrygdetidsperioderForTabell(
                        trygdetid.trygdetidGrunnlag,
                        beregningsperiode.beregningsMetode,
                    ),
                overstyrt = trygdetid.beregnetTrygdetid?.resultat?.overstyrt == true,
                mindreEnnFireFemtedelerAvOpptjeningstiden =
                    trygdetid.beregnetTrygdetid
                        ?.resultat?.fremtidigTrygdetidNorge?.mindreEnnFireFemtedelerAvOpptjeningstiden ?: false,
            )
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
