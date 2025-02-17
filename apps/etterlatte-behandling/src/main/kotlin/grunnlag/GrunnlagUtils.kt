package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.Saksbehandler

object GrunnlagUtils {
    fun opplysningsbehov(
        sak: Sak,
        persongalleri: Persongalleri,
    ): Opplysningsbehov {
        val brukerTokenInfo = Kontekst.get().brukerTokenInfo!!

        return Opplysningsbehov(
            sakId = sak.id,
            sakType = sak.sakType,
            persongalleri = persongalleri,
            kilde =
                when (brukerTokenInfo) {
                    is Saksbehandler -> Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now())
                    else -> Grunnlagsopplysning.Gjenny(brukerTokenInfo.ident(), Tidspunkt.now())
                },
        )
    }
}
