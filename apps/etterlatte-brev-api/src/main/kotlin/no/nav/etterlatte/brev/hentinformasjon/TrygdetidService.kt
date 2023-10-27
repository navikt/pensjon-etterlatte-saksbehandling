package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID
import kotlin.math.max

class TrygdetidService(private val trygdetidKlient: TrygdetidKlient) {
    suspend fun finnTrygdetid(
        behandlingId: UUID,
        beregning: BeregningDTO,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        val trygdetidMedGrunnlag: TrygdetidDto =
            trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo) ?: return null

        val trygdetidsperioder = finnTrygdetidsperioder(trygdetidMedGrunnlag)

        val samlaTrygdetid2 =
            beregning.beregningsperioder
                .sumOf {
                    when (it.beregningsMetode) {
                        BeregningsMetode.NASJONAL -> it.samletNorskTrygdetid ?: 0
                        BeregningsMetode.PRORATA -> beregnProrata(it.samletTeoretiskTrygdetid, it.broek)
                        BeregningsMetode.BEST ->
                            max(
                                it.samletNorskTrygdetid ?: 0,
                                beregnProrata(it.samletTeoretiskTrygdetid, it.broek),
                            )
                        null -> 0
                    }
                }
        /*
        Nasjonal - bruk samletTrygdetidNorge
        Prorata - bruk samletTrygdetidTeoretisk ganget med prorata brøk (og her viser vi trygdetid og brøk - ikke resulterende verdi i gjenny)
        Best - regn ut begge to og bruk den som gir høyest verdi
         */

        val beregnetTrygdetid = trygdetidMedGrunnlag.beregnetTrygdetid
        val resultat = beregnetTrygdetid?.resultat
        val samlaTrygdetid = resultat?.samletTrygdetidNorge ?: resultat?.samletTrygdetidTeoretisk ?: 0

        return Trygdetid(
            aarTrygdetid = samlaTrygdetid2,
            maanederTrygdetid = samlaTrygdetid % 12,
            perioder = trygdetidsperioder,
        )
    }

    private fun beregnProrata(
        samletTeoretiskTrygdetid: Int?,
        broek: IntBroek?,
    ): Int {
        if (broek == null) return samletTeoretiskTrygdetid ?: 0
        val samlet = samletTeoretiskTrygdetid ?: 0
        val br = broek.teller / broek.nevner
        return samlet.times(br)
    }

    private fun finnTrygdetidsperioder(trygdetidMedGrunnlag: TrygdetidDto) =
        trygdetidMedGrunnlag.trygdetidGrunnlag.map {
            Trygdetidsperiode(
                datoFOM = it.periodeFra,
                datoTOM = it.periodeTil,
                land = it.bosted,
                opptjeningsperiode = it.beregnet?.aar.toString(),
            )
        }
}
