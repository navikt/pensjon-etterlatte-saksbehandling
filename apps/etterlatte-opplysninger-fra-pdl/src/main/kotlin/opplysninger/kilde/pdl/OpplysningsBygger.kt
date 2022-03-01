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
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {

    override fun byggOpplysninger(barnepensjon: Barnepensjon, pdl: Pdl): List<Behandlingsopplysning<out Any>> {

        val soekersFnr = barnepensjon.soeker.foedselsnummer.svar.value
        val avdoedFnr = hentAvdoedFnr(barnepensjon)
        val gjenlevendeForelderFnr = hentGjenlevendeForelderFnr(barnepensjon)

        val soekerPdl = pdl.hentPdlModell(soekersFnr)
        val avdoedPdl = pdl.hentPdlModell(avdoedFnr)
        val gjenlevendeForelderPdl = pdl.hentPdlModell(gjenlevendeForelderFnr)

        return listOf(
            personOpplysning(soekerPdl, Opplysningstyper.SOEKER_PERSONINFO_V1.value, PersonType.BARN),
            soekerFoedselsdato(soekerPdl, Opplysningstyper.SOEKER_FOEDSELSDATO_V1.value),
            personOpplysning(avdoedPdl, Opplysningstyper.AVDOED_PERSONINFO_V1.value, PersonType.AVDOED),
            avdoedDodsdato(avdoedPdl, Opplysningstyper.AVDOED_DOEDSFALL_V1.value),
            soekerRelasjonForeldre(soekerPdl, Opplysningstyper.SOEKER_RELASJON_FORELDRE_V1.value, pdl),
            gjenlevendeForelderOpplysning(gjenlevendeForelderPdl, Opplysningstyper.GJENLEVENDE_FORELDER_PERSONINFO_V1.value)
        )
    }

    fun gjenlevendeForelderOpplysning(gjenlevendePdl: Person, opplysningsType: String): Behandlingsopplysning<PersonInfo> {
        val gjenlevendePersonInfo = PersonInfo(gjenlevendePdl.fornavn, gjenlevendePdl.etternavn, gjenlevendePdl.foedselsnummer, "adresse tba", PersonType.GJENLEVENDE_FORELDER)
        return lagOpplysning(opplysningsType, gjenlevendePersonInfo);
    }

    fun personOpplysning(soekerPdl: Person, opplysningsType: String, personType: PersonType): Behandlingsopplysning<PersonInfo> {
        val soekerPersonInfo = PersonInfo(soekerPdl.fornavn, soekerPdl.etternavn, soekerPdl.foedselsnummer, "Adresse", personType)
        return lagOpplysning(opplysningsType, soekerPersonInfo)
    }

    fun avdoedDodsdato(avdoedPdl: Person, opplysningsType: String): Behandlingsopplysning<Doedsdato> {
        return lagOpplysning(opplysningsType, Doedsdato(avdoedPdl.doedsdato!!, avdoedPdl.foedselsnummer.value)) // TODO
    }

    fun soekerFoedselsdato(soekerPdl: Person, opplysningsType: String): Behandlingsopplysning<Foedselsdato> {
        return lagOpplysning(opplysningsType, Foedselsdato(soekerPdl.foedselsdato!!, soekerPdl.foedselsnummer.value)) // TODO
    }

    fun soekerRelasjonForeldre(soekerPdl: Person, opplysningsType: String,  pdl: Pdl) : Behandlingsopplysning<Foreldre> {
        val foreldreFraPdl = soekerPdl.familieRelasjon?.foreldre?.map { it.foedselsnummer.value }?.map {pdl.hentPdlModell(it)}
        println(foreldreFraPdl)
        val foreldrePersonInfo = foreldreFraPdl?.map { PersonInfo(it.fornavn, it.etternavn, it.foedselsnummer, "Adresse", PersonType.FORELDER ) }

        return lagOpplysning(opplysningsType, Foreldre(foreldrePersonInfo))
    }

    fun hentAvdoedFnr(barnepensjon: Barnepensjon): String {
        val fnr = barnepensjon.foreldre.find { it.type === PersonType.AVDOED }?.foedselsnummer?.svar?.value
        if(fnr != null) {
            return fnr
        }
        throw Exception("Mangler fødselsnummer")
    }

    fun hentGjenlevendeForelderFnr(barnepensjon: Barnepensjon): String {
        val fnr = barnepensjon.foreldre.find { it.type === PersonType.GJENLEVENDE_FORELDER }?.foedselsnummer?.svar?.value
        if(fnr != null) {
            return fnr
        }
        throw Exception("Mangler fødselsnummer på gjenlevende forelder")
    }

}

fun <T> lagOpplysning(opplysningsType: String, opplysning: T): Behandlingsopplysning<T> {
    return Behandlingsopplysning(
        UUID.randomUUID(),
        Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}
