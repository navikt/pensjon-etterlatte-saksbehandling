package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import java.util.UUID

fun lagEnkelopplysningerFraPDL(
    person: Person,
    personDTO: PersonDTO,
    opplysningstype: Opplysningstype,
    fnr: Folkeregisteridentifikator,
    personRolle: PersonRolle,
): List<Grunnlagsopplysning<JsonNode>> {
    val tidspunktForInnhenting = Tidspunkt.now()
    val opplysningsbolk = Opplysningsbolk(fnr, tidspunktForInnhenting)

    val opplysningNavn =
        OpplysningDTO(
            Navn(personDTO.fornavn.verdi, personDTO.mellomnavn?.verdi, personDTO.etternavn.verdi),
            personDTO.fornavn.opplysningsid,
        )

    opplysningsbolk.apply {
        leggTilOpplysning(Opplysningstype.NAVN, opplysningNavn)
        leggTilOpplysninger(Opplysningstype.DELTBOSTEDSADRESSE, personDTO.deltBostedsadresse)
        leggTilOpplysninger(Opplysningstype.KONTAKTADRESSE, personDTO.kontaktadresse)
        leggTilOpplysninger(Opplysningstype.OPPHOLDSADRESSE, personDTO.oppholdsadresse)
        leggTilOpplysninger(Opplysningstype.VERGEMAALELLERFREMTIDSFULLMAKT, personDTO.vergemaalEllerFremtidsfullmakt)
        leggTilOpplysning(Opplysningstype.FOEDSELSDATO, personDTO.foedselsdato)
        leggTilOpplysning(Opplysningstype.FOEDELAND, personDTO.foedeland)
        leggTilOpplysning(Opplysningstype.DOEDSDATO, personDTO.doedsdato)
        leggTilOpplysning(Opplysningstype.SIVILSTATUS, personDTO.sivilstatus)
        leggTilOpplysninger(Opplysningstype.SIVILSTAND, personDTO.sivilstand)
        leggTilOpplysning(Opplysningstype.STATSBORGERSKAP, personDTO.statsborgerskap)
        leggTilOpplysning(Opplysningstype.UTLAND, personDTO.utland)
        leggTilOpplysning(Opplysningstype.FAMILIERELASJON, personDTO.familieRelasjon)
        leggTilOpplysning(Opplysningstype.FOEDSELSNUMMER, personDTO.foedselsnummer)
        leggTilOpplysning(Opplysningstype.FOEDSELSAAR, personDTO.foedselsaar)
        leggTilOpplysning(Opplysningstype.PERSONROLLE, OpplysningDTO(personRolle, null))
        personDTO.avdoedesBarn?.let { leggTilOpplysning(Opplysningstype.AVDOEDESBARN, OpplysningDTO(it, null)) }
        leggTilOpplysninger(Opplysningstype.BOSTEDSADRESSE, personDTO.bostedsadresse)
    }

    val gammalGrunnlagsopplysning = lagPdlOpplysning(opplysningstype, person, tidspunktForInnhenting)

    return opplysningsbolk.hentOpplysninger() + gammalGrunnlagsopplysning
}

class Opplysningsbolk(private val fnr: Folkeregisteridentifikator, private val innhentetTidspunkt: Tidspunkt) {
    private val opplysninger = mutableListOf<Grunnlagsopplysning<JsonNode>>()

    fun leggTilOpplysninger(
        opplysningstype: Opplysningstype,
        grunnlagsopplysning: List<OpplysningDTO<out Any>>?,
    ) {
        if (grunnlagsopplysning.isNullOrEmpty()) {
            return
        }
        val opplysningSamlet =
            OpplysningDTO(
                grunnlagsopplysning.map { it.verdi },
                opplysningsid = grunnlagsopplysning.firstOrNull()?.opplysningsid,
            )
        leggTilOpplysning(opplysningstype, opplysningSamlet)
    }

    fun leggTilOpplysning(
        opplysningstype: Opplysningstype,
        opplysningDTO: OpplysningDTO<out Any>?,
        periode: Periode? = null,
    ) {
        opplysningDTO?.let {
            opplysninger.add(
                lagPdlPersonopplysning(
                    tidspunktForInnhenting = innhentetTidspunkt,
                    opplysningsType = opplysningstype,
                    opplysning = opplysningDTO,
                    fnr = fnr,
                    periode = periode,
                ),
            )
        }
    }

    fun hentOpplysninger() = opplysninger.toList()
}

fun lagPdlOpplysning(
    opplysningsType: Opplysningstype,
    opplysning: Person,
    tidspunktForInnhenting: Tidspunkt,
): Grunnlagsopplysning<JsonNode> {
    return Grunnlagsopplysning(
        UUID.randomUUID(),
        Grunnlagsopplysning.Pdl(tidspunktForInnhenting, null, null),
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning.toJsonNode(),
    )
}

fun <T> lagPdlPersonopplysning(
    tidspunktForInnhenting: Tidspunkt,
    opplysningsType: Opplysningstype,
    opplysning: OpplysningDTO<T>,
    fnr: Folkeregisteridentifikator,
    periode: Periode? = null,
): Grunnlagsopplysning<JsonNode> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde =
            Grunnlagsopplysning.Pdl(
                tidspunktForInnhenting = tidspunktForInnhenting,
                registersReferanse = null,
                opplysningId = opplysning.opplysningsid.toString(),
            ),
        opplysningType = opplysningsType,
        meta = objectMapper.valueToTree(opplysning),
        opplysning = opplysning.verdi!!.toJsonNode(),
        fnr = fnr,
        periode = periode,
    )
}
