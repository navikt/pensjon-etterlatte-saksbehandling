package grunnlagsendring.doedshendelse

import no.nav.etterlatte.grunnlagsendring.doedshendelse.PersonFnrMedRelasjon
import no.nav.etterlatte.libs.common.behandling.PersonUtenIdent

data class BeroerteVedDoedshendelse(
    val beroerteMedFnr: List<PersonFnrMedRelasjon>,
    val beroerteBarnUtenIdent: List<PersonUtenIdent>,
    val beroerteEktefellerUtenIdent: List<PersonUtenIdent>,
) {
    fun size(): Int = beroerteMedFnr.size + beroerteBarnUtenIdent.size + beroerteEktefellerUtenIdent.size
}
