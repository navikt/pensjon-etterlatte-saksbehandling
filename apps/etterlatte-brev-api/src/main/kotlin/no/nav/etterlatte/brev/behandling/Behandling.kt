package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.math.BigDecimal
import java.time.LocalDate

data class Behandling(
    val sakId: Long,
    val behandlingId: String,
    val persongalleri: Persongalleri,
    val vedtak: Vedtak,
    val grunnlag: Grunnlag,
    val utbetalingsinfo: Utbetalingsinfo? = null
) {
    init {
        if (vedtak.type == VedtakType.INNVILGELSE)
            requireNotNull(utbetalingsinfo) { "Utbetalingsinformasjon mangler på behandling (id=${vedtak.behandling.id}" }
    }
}

data class Utbetalingsinfo(
    val beloep: BigDecimal,
    val kontonummer: String,
    val virkningsdato: LocalDate,
    val grunnbeloep: Grunnbeloep
)
