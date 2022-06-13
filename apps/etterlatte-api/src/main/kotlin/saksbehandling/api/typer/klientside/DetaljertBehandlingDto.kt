package no.nav.etterlatte.saksbehandling.api.typer.klientside

import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandlingDto(
    val id: UUID,
    val sak: Long,
    val gyldighetsprøving: GyldighetsResultat?,
    val vilkårsprøving: VilkaarResultat?,
    val kommerSoekerTilgode: KommerSoekerTilgode?,
    val beregning: BeregningsResultat?,
    val fastsatt: Boolean = false,
    val soeknadMottattDato: LocalDateTime?,
    val virkningstidspunkt: LocalDate?
)