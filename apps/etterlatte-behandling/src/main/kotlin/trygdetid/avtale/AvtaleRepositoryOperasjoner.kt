package no.nav.etterlatte.trygdetid.avtale

import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import java.util.UUID

interface AvtaleRepositoryOperasjoner {
    fun hentAvtale(behandlingId: UUID): Trygdeavtale?

    fun lagreAvtale(trygdeavtale: Trygdeavtale)

    fun opprettAvtale(trygdeavtale: Trygdeavtale)
}
