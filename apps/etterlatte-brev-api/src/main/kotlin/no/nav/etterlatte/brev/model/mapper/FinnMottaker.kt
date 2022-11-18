package no.nav.etterlatte.brev.model.mapper

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.toJson

fun Vedtak.finnMottaker(): Mottaker = this.grunnlag
    .find { it.opplysningType === Opplysningstype.SOEKER_PDL_V1 }!!
    .let {
        val person: Person = objectMapper.readValue(it.opplysning.toJson())
        val adresse = requireNotNull(person.bostedsadresse?.find { adresse -> adresse.aktiv })

        Mottaker(
            navn = "${person.fornavn} ${person.etternavn}",
            adresse = listOfNotNull(adresse.adresseLinje1, adresse.adresseLinje2, adresse.adresseLinje3)
                .joinToString(" "),
            postnummer = requireNotNull(adresse.postnr),
            poststed = requireNotNull(adresse.poststed),
            land = adresse.land
        )
    }