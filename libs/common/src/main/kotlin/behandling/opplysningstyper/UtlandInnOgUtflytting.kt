package no.nav.etterlatte.libs.common.behandling.opplysningstyper

import java.time.LocalDate

data class UtlandInnOgUtflytting(
    val harHattUtenlandsopphold: String,
    val innflytting: List<InnflyttingTilNorge>?,
    val utflytting: List<UtflyttingFraNorge>?,
    val foedselsnummer: String
)

data class InnflyttingTilNorge(
    val fraflyttingsland: String?,
    val dato: LocalDate?,
)

data class UtflyttingFraNorge(
    val tilflyttingsland: String?,
    val dato: LocalDate?,
)