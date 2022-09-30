package no.nav.etterlatte.barnepensjon

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.hentBostedsadresse
import no.nav.etterlatte.libs.common.grunnlag.hentKontaktadresse
import no.nav.etterlatte.libs.common.grunnlag.hentOppholdsadresse

fun hentAdresser(
    person: Grunnlagsdata<JsonNode>
): Adresser {
    val bostedadresse = person.hentBostedsadresse()?.perioder?.map { it.verdi }
    val oppholdadresse = person.hentOppholdsadresse()?.verdi
    val kontaktadresse = person.hentKontaktadresse()?.verdi

    val adresser = Adresser(bostedadresse, oppholdadresse, kontaktadresse)
    val ingenAdresser = bostedadresse == null && oppholdadresse == null && kontaktadresse == null

    return if (ingenAdresser) {
        throw OpplysningKanIkkeHentesUt()
    } else {
        adresser
    }
}