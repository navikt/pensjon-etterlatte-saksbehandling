package no.nav.etterlatte.beregning

import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.*
import javax.sql.DataSource

class AvkortingRepository(private val dataSource: DataSource) {

    fun hentAvkorting(behandlingId: UUID): Avkorting {
        return Avkorting(
            behandlingId = behandlingId,
            avkortingGrunnlag = listOf(
                AvkortingGrunnlag(
                    periode = Periode(fom = YearMonth.now(), tom = null),
                    aarsInntekt = 500000,
                    gjeldendeAar = 2023,
                    spesifikasjon = "Arbeidslønn"
                )
            ),
            beregningEtterAvkorting = listOf(),
            tidspunktForAvkorting = Tidspunkt.now()
        )
    }

    fun lagreAvkortingGrunnlag(behandlingId: UUID, avkortingGrunnlag: AvkortingGrunnlag): Avkorting {
        return Avkorting(
            behandlingId = behandlingId,
            avkortingGrunnlag = listOf(avkortingGrunnlag),
            beregningEtterAvkorting = listOf(),
            tidspunktForAvkorting = Tidspunkt.now()
        )
    }
}