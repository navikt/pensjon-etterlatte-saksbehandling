package barnepensjon.kommerbarnettilgode

import no.nav.etterlatte.libs.common.saksbehandleropplysninger.ResultatKommerBarnetTilgode
import no.nav.etterlatte.libs.common.vikaar.*
import java.time.LocalDateTime

fun saksbehandlerResultat(
    vilkaartype: Vilkaartyper,
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
        vilkaartype,
        resultat,
        null,
        listOf(kriterie),
        LocalDateTime.now()
    )
}
