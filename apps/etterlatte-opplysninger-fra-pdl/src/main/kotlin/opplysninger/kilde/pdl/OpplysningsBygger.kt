package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Doedsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foedselsdato
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Foreldre
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
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
            personOpplysning(soekerPdl, Opplysningstyper.SOEKER_PERSONINFO_V1.value, PersonType.BARN),
            soekerFoedselsdato(soekerPdl, Opplysningstyper.SOEKER_FOEDSELSDATO_V1.value),
            personOpplysning(avdoedPdl, Opplysningstyper.AVDOED_PERSONINFO_V1.value, PersonType.AVDOED),
            avdoedDodsdato(avdoedPdl, Opplysningstyper.AVDOED_DOEDSFALL_V1.value),
            soekerRelasjonForeldre(soekerPdl, Opplysningstyper.SOEKER_RELASJON_FORELDRE_V1.value,  pdl)
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

    fun soekerRelasjonForeldre(soekerPdl: Person, opplysningsType: String,  pdl: Pdl) : Behandlingsopplysning<Foreldre> {
        val foreldreFraPdl = soekerPdl.familieRelasjon?.foreldre?.map { it.foedselsnummer.value }?.map {pdl.hentPdlModell(it)}
        val foreldrePersonInfo = foreldreFraPdl?.map { PersonInfo(it.fornavn, it.etternavn, it.foedselsnummer, PersonType.FORELDER ) }
        return  lagOpplysning(opplysningsType, Foreldre(foreldrePersonInfo))
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
