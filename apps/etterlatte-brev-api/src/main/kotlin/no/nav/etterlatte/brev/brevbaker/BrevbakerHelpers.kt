package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.adresse.Avsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.pensjon.brevbaker.api.model.BrevbakerFelles
import no.nav.pensjon.brevbaker.api.model.BrevbakerFelles.Bruker
import no.nav.pensjon.brevbaker.api.model.BrevbakerFelles.NavEnhet
import no.nav.pensjon.brevbaker.api.model.BrevbakerFelles.SignerendeSaksbehandlere
import no.nav.pensjon.brevbaker.api.model.BrevbakerType.Foedselsnummer
import java.time.LocalDate

object BrevbakerHelpers {
    fun mapFelles(
        sakId: SakId,
        soeker: Soeker,
        avsender: Avsender,
        annenMottakerNavn: String? = null,
    ) = BrevbakerFelles(
        dokumentDato = LocalDate.now(),
        saksnummer = sakId.toString(),
        avsenderEnhet =
            NavEnhet(
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
            avsender.saksbehandler?.let {
                SignerendeSaksbehandlere(
                    saksbehandler = avsender.saksbehandler,
                    attesterendeSaksbehandler = avsender.attestant,
                )
            },
        annenMottakerNavn = annenMottakerNavn,
    )
}
