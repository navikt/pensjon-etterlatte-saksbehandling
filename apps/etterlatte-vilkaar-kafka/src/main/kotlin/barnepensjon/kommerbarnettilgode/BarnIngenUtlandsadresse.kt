package vilkaar.barnepensjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraKriterier
import no.nav.etterlatte.barnepensjon.vurderOpplysning
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentUtenlandsadresse
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.vikaar.Kriterie
import no.nav.etterlatte.libs.common.vikaar.KriterieOpplysningsType
import no.nav.etterlatte.libs.common.vikaar.Kriteriegrunnlag
import no.nav.etterlatte.libs.common.vikaar.Kriterietyper
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.libs.common.vikaar.VurdertVilkaar
import java.time.LocalDateTime

fun barnIngenOppgittUtlandsadresse(
    soeker: Grunnlagsdata<JsonNode>?
): VurdertVilkaar {
    val utenlandsopphold = soeker?.hentUtenlandsadresse()
    val opplysningsGrunnlag = listOfNotNull(
        utenlandsopphold?.let {
            Kriteriegrunnlag(
                it.id,
                KriterieOpplysningsType.SOEKER_UTENLANDSOPPHOLD,
                it.kilde,
                it.verdi
            )
        }
    )

    val resultat = try {
        if (utenlandsopphold == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            val ikkeAdresseIUtland = utenlandsopphold.verdi.harHattUtenlandsopphold == JaNeiVetIkke.NEI
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