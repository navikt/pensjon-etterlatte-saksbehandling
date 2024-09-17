package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.common.Enhet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak

fun sak(
    id: Long,
    sakType: SakType,
    ident: String = "123",
    enhet: Enhet = Enhet.PORSGRUNN,
): Sak =
    Sak(
        ident = ident,
        sakType = sakType,
        id = id,
        enhet = enhet,
    )
