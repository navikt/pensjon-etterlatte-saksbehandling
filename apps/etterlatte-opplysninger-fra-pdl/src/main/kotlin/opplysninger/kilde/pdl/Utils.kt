package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.Instant
import java.util.*

fun <T> lagPdlOpplysning(
    opplysningsType: Opplysningstype,
    opplysning: T,
    tidspunktForInnhenting: Instant
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        UUID.randomUUID(),
        Grunnlagsopplysning.Pdl("pdl", tidspunktForInnhenting, null, null),
        opplysningsType,
        objectMapper.createObjectNode(),
        opplysning
    )
}

fun <T> lagPdlPersonopplysning(
    tidspunktForInnhenting: Instant,
    opplysningsType: Opplysningstype,
    opplysning: OpplysningDTO<T>,
    fnr: Foedselsnummer,
    periode: Periode? = null
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.Pdl(
            navn = "pdl",
            tidspunktForInnhenting = tidspunktForInnhenting,
            registersReferanse = null,
            opplysningId = opplysning.opplysningsid.toString()
        ),
        opplysningType = opplysningsType,
        meta = objectMapper.valueToTree(opplysning),
        opplysning = opplysning.verdi,
        fnr = fnr,
        periode = periode
    )
}