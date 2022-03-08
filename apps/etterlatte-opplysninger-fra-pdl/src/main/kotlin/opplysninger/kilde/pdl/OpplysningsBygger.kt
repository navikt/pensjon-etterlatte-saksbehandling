package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.*
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import java.time.Instant
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {

    override fun byggOpplysninger(barnepensjon: Barnepensjon, pdl: Pdl): List<Behandlingsopplysning<out Any>> {

        val soekersFnr = barnepensjon.soeker.foedselsnummer.svar.value
        val avdoedFnr = hentAvdoedFnr(barnepensjon)
        val gjenlevendeForelderFnr = hentGjenlevendeForelderFnr(barnepensjon)

        val soekerPdl = pdl.hentPdlModell(soekersFnr, PersonRolle.BARN)
        val avdoedPdl = pdl.hentPdlModell(avdoedFnr, PersonRolle.AVDOED)
        val gjenlevendeForelderPdl = pdl.hentPdlModell(gjenlevendeForelderFnr, PersonRolle.GJENLEVENDE)

        return listOf(
            personOpplysning(soekerPdl, Opplysningstyper.SOEKER_PERSONINFO_V1, PersonType.BARN),
            soekerFoedselsdato(soekerPdl, Opplysningstyper.SOEKER_FOEDSELSDATO_V1),
            personOpplysning(avdoedPdl, Opplysningstyper.AVDOED_PERSONINFO_V1, PersonType.AVDOED),
            avdoedDodsdato(avdoedPdl, Opplysningstyper.AVDOED_DOEDSFALL_V1),
            avdoedInnOgUtflytting(avdoedPdl, Opplysningstyper.AVDOED_INN_OG_UTFLYTTING_V1),
            soekerRelasjonForeldre(soekerPdl, Opplysningstyper.SOEKER_RELASJON_FORELDRE_V1, pdl),
            soekerBostedadresse(soekerPdl, Opplysningstyper.SOEKER_BOSTEDADRESSE_V1),
            soekerOppholdadresse(soekerPdl, Opplysningstyper.SOEKER_OPPHOLDADRESSE_V1),
            soekerKontaktadresse(soekerPdl, Opplysningstyper.SOEKER_KONTAKTADRESSE_V1),
            avdoedBostedadresse(avdoedPdl, Opplysningstyper.AVDOED_BOSTEDADRESSE_V1),
            avdoedOppholdadresse(avdoedPdl, Opplysningstyper.SOEKER_OPPHOLDADRESSE_V1),
            gjenlevendeBostedadresse(gjenlevendeForelderPdl, Opplysningstyper.GJENLEVENDE_FORELDER_BOSTEDADRESSE_V1),
            gjenlevendeOppholdadresse(gjenlevendeForelderPdl, Opplysningstyper.GJENLEVENDE_FORELDER_OPPHOLDADRESSE_V1),
            gjenlevendeForelderOpplysning(
                gjenlevendeForelderPdl,
                Opplysningstyper.GJENLEVENDE_FORELDER_PERSONINFO_V1
            )
        )
    }

    fun avdoedInnOgUtflytting(avdoedPdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<UtlandInnOgUtflytting> {
        val harHattUtenlandsopphold =  if (avdoedPdl.utland == null) "NEI" else "JA"
        val opphold = UtlandInnOgUtflytting(
            harHattUtenlandsopphold = harHattUtenlandsopphold,
            innflytting = avdoedPdl.utland?.innflyttingTilNorge?.map { InnflyttingTilNorge(it.fraflyttingsland, it.dato) },
            utflytting = avdoedPdl.utland?.utflyttingFraNorge?.map { UtflyttingFraNorge(it.tilflyttingsland, it.dato) },
            foedselsnummer = avdoedPdl.foedselsnummer.value
        )
        return lagOpplysning(opplysningsType, opphold)
    }

    fun gjenlevendeForelderOpplysning(
        gjenlevendePdl: Person,
        opplysningsType: Opplysningstyper
    ): Behandlingsopplysning<PersonInfo> {
        val gjenlevendePersonInfo = PersonInfo(
            gjenlevendePdl.fornavn,
            gjenlevendePdl.etternavn,
            gjenlevendePdl.foedselsnummer,
            "adresse tba",
            PersonType.GJENLEVENDE_FORELDER
        )
        return lagOpplysning(opplysningsType, gjenlevendePersonInfo);
    }

    fun personOpplysning(
        soekerPdl: Person,
        opplysningsType: Opplysningstyper,
        personType: PersonType
    ): Behandlingsopplysning<PersonInfo> {
        val soekerPersonInfo =
            PersonInfo(soekerPdl.fornavn, soekerPdl.etternavn, soekerPdl.foedselsnummer, "Adresse", personType)
        return lagOpplysning(opplysningsType, soekerPersonInfo)
    }

    fun avdoedDodsdato(avdoedPdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<Doedsdato> {
        return lagOpplysning(opplysningsType, Doedsdato(avdoedPdl.doedsdato!!, avdoedPdl.foedselsnummer.value)) // TODO
    }

    fun soekerFoedselsdato(soekerPdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<Foedselsdato> {
        return lagOpplysning(
            opplysningsType,
            Foedselsdato(soekerPdl.foedselsdato!!, soekerPdl.foedselsnummer.value)
        ) // TODO
    }

    fun soekerRelasjonForeldre(soekerPdl: Person, opplysningsType: Opplysningstyper, pdl: Pdl): Behandlingsopplysning<Foreldre> {
        val foreldreFraPdl =
            soekerPdl.familieRelasjon?.foreldre?.map { it.value }
                ?.map { pdl.hentPdlModell(it, PersonRolle.GJENLEVENDE) }
        val foreldrePersonInfo = foreldreFraPdl?.map {
            PersonInfo(
                it.fornavn,
                it.etternavn,
                it.foedselsnummer,
                "Adresse",
                PersonType.FORELDER
            )
        }

        return lagOpplysning(opplysningsType, Foreldre(foreldrePersonInfo))
    }

    fun soekerBostedadresse(soekerPdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<Bostedadresse> {
        val adresse = soekerPdl.bostedsadresse?.map { mapAdresse(it)}
        return lagOpplysning(opplysningsType, Bostedadresse(adresse))
    }

    fun soekerOppholdadresse(soekerPdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<Oppholdadresse> {
        val adresse = soekerPdl.oppholdsadresse?.map { mapAdresse(it) }
        return lagOpplysning(opplysningsType, Oppholdadresse(adresse))
    }

<<<<<<< HEAD
    fun soekerKontaktadresse(soekerPdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<Kontaktadresse> {
        val adresse = soekerPdl.kontaktadresse?.map { mapAdresse(it) }
        return lagOpplysning(opplysningsType, Kontaktadresse(adresse))
    }

=======
    fun avdoedBostedadresse(pdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<Bostedadresse> {
        val adresse = pdl.bostedsadresse?.map { mapAdresse(it)}
        return lagOpplysning(opplysningsType, Bostedadresse(adresse))
    }

    fun avdoedOppholdadresse(pdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<Oppholdadresse> {
        val adresse = pdl.oppholdsadresse?.map { mapAdresse(it) }
        return lagOpplysning(opplysningsType, Oppholdadresse(adresse))
    }

    fun gjenlevendeBostedadresse(pdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<Bostedadresse> {
        val adresse = pdl.bostedsadresse?.map { mapAdresse(it)}
        return lagOpplysning(opplysningsType, Bostedadresse(adresse))
    }

    fun gjenlevendeOppholdadresse(pdl: Person, opplysningsType: Opplysningstyper): Behandlingsopplysning<Oppholdadresse> {
        val adresse = pdl.oppholdsadresse?.map { mapAdresse(it) }
        return lagOpplysning(opplysningsType, Oppholdadresse(adresse))
    }
>>>>>>> fb75320 (opplysninger på gjenlevende og avoded)

    fun mapAdresse(it: no.nav.etterlatte.libs.common.person.Adresse): Adresse {
        return Adresse(
            it.type,
            it.aktiv,
            it.coAdresseNavn,
            it.adresseLinje1,
            it.adresseLinje2,
            it.adresseLinje3,
            it.postnr,
            it.poststed,
            it.land,
            it.kilde,
            it.gyldigFraOgMed?.toLocalDate(),
            it.gyldigTilOgMed?.toLocalDate()
        )
    }

    fun hentAvdoedFnr(barnepensjon: Barnepensjon): String {
        val fnr = barnepensjon.foreldre.find { it.type === PersonType.AVDOED }?.foedselsnummer?.svar?.value
        if (fnr != null) {
            return fnr
        }
        throw Exception("Mangler fødselsnummer")
    }

    fun hentGjenlevendeForelderFnr(barnepensjon: Barnepensjon): String {
        val fnr =
            barnepensjon.foreldre.find { it.type === PersonType.GJENLEVENDE_FORELDER }?.foedselsnummer?.svar?.value
        if (fnr != null) {
            return fnr
        }
        throw Exception("Mangler fødselsnummer på gjenlevende forelder")
    }
}

fun <T> lagOpplysning(opplysningsType: Opplysningstyper, opplysning: T): Behandlingsopplysning<T> {
    return Behandlingsopplysning(
        UUID.randomUUID(),
        Behandlingsopplysning.Pdl("pdl", Instant.now(), null),
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}
