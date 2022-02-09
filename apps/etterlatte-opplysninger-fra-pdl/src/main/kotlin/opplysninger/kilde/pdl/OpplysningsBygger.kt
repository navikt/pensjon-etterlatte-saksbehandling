package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.PersonInfo
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import java.time.Instant
import java.time.LocalDate
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {



    override fun byggOpplysninger(barnepensjon: Barnepensjon, pdl: Pdl): List<Behandlingsopplysning<out Any>> {

        val soekersFnr = barnepensjon.soeker.foedselsnummer.value
        val avdoedFnr = hentAvdoedFnr(barnepensjon)

        val soekerPdl = pdl.hentPdlModell(soekersFnr)
        val avdoedPdl = pdl.hentPdlModell(avdoedFnr)

        return listOf(
            personOpplysning(soekerPdl, "soeker_personinfo:v1", PersonType.BARN),
            personOpplysning(avdoedPdl, "avdoed_personinfo:v1", PersonType.AVDOED),
            avdoedDodsdato(avdoedPdl, "avdoed_doedsfall:v1"),
            soekerFoedselsdato(soekerPdl, "soeker_foedselsdato:v1")
        )
    }

    fun personOpplysning(soekerPdl: Person, opplysningsType: String, personType: PersonType): Behandlingsopplysning<PersonInfo> {
        val soekerPersonInfo = PersonInfo(soekerPdl.fornavn, soekerPdl.etternavn, soekerPdl.foedselsnummer, personType)
        return lagOpplysning(opplysningsType, soekerPersonInfo)
    }

    fun avdoedDodsdato(avdoedPdl: Person, opplysningsType: String): Behandlingsopplysning<Doedsdato> {
        return lagOpplysning(opplysningsType, Doedsdato(LocalDate.parse(avdoedPdl.doedsdato), avdoedPdl.foedselsnummer.value))
    }

    fun soekerFoedselsdato(soekerPdl: Person, opplysningsType: String): Behandlingsopplysning<Foedselsdato> {
        return lagOpplysning(opplysningsType, Foedselsdato(LocalDate.parse(soekerPdl.foedselsdato), soekerPdl.foedselsnummer.value))

    }

    fun hentAvdoedFnr(barnepensjon: Barnepensjon): String {
        val fnr = barnepensjon.foreldre.find { it.type === PersonType.AVDOED }?.foedselsnummer?.value
        if(fnr != null) {
            return fnr
        }
        throw Exception("Mangler fødselsnummer")
    }


}

fun <T> lagOpplysning(opplysningsType: String, opplysning: T): Behandlingsopplysning<T> {
    return Behandlingsopplysning(
        UUID.randomUUID(),
        Behandlingsopplysning.Register("pdl", Instant.now(), null),
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}
