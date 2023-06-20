package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.behandling.Saksrolle
import java.time.LocalDateTime
import java.util.*

data class Grunnlagsendringshendelse(
    val id: UUID,
    val sakId: Long,
    val type: GrunnlagsendringsType,
    val opprettet: LocalDateTime,
    val status: GrunnlagsendringStatus = GrunnlagsendringStatus.VENTER_PAA_JOBB,
    val behandlingId: UUID? = null,
    val hendelseGjelderRolle: Saksrolle,
    val gjelderPerson: String,
    val samsvarMellomKildeOgGrunnlag: SamsvarMellomKildeOgGrunnlag? = null,
    val kommentar: String? = null
)

enum class GrunnlagsendringsType {
    DOEDSFALL,
    UTFLYTTING,
    FORELDER_BARN_RELASJON,
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT,
    GRUNNBELOEP,
    INSTITUSJONSOPPHOLD,
    SIVILSTAND
}

enum class GrunnlagsendringStatus {
    VENTER_PAA_JOBB, // naar hendelsen registreres // FØR: IKKE_VURDERT
    SJEKKET_AV_JOBB, // FØR: MED_I_BEHANDLING
    TATT_MED_I_BEHANDLING, // tatt med i behandling av saksbehandler
    FORKASTET,
    VURDERT_SOM_IKKE_RELEVANT;

    companion object {
        fun relevantForSaksbehandler() = listOf(SJEKKET_AV_JOBB, TATT_MED_I_BEHANDLING, VURDERT_SOM_IKKE_RELEVANT)
    }
}