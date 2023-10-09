package no.nav.etterlatte.klage.modell

import no.nav.etterlatte.libs.common.behandling.EkstradataInnstilling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.Mottaker
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDate

enum class KabalSakType {
    KLAGE,
    ANKE,
}

enum class KabalKlagerType {
    PERSON,
    VIRKSOMHET,
}

data class KabalKlagerPart(
    val type: KabalKlagerType,
    val verdi: String,
)

data class KlageAnnenPart(
    val id: KabalKlagerPart,
    val skalMottaKopi: Boolean,
)

data class KabalKlager(
    val id: KabalKlagerPart,
    val klagersProsessfullmektig: KlageAnnenPart?,
)

data class KabalFagsak(
    val fagsakId: String,
    val fagsystem: String,
)

enum class KabalYtelse {
    PEN_GJE,
    PEN_BAR,
    ;

    companion object {
        fun fra(sakType: SakType): KabalYtelse {
            return when (sakType) {
                SakType.BARNEPENSJON -> PEN_BAR
                SakType.OMSTILLINGSSTOENAD -> PEN_GJE
            }
        }
    }
}

/**
 * Har det subsettet av datamodellen for ovesendelse til Kabal som vi kommer til å bruke
 *
 * Se https://kabal-api.intern.dev.nav.no/swagger-ui/index.html#/kabal-api-external/sendInnSakV3
 *
 **/
data class KabalOversendelse(
    val type: KabalSakType,
    val klager: KabalKlager,
    val sakenGjelder: KlageAnnenPart?,
    val fagsak: KabalFagsak,
    val kildeReferanse: String,
    val hjemler: List<KabalHjemmel>,
    val forrigeBehandlendeEnhet: String,
    val tilknyttedeJournalposter: List<KabalJournalpostref>,
    val brukersHenvendelseMottattNavDato: LocalDate,
    val innsendtTilNav: LocalDate,
    val kilde: String,
    val ytelse: KabalYtelse,
    val kommentar: String?,
) {
    companion object {
        private const val FAGSYSTEM: String = "EY"

        fun fra(
            klage: Klage,
            ekstraData: EkstradataInnstilling,
        ): KabalOversendelse {
            val innstilling =
                when (val utfall = klage.utfall) {
                    is KlageUtfall.DelvisOmgjoering -> utfall.innstilling
                    is KlageUtfall.StadfesteVedtak -> utfall.innstilling
                    else -> throw IllegalArgumentException("Kan ikke sende noe som ikke er innstilling til Kabal")
                }

            val erKlagerIkkeBruker = ekstraData.mottakerInnstilling.foedselsnummer?.value != klage.sak.ident

            return KabalOversendelse(
                type = KabalSakType.KLAGE,
                klager =
                    KabalKlager(
                        id = ekstraData.mottakerInnstilling.tilKlagerPart(),
                        klagersProsessfullmektig =
                            ekstraData.vergeEllerFullmektig?.motpartsPersonident?.let {
                                KlageAnnenPart(
                                    id =
                                        KabalKlagerPart(
                                            type = KabalKlagerType.PERSON,
                                            verdi = it.value,
                                        ),
                                    skalMottaKopi = true,
                                )
                            },
                    ),
                sakenGjelder =
                    KlageAnnenPart(
                        id = KabalKlagerPart(KabalKlagerType.PERSON, klage.sak.ident),
                        skalMottaKopi = true,
                    ).takeIf { erKlagerIkkeBruker },
                fagsak = KabalFagsak(fagsakId = klage.sak.id.toString(), fagsystem = FAGSYSTEM),
                kildeReferanse = klage.id.toString(),
                hjemler = listOf(KabalHjemmel.FTRL_21_12), // TODO hent fra innstilling når hjemlene våre er på plass
                forrigeBehandlendeEnhet = klage.sak.enhet,
                tilknyttedeJournalposter =
                    listOfNotNull(
//                        KabalJournalpostref(
//                            type = KabalJournalpostType.OVERSENDELSESBREV,
//                            journalpostId = ekstraData.journalpostInnstillingsbrev,
//                        ),
                        ekstraData.journalpostKlage?.let {
                            KabalJournalpostref(
                                type = KabalJournalpostType.BRUKERS_KLAGE,
                                journalpostId = it,
                            )
                        },
                        ekstraData.journalpostSoeknad?.let {
                            KabalJournalpostref(
                                type = KabalJournalpostType.BRUKERS_SOEKNAD,
                                journalpostId = it,
                            )
                        },
                        ekstraData.journalpostVedtak?.let {
                            KabalJournalpostref(
                                type = KabalJournalpostType.OPPRINNELIG_VEDTAK,
                                journalpostId = it,
                            )
                        },
                    ),
                brukersHenvendelseMottattNavDato = Tidspunkt.now().toLocalDate(),
                innsendtTilNav = Tidspunkt.now().toLocalDate(),
                kilde = FAGSYSTEM,
                ytelse = KabalYtelse.fra(klage.sak.sakType),
                kommentar = innstilling.tekst, // TODO rename her og i basen
            )
        }
    }
}

fun Mottaker.tilKlagerPart(): KabalKlagerPart {
    val fnr = this.foedselsnummer
    if (fnr != null) {
        return KabalKlagerPart(
            type = KabalKlagerType.PERSON,
            verdi = fnr.value,
        )
    }
    val orgnr = this.orgnummer
    if (orgnr != null) {
        return KabalKlagerPart(
            type = KabalKlagerType.VIRKSOMHET,
            verdi = orgnr,
        )
    }
    throw IllegalArgumentException("Kan ikke mappe en mottaker som ikke har foedselsnummer og orgnummer!")
}

enum class KabalJournalpostType {
    BRUKERS_SOEKNAD,
    OPPRINNELIG_VEDTAK,
    BRUKERS_KLAGE,
    BRUKERS_ANKE,
    OVERSENDELSESBREV,
    KLAGE_VEDTAK,
    ANNET,
}

data class KabalJournalpostref(
    val type: KabalJournalpostType,
    val journalpostId: String,
)
