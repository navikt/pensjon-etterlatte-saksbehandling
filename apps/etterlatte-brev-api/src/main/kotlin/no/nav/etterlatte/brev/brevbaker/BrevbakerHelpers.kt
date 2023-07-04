package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.pensjon.brevbaker.api.model.Bruker
import no.nav.pensjon.brevbaker.api.model.Felles
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import no.nav.pensjon.brevbaker.api.model.NAVEnhet
import no.nav.pensjon.brevbaker.api.model.SignerendeSaksbehandlere
import java.time.LocalDate

object BrevbakerHelpers {

    fun hentBrevkode(sakType: SakType, vedtakType: VedtakType): EtterlatteBrevKode {
        return when (sakType) {
            SakType.OMSTILLINGSSTOENAD -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE -> EtterlatteBrevKode.OMS_INNVILGELSE_MANUELL
                    else -> TODO("Finnes ingen brevkode for saktype=$sakType og vedtakType=$vedtakType")
                }
            }

            SakType.BARNEPENSJON -> {
                when (vedtakType) {
                    VedtakType.INNVILGELSE -> EtterlatteBrevKode.BARNEPENSJON_INNVILGELSE
                    else -> TODO("Finnes ingen brevkode for saktype=$sakType og vedtakType=$vedtakType")
                }
            }
        }
    }

    fun mapFelles(
        sakId: Long,
        soeker: Soeker,
        avsender: Avsender,
        vergeNavn: String? = null
    ) = Felles(
        dokumentDato = LocalDate.now(),
        saksnummer = sakId.toString(),
        avsenderEnhet = NAVEnhet(
            nettside = "nav.no",
            navn = avsender.kontor,
            telefonnummer = avsender.telefonnummer
        ),
        bruker = Bruker(
            fornavn = soeker.fornavn,
            mellomnavn = soeker.mellomnavn,
            etternavn = soeker.etternavn,
            foedselsnummer = Foedselsnummer(soeker.fnr.value)
        ),
        signerendeSaksbehandlere = SignerendeSaksbehandlere(
            saksbehandler = avsender.saksbehandler,
            attesterendeSaksbehandler = avsender.attestant
        ),
        vergeNavn = vergeNavn
    )
}