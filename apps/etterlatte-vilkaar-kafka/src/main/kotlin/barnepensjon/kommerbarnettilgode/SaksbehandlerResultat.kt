package barnepensjon.kommerbarnettilgode

import no.nav.etterlatte.libs.common.saksbehandleropplysninger.ResultatKommerBarnetTilgode
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDateTime

fun saksbehandlerResultat(
    saksbehandlerVurdering: VilkaarOpplysning<ResultatKommerBarnetTilgode>?
): VurdertVilkaar? {
    if (saksbehandlerVurdering == null) {
        return null
    }

    val opplysningsGrunnlag = listOfNotNull(
        saksbehandlerVurdering.let {
            Kriteriegrunnlag(
                saksbehandlerVurdering.id,
                KriterieOpplysningsType.SAKSBEHANDLER_RESULTAT,
                saksbehandlerVurdering.kilde,
                saksbehandlerVurdering.opplysning
            )
        }
    )

    val resultat = if (saksbehandlerVurdering.opplysning.svar == "JA") {
        VurderingsResultat.OPPFYLT
    } else {
        VurderingsResultat.IKKE_OPPFYLT
    }

    val kriterie = Kriterie(
        Kriterietyper.SAKSBEHANDLER_RESULTAT,
        resultat,
        opplysningsGrunnlag
    )

    return VurdertVilkaar(
        Vilkaartyper.SAKSBEHANDLER_RESULTAT,
        resultat,
        null,
        listOf(kriterie),
        LocalDateTime.now()
    )
}