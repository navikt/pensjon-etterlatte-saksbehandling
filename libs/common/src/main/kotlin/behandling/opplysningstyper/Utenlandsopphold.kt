package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OppholdUtlandType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Svar
import java.time.LocalDate

data class Utenlandsopphold(
    val harHattUtenlandsopphold: Svar,
    val opphold: List<UtenlandsoppholdOpplysninger>?,
    val fnr: String
)

data class UtenlandsoppholdOpplysninger(
    val land: String,
    val fraDato: LocalDate?,
    val tilDato: LocalDate?,
    val oppholdsType: List<OppholdUtlandType>,
    val medlemFolketrygd: Svar,
    val pensjonsutbetaling: String?
)