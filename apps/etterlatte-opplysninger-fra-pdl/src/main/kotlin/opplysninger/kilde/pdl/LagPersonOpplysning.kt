package no.nav.etterlatte.opplysninger.kilde.pdl

import no.nav.etterlatte.common.objectMapper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.time.Instant
import java.util.*

fun <T> lagPersonOpplysning(
    opplysningsType: Opplysningstyper,
    opplysning: OpplysningDTO<T>,
    fnr: Foedselsnummer,
    periode: Periode? = null
): Grunnlagsopplysning<T> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.Pdl(
            navn = "pdl",
            tidspunktForInnhenting = Instant.now(),
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