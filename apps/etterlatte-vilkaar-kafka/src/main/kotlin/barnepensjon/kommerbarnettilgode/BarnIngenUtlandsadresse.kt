package vilkaar.barnepensjon

import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDateTime

fun barnIngenOppgittUtlandsadresse(
        soekerSoeknad: VilkaarOpplysning<SoekerBarnSoeknad>?,
): VurdertVilkaar {

    val opplysningsGrunnlag = listOfNotNull(
        soekerSoeknad?.let {
            Kriteriegrunnlag(
                soekerSoeknad.id,
                KriterieOpplysningsType.SOEKER_UTENLANDSOPPHOLD,
                soekerSoeknad.kilde,
                soekerSoeknad.opplysning.utenlandsadresse
            )
        }
    )

    val resultat = try {
        if (soekerSoeknad == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            val ikkeAdresseIUtland = soekerSoeknad.opplysning.utenlandsadresse.adresseIUtlandet == JaNeiVetIkke.NEI
            vurderOpplysning { ikkeAdresseIUtland }
        }
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    val kriterie = Kriterie(
        Kriterietyper.SOEKER_IKKE_ADRESSE_I_UTLANDET,
        resultat,
        opplysningsGrunnlag
    )

    return VurdertVilkaar(
        Vilkaartyper.BARN_INGEN_OPPGITT_UTLANDSADRESSE,
        setVilkaarVurderingFraKriterier(listOf(kriterie)),
        null,
        listOf(kriterie),
        LocalDateTime.now()
    )
}