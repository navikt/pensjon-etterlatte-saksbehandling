package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.model.Avsender
import no.nav.pensjon.brevbaker.api.model.Bruker
import no.nav.pensjon.brevbaker.api.model.Felles
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.NAVEnhet
import no.nav.pensjon.brevbaker.api.model.SignerendeSaksbehandlere
import java.time.LocalDate

object BrevbakerHelpers {
    fun mapFelles(
        sakId: Long,
        soeker: Soeker,
        avsender: Avsender,
        vergeNavn: String? = null,
    ) = Felles(
        dokumentDato = LocalDate.now(),
        saksnummer = sakId.toString(),
        avsenderEnhet =
            NAVEnhet(
                nettside = "nav.no",
                navn = avsender.kontor,
                telefonnummer = avsender.telefonnummer,
            ),
        bruker =
            Bruker(
                fornavn = soeker.fornavn,
                mellomnavn = soeker.mellomnavn,
                etternavn = soeker.etternavn,
                foedselsnummer = Foedselsnummer(soeker.fnr.value),
            ),
        signerendeSaksbehandlere =
            SignerendeSaksbehandlere(
                saksbehandler = avsender.saksbehandler,
                attesterendeSaksbehandler = avsender.attestant,
            ),
        vergeNavn = vergeNavn,
    )
}
