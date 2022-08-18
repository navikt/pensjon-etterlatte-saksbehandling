package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import java.time.Instant
import java.util.*

class OpplysningsByggerService : OpplysningsBygger {

    override fun byggOpplysninger(barnepensjon: Barnepensjon, pdl: Pdl): List<Grunnlagsopplysning<out Any>> {
        val soekersFnr = barnepensjon.soeker.foedselsnummer.svar.value
        val avdoedFnr = hentAvdoedFnr(barnepensjon)
        val gjenlevendeForelderFnr = hentGjenlevendeForelderFnr(barnepensjon)

        val soekerPdl = pdl.hentPdlModell(soekersFnr, PersonRolle.BARN)
        val avdoedPdl = pdl.hentPdlModell(avdoedFnr, PersonRolle.AVDOED)
        val gjenlevendeForelderPdl = pdl.hentPdlModell(gjenlevendeForelderFnr, PersonRolle.GJENLEVENDE)

        return listOf(
            personOpplysning(avdoedPdl, Opplysningstyper.AVDOED_PDL_V1),
            personOpplysning(gjenlevendeForelderPdl, Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1),
            personOpplysning(soekerPdl, Opplysningstyper.SOEKER_PDL_V1)
        )
    }

    fun personOpplysning(
        personPdl: Person,
        opplysningsType: Opplysningstyper
    ): Grunnlagsopplysning<Person> {
        return lagOpplysning(opplysningsType, personPdl)
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

fun <T> lagOpplysning(opplysningsType: Opplysningstyper, opplysning: T): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        UUID.randomUUID(),
        Grunnlagsopplysning.Pdl("pdl", Instant.now(), null),
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}