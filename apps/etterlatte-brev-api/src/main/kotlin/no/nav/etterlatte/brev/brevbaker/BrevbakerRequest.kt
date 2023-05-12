package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.model.Attestant
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.pensjon.brev.api.model.Bruker
import no.nav.pensjon.brev.api.model.Felles
import no.nav.pensjon.brev.api.model.Foedselsnummer
import no.nav.pensjon.brev.api.model.NAVEnhet
import no.nav.pensjon.brev.api.model.SignerendeSaksbehandlere
import java.time.LocalDate

data class BrevbakerRequest(
    val kode: EtterlatteBrevKode,
    val letterData: Any,
    val felles: Felles,
    val language: LanguageCode
) {
    companion object {
        fun fra(
            behandling: Behandling,
            avsender: Avsender,
            mottaker: Mottaker,
            attestant: Attestant?
        ): BrevbakerRequest {
            return BrevbakerRequest(
                kode = EtterlatteBrevKode.BARNEPENSJON_VEDTAK, // TODO: Sett opp støtte for OMS
                letterData = BrevDataMapper.fra(behandling, avsender, mottaker, attestant),
                felles = Felles(
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
                        foedselsnummer = Foedselsnummer(mottaker.foedselsnummer!!.value),
                        foedselsdato = LocalDate.now() // Blir ikke brukt, men kan ikke være null
                    ),
                    signerendeSaksbehandlere = SignerendeSaksbehandlere(
                        saksbehandler = avsender.saksbehandler,
                        attesterendeSaksbehandler = attestant?.navn
                    ),
                    vergeNavn = null
                ),
                language = LanguageCode.createLanguageCode(behandling.spraak.verdi)
            )
        }
    }
}

enum class LanguageCode {
    BOKMAL, NYNORSK, ENGLISH;

    companion object {
        fun createLanguageCode(spraak: String): LanguageCode {
            return when (spraak.uppercase()) {
                ("EN") -> ENGLISH
                ("NB") -> BOKMAL
                ("NN") -> NYNORSK
                else -> standardLanguage()
            }
        }

        fun standardLanguage() = BOKMAL
    }
}

enum class EtterlatteBrevKode { A_LETTER, BARNEPENSJON_VEDTAK }