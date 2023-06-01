package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.pensjon.brevbaker.api.model.*
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
            foedselsnummer = Foedselsnummer(mottaker.foedselsnummer!!.value)
        ),
        signerendeSaksbehandlere = SignerendeSaksbehandlere(
            saksbehandler = avsender.saksbehandler,
            attesterendeSaksbehandler = avsender.attestant
        ),
        vergeNavn = null
    )
}