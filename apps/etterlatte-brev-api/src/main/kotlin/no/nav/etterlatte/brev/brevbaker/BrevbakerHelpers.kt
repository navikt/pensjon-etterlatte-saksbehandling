package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.pensjon.brevbaker.api.model.Bruker
import no.nav.pensjon.brevbaker.api.model.Felles
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.NAVEnhet
import no.nav.pensjon.brevbaker.api.model.SignerendeSaksbehandlere
import java.time.LocalDate

object BrevbakerHelpers {
    fun mapFelles(
        behandling: Behandling,
        avsender: Avsender,
        mottaker: Mottaker
    ) = Felles(
        dokumentDato = LocalDate.now(),
        saksnummer = behandling.sakId.toString(),
        avsenderEnhet = NAVEnhet(
            nettside = "nav.no",
            navn = avsender.kontor,
            telefonnummer = avsender.telefonnummer
        ),
        bruker = Bruker(
            fornavn = behandling.persongalleri.soeker.fornavn,
            mellomnavn = behandling.persongalleri.soeker.mellomnavn,
            etternavn = behandling.persongalleri.soeker.etternavn,
            foedselsnummer = Foedselsnummer(behandling.persongalleri.soeker.fnr.value)
        ),
        signerendeSaksbehandlere = SignerendeSaksbehandlere(
            saksbehandler = avsender.saksbehandler,
            attesterendeSaksbehandler = avsender.attestant
        ),
        vergeNavn = null
    )
}