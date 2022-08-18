package model.brev.mapper

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.model.brev.Mottaker

fun Vedtak.finnMottaker(): Mottaker = this.grunnlag
    .find { it.opplysningType === Opplysningstyper.SOEKER_PDL_V1 }!!
    .let {
        val person: Person = objectMapper.readValue(it.opplysning.toJson())
        val adresse = requireNotNull(person.bostedsadresse?.find { adresse -> adresse.aktiv })

        Mottaker(
            fornavn = person.fornavn,
            etternavn = person.etternavn,
            adresse = listOfNotNull(adresse.adresseLinje1, adresse.adresseLinje2, adresse.adresseLinje3)
                .joinToString(" "),
            postnummer = requireNotNull(adresse.postnr),
            poststed = requireNotNull(adresse.poststed),
            land = adresse.land
        )
    }