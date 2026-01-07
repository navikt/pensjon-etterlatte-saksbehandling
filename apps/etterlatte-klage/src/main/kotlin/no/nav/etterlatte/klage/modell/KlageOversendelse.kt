package no.nav.etterlatte.klage.modell

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.EkstradataInnstilling
import no.nav.etterlatte.libs.common.behandling.KabalHjemmel
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageMottaker
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.klage.kodeverk.Fagsystem
import no.nav.klage.kodeverk.hjemmel.Hjemmel
import no.nav.klage.kodeverk.ytelse.Ytelse
import java.time.LocalDate

// vi lytter kun på KLAGE saker, men må legge inn andre typer så kafka ikke tryner
enum class KabalSakType {
    KLAGE,
    ANKE,
    ANKE_I_TRYGDERETTEN,
    OMGJOERINGSKRAV,
    BEHANDLING_ETTER_TRYGDERETTEN_OPPHEVET,
    BEGJAERING_OM_GJENOPPTAK,
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

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KabalKlager(
    val id: KabalKlagerPart,
    val klagersProsessfullmektig: KlageAnnenPart?,
)

data class KabalFagsak(
    val fagsakId: String,
    val fagsystem: Fagsystem,
)

fun SakType.tilYtelse(): Ytelse =
    when (this) {
        SakType.BARNEPENSJON -> Ytelse.PEN_BAR
        SakType.OMSTILLINGSSTOENAD -> Ytelse.PEN_EYO
    }

fun KabalHjemmel.tilHjemmel(): Hjemmel = enumValueOf(this.name)

/**
 * Har det subsettet av datamodellen for ovesendelse til Kabal som vi kommer til å bruke
 *
 * Se https://kabal-api.intern.dev.nav.no/swagger-ui/index.html#/kabal-api-external/sendInnSakV3
 *
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
data class KabalOversendelse(
    val type: KabalSakType,
    val klager: KabalKlager,
    val sakenGjelder: KlageAnnenPart?,
    val fagsak: KabalFagsak,
    val kildeReferanse: String,
    val hjemler: List<Hjemmel>,
    val forrigeBehandlendeEnhet: Enhetsnummer,
    val tilknyttedeJournalposter: List<KabalJournalpostref>,
    val brukersHenvendelseMottattNavDato: LocalDate,
    val innsendtTilNav: LocalDate,
    val kilde: Fagsystem,
    val ytelse: Ytelse,
    val kommentar: String?,
) {
    companion object {
        private val fagsystem = Fagsystem.EY

        fun fra(
            klage: Klage,
            ekstraData: EkstradataInnstilling,
        ): KabalOversendelse {
            val innstilling =
                when (val utfall = klage.utfall) {
                    is KlageUtfallMedData.DelvisOmgjoering -> utfall.innstilling
                    is KlageUtfallMedData.StadfesteVedtak -> utfall.innstilling
                    else -> throw IllegalArgumentException("Kan ikke sende noe som ikke er innstilling til Kabal")
                }

            val erKlagerIkkeBruker = ekstraData.mottakerInnstilling.foedselsnummer?.value != klage.sak.ident

            /*
             * Se på mappingen av klager / sakenGjelder opp mot data vi får med fra den innkommende klagen
             */
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
                fagsak = KabalFagsak(fagsakId = klage.sak.id.toString(), fagsystem = fagsystem),
                kildeReferanse = klage.id.toString(),
                hjemler = listOf(innstilling.lovhjemmel.tilHjemmel()),
                forrigeBehandlendeEnhet = klage.sak.enhet,
                tilknyttedeJournalposter =
                    listOfNotNull(
                        KabalJournalpostref(
                            type = KabalJournalpostType.OVERSENDELSESBREV,
                            journalpostId = ekstraData.journalpostInnstillingsbrev,
                        ),
                        ekstraData.journalpostKlage?.let {
                            KabalJournalpostref(
                                type = KabalJournalpostType.BRUKERS_KLAGE,
                                journalpostId = it,
                            )
                        } ?: klage.innkommendeDokument?.journalpostId?.let {
                            KabalJournalpostref(type = KabalJournalpostType.BRUKERS_KLAGE, journalpostId = it)
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
                brukersHenvendelseMottattNavDato = klage.opprettet.toLocalDate(),
                innsendtTilNav = klage.innkommendeDokument?.mottattDato ?: klage.opprettet.toLocalDate(),
                kilde = fagsystem,
                ytelse = klage.sak.sakType.tilYtelse(),
                kommentar = innstilling.internKommentar,
            )
        }
    }
}

fun KlageMottaker.tilKlagerPart(): KabalKlagerPart {
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
    throw IllegalArgumentException(
        "Kan ikke mappe en mottaker som ikke har foedselsnummer og " +
            "orgnummer. Fikk navn på mottaker=<$navn>",
    )
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
