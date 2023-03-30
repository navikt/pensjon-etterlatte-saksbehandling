package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import java.time.LocalDate

data class AvdoedSoeknad(
    val type: PersonType,
    val fornavn: String,
    val etternavn: String,
    val folkeregisteridentifikator: Folkeregisteridentifikator,
    val doedsdato: LocalDate,
    val statsborgerskap: String,
    val utenlandsopphold: Utenlandsopphold,
    val doedsaarsakSkyldesYrkesskadeEllerYrkessykdom: JaNeiVetIkke
)

data class Utenlandsopphold(
    val harHattUtenlandsopphold: JaNeiVetIkke,
    val opphold: List<UtenlandsoppholdOpplysninger>?
)

data class UtenlandsoppholdOpplysninger(
    val land: String,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
    val oppholdsType: List<OppholdUtlandType>,
    val medlemFolketrygd: JaNeiVetIkke,
    val pensjonsutbetaling: String?
)