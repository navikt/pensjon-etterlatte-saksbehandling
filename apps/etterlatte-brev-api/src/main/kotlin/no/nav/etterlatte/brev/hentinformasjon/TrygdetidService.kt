package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
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
        val beregning = requireNotNull(beregningKlient.hentBeregning(behandlingId, brukerTokenInfo))
        val trygdetider = trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo)

        // Beregningsmetode per nå er det samme på tvers av trygdetider
        val beregningsmetode =
            beregning.beregningsperioder.first { beregningsperiode ->
                trygdetider.any { it.ident == beregningsperiode.trygdetidForIdent }
            }.beregningsMetode

        return trygdetider.map { trygdetid ->
            val (samletTrygdetid, prorataBroek) = utledTrygdetid(trygdetid, beregningsmetode)
            Trygdetid(
                ident = trygdetid.ident,
                aarTrygdetid = samletTrygdetid,
                maanederTrygdetid = 0,
                prorataBroek = prorataBroek,
                perioder = finnTrygdetidsperioderForTabell(trygdetid.trygdetidGrunnlag, beregningsmetode),
                overstyrt = trygdetid.beregnetTrygdetid?.resultat?.overstyrt == true,
                mindreEnnFireFemtedelerAvOpptjeningstiden =
                    trygdetid.beregnetTrygdetid
                        ?.resultat?.fremtidigTrygdetidNorge?.mindreEnnFireFemtedelerAvOpptjeningstiden ?: false,
            )
        }
    }

    private fun utledTrygdetid(
        trygdetid: TrygdetidDto,
        beregningsmetode: BeregningsMetode?,
    ): Pair<Int, IntBroek?> {
        val trygdetidResultat = trygdetid.beregnetTrygdetid?.resultat ?: throw ManglerMedTrygdetidVeBrukIBrev()
        return when (beregningsmetode) {
            BeregningsMetode.NASJONAL ->
                Pair(
                    trygdetidResultat.samletTrygdetidNorge ?: throw ManglerMedTrygdetidVeBrukIBrev(),
                    null,
                )
            BeregningsMetode.PRORATA ->
                Pair(
                    trygdetidResultat.samletTrygdetidTeoretisk ?: throw ManglerMedTrygdetidVeBrukIBrev(),
                    trygdetidResultat.prorataBroek,
                )
            BeregningsMetode.BEST -> throw UgyldigBeregningsMetode()
            else -> throw ManglerMedTrygdetidVeBrukIBrev()
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

class ManglerMedTrygdetidVeBrukIBrev : UgyldigForespoerselException(
    code = "MANGLER_TRYGDETID_VED_BREV",
    detail = "Trygdetid har mangler ved bruk til brev",
)
