package no.nav.etterlatte.behandling.forlengetoms

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID

data class ForlengetOmsVurdering(
    val behandlingId: UUID,
    val aktivitet: ForlengetOmsAktivitet,
    val begrunnelse: String,
    val utfall: ForlengetOmsUtfall,
    val opprettet: Grunnlagsopplysning.Saksbehandler,
    val endret: Grunnlagsopplysning.Saksbehandler,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class ForlengetOmsAktivitet {
    @JsonTypeName("UTDANNING")
    data class Utdanning(
        val skole: String,
        val studium: String,
        val studererMinst50Prosent: JaNei,
    ) : ForlengetOmsAktivitet()

    @JsonTypeName("ARBEIDSRETTET_TILTAK")
    data class ArbeidsrettetTiltak(
        val tiltak: String,
        val omfang: Tiltaksomfang,
        val aktivitetsplan: JaNei,
    ) : ForlengetOmsAktivitet()

    @JsonTypeName("NEI")
    data object IngenAktivitet : ForlengetOmsAktivitet()
}

enum class Tiltaksomfang {
    HELTID,
    DELTID,
}

enum class ForlengetOmsUtfall {
    INNVILGELSE,
    AVSLAG,
}
