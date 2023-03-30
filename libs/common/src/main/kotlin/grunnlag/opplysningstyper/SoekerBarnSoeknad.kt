package no.nav.etterlatte.libs.common.grunnlag.opplysningstyper

import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.OmsorgspersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType

data class SoekerBarnSoeknad(
    val type: PersonType,
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator,
    val statsborgerskap: String,
    val utenlandsadresse: UtenlandsadresseBarn,
    val foreldre: List<Forelder>,
    val verge: Verge,
    val omsorgPerson: OmsorgspersonType?
)

data class UtenlandsadresseBarn(
    val adresseIUtlandet: JaNeiVetIkke?,
    val land: String?,
    val adresse: String?
)

data class Forelder(
    val type: PersonType,
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Folkeregisteridentifikator
)

data class Verge(
    val barnHarVerge: JaNeiVetIkke?,
    val fornavn: String?,
    val etternavn: String?,
    val foedselsnummer: Folkeregisteridentifikator?
)