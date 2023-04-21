package no.nav.etterlatte.beregning

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.*
import javax.sql.DataSource

class AvkortingRepository(private val dataSource: DataSource) {

    fun hentAvkorting(behandlingId: UUID): Avkorting {
        return Avkorting(
            behandlingId = behandlingId,
            avkortingGrunnlag = listOf(),
            beregningEtterAvkorting = listOf(),
            tidspunktForAvkorting = Tidspunkt.now()
        )
    }
}