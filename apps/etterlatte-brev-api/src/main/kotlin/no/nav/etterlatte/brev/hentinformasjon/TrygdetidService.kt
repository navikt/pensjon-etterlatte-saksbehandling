package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.token.BrukerTokenInfo
import java.util.UUID

class TrygdetidService(private val trygdetidKlient: TrygdetidKlient) {
    suspend fun finnTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Trygdetid? {
        val trygdetidMedGrunnlag = trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo) ?: return null

        val trygdetidsperioder = finnTrygdetidsperioder(trygdetidMedGrunnlag)

        val beregnetTrygdetid = trygdetidMedGrunnlag.beregnetTrygdetid?.resultat
        val samlaTrygdetid = beregnetTrygdetid?.samletTrygdetidNorge ?: beregnetTrygdetid?.samletTrygdetidTeoretisk ?: 0
        val aarTrygdetid = samlaTrygdetid.div(12)

        return Trygdetid(
            aarTrygdetid = aarTrygdetid,
            maanederTrygdetid = samlaTrygdetid % 12,
            perioder = trygdetidsperioder,
        )
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
