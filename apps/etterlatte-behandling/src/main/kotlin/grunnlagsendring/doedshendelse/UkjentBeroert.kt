package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent
import no.nav.etterlatte.libs.common.person.Sivilstand

data class UkjentBeroert(
    val avdoedFnr: String,
    val barnUtenIdent: List<PersonUtenIdent>,
    val ektefellerUtenIdent: List<Sivilstand>,
)
