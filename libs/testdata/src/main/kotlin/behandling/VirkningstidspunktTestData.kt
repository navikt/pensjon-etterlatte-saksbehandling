package no.nav.etterlatte.libs.testdata.behandling

import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.YearMonth

class VirkningstidspunktTestData {

    companion object {
        fun virkningstidsunkt(dato: YearMonth = YearMonth.now()) =
            Virkningstidspunkt(dato, Grunnlagsopplysning.Saksbehandler.create("Z12345678"), "begrunnelse")
    }
}