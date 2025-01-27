package no.nav.etterlatte.opplysningerfrasoknad.opplysninger

import no.nav.etterlatte.libs.common.innsendtsoeknad.OmsorgspersonType
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.time.LocalDate

data class SoekerBarnSoeknad(
    val type: PersonType,
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator?,
    val foedselsdato: LocalDate?,
    val statsborgerskap: String,
    val utenlandsadresse: UtenlandsadresseBarn,
    val foreldre: List<Forelder>,
    val verge: Verge,
    val omsorgPerson: OmsorgspersonType?,
)

data class UtenlandsadresseBarn(
    val adresseIUtlandet: JaNeiVetIkke?,
    val land: String?,
    val adresse: String?,
)

data class Forelder(
    val type: PersonType,
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator?,
    val foedselsdato: LocalDate?,
)

data class Verge(
    val barnHarVerge: JaNeiVetIkke?,
    val fornavn: String?,
    val etternavn: String?,
    val foedselsnummer: Folkeregisteridentifikator?,
)
