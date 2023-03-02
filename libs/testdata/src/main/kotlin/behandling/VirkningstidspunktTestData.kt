package no.nav.etterlatte.libs.testdata.behandling

import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth

class VirkningstidspunktTestData {

    companion object {
        fun virkningstidsunkt(dato: YearMonth = YearMonth.now()) =
            Virkningstidspunkt(
                dato,
                Grunnlagsopplysning.Saksbehandler("Z12345678", Tidspunkt.now().instant),
                "begrunnelse"
            )
    }
}