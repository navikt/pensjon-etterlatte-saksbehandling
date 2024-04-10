package no.nav.etterlatte.brev.hentinformasjon

import no.nav.etterlatte.brev.behandling.Trygdetid
import no.nav.etterlatte.brev.behandling.Trygdetidsperiode
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.TrygdetidType
import java.util.UUID

class TrygdetidService(private val trygdetidKlient: TrygdetidKlient) {
    suspend fun finnTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = trygdetidKlient.hentTrygdetid(behandlingId, brukerTokenInfo).map { trygdetid ->
        Trygdetid(
            ident = trygdetid.ident,
            samletTrygdetidNorge = trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge,
            samletTrygdetidTeoretisk = trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidTeoretisk,
            maanederTrygdetid = 0,
            prorataBroek = trygdetid.beregnetTrygdetid?.resultat?.prorataBroek,
            perioder =
                trygdetid.trygdetidGrunnlag.map { grunnlag ->
                    Trygdetidsperiode(
                        datoFOM = grunnlag.periodeFra,
                        datoTOM = grunnlag.periodeTil,
                        land = grunnlag.bosted,
                        opptjeningsperiode = grunnlag.beregnet,
                        type = TrygdetidType.valueOf(grunnlag.type),
                        prorata = grunnlag.prorata,
                    )
                },
            overstyrt = trygdetid.beregnetTrygdetid?.resultat?.overstyrt == true,
            mindreEnnFireFemtedelerAvOpptjeningstiden =
                trygdetid.beregnetTrygdetid
                    ?.resultat?.fremtidigTrygdetidNorge?.mindreEnnFireFemtedelerAvOpptjeningstiden ?: false,
        )
    }
}
