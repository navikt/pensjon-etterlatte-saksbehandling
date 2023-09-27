package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Tenkt flyt mellom statuser i Gjenny:
 *
 *                      ┌┬────────────────┐           ┌┬───────────────┐
 *                 ┌────►│FORMKRAV_OPPFYLT├───────────►│UTFALL_VURDERT ├─┐
 *                 │    └┴────────────────┘           └┴───────────────┘ │
 *  ┌┬──────────┐  │                                                     │  ┌┬─────────────┐
 *  ││OPPRETTET ├──┤    ┌┬─────────────────────┐                        ┌┴──►│FERDIGSTILT  │
 *  └┴──────────┘  └────►│FORMKRAV_IKKE_OPPFYLT├────────────────────────┘   └┴─────────────┘
 *                      └┴─────────────────────┘
 *
 */
enum class KlageStatus {
    OPPRETTET, // klagen er opprettet i gjenny
    FORMKRAV_OPPFYLT, //
    FORMKRAV_IKKE_OPPFYLT,
    UTFALL_VURDERT,

    // potensielt en status for å markere oversendingen til Kabal
    FERDIGSTILT, // klagen er ferdig fra gjenny sin side

    ;

    companion object {
        fun kanOppdatereFormkrav(status: KlageStatus): Boolean {
            return status !== FERDIGSTILT
        }

        fun kanOppdatereUtfall(status: KlageStatus): Boolean {
            return status === FORMKRAV_OPPFYLT || status === UTFALL_VURDERT
        }
    }
}

// Placeholder til vi vet mer om hvilken flyt vi har her
enum class KabalStatus {
    OPPRETTET,
    UTREDES,
    VENTER,
    FERDIGSTILT,
}

enum class BehandlingResultat {
    MEDHOLD,
    IKKE_MEDHOLD,
    IKKE_MEDHOLD_FORMKRAV_AVVIST,
    IKKE_SATT,
    HENLAGT,
}

data class Kabalrespons(
    val kabalStatus: KabalStatus,
    val resultat: BehandlingResultat,
)

data class SendtInnstillingsbrev(
    val journalfoerTidspunkt: Tidspunkt,
    val sendtKabalTidspunkt: Tidspunkt,
    val brevId: Long,
    val journalpostId: String,
)

data class KlageResultat(
    val opprettetOppgaveOmgjoeringId: UUID?,
    val sendtInnstillingsbrev: SendtInnstillingsbrev?,
)

data class Klage(
    val id: UUID,
    val sak: Sak,
    val opprettet: Tidspunkt,
    val status: KlageStatus,
    val kabalStatus: KabalStatus?,
    val formkrav: FormkravMedBeslutter?,
    val utfall: KlageUtfall?,
    val resultat: KlageResultat?,
) {
    fun oppdaterFormkrav(
        formkrav: Formkrav,
        saksbehandlerIdent: String,
    ): Klage {
        if (!this.kanOppdatereFormkrav()) {
            throw IllegalStateException(
                "Kan ikke oppdatere formkrav i klagen med id=${this.id}, på grunn av " +
                    "tilstanden til klagen: ${this.status}",
            )
        }
        return this.copy(
            formkrav =
                FormkravMedBeslutter(
                    formkrav = formkrav,
                    saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandlerIdent),
                ),
            status =
                when (formkrav.erFormkraveneOppfylt) {
                    JaNei.JA -> KlageStatus.FORMKRAV_OPPFYLT
                    JaNei.NEI -> KlageStatus.FORMKRAV_IKKE_OPPFYLT
                },
        )
    }

    fun oppdaterUtfall(utfallMedBrev: KlageUtfall): Klage {
        if (!this.kanOppdatereUtfall()) {
            throw IllegalStateException(
                "Kan ikke oppdatere utfallet i klagen med id=${this.id} på grunn av statusen" +
                    "til klagen (${this.status})",
            )
        }
        return this.copy(
            utfall = utfallMedBrev,
            status = KlageStatus.UTFALL_VURDERT,
        )
    }

    fun ferdigstill(resultat: KlageResultat): Klage {
        if (!this.kanFerdigstille()) {
            throw IllegalStateException(
                "Kan ikke ferdigstille klagen med id=${this.id} med resultatet $resultat " +
                    "på grunn av status til klagen (${this.status})",
            )
        }
        return this.copy(
            resultat = resultat,
            status = KlageStatus.FERDIGSTILT,
        )
    }

    fun kanOppdatereFormkrav(): Boolean {
        return KlageStatus.kanOppdatereFormkrav(this.status)
    }

    fun kanOppdatereUtfall(): Boolean {
        return KlageStatus.kanOppdatereUtfall(this.status)
    }

    fun kanFerdigstille(): Boolean {
        return when (this.status) {
            KlageStatus.UTFALL_VURDERT -> {
                this.utfall != null
            }
            KlageStatus.FORMKRAV_IKKE_OPPFYLT -> {
                // TODO("Støtt avslag på formkrav på klage")
                false
            }
            else -> false
        }
    }

    companion object {
        fun ny(sak: Sak): Klage {
            return Klage(
                id = UUID.randomUUID(),
                sak = sak,
                opprettet = Tidspunkt.now(),
                status = KlageStatus.OPPRETTET,
                kabalStatus = null,
                formkrav = null,
                utfall = null,
                resultat = null,
            )
        }
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "utfall")
sealed class KlageUtfall {
    abstract val saksbehandler: Grunnlagsopplysning.Saksbehandler

    @JsonTypeName("OMGJOERING")
    data class Omgjoering(
        val omgjoering: KlageOmgjoering,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfall()

    @JsonTypeName("DELVIS_OMGJOERING")
    data class DelvisOmgjoering(
        val omgjoering: KlageOmgjoering,
        val innstilling: InnstillingTilKabal,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfall()

    @JsonTypeName("STADFESTE_VEDTAK")
    data class StadfesteVedtak(
        val innstilling: InnstillingTilKabal,
        override val saksbehandler: Grunnlagsopplysning.Saksbehandler,
    ) : KlageUtfall()
}

enum class GrunnForOmgjoering {
    FEIL_LOVANVENDELSE,
    FEIL_REGELVERKSFORSTAAELSE,
    FEIL_ELLER_ENDRET_FAKTA,
    PROSESSUELL_FEIL,
    SAKEN_HAR_EN_AAPEN_BEHANDLING,
    ANNET,
}

data class KlageOmgjoering(val grunnForOmgjoering: GrunnForOmgjoering, val begrunnelse: String)

class InnstillingTilKabal(val lovhjemmel: String, val tekst: String, val brev: KlageBrevInnstilling)

data class InnstillingTilKabalUtenBrev(val lovhjemmel: String, val tekst: String)

data class KlageBrevInnstilling(val brevId: Long)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "utfall")
sealed class KlageUtfallUtenBrev {
    @JsonTypeName("OMGJOERING")
    data class Omgjoering(
        val omgjoering: KlageOmgjoering,
    ) : KlageUtfallUtenBrev()

    @JsonTypeName("DELVIS_OMGJOERING")
    data class DelvisOmgjoering(
        val omgjoering: KlageOmgjoering,
        val innstilling: InnstillingTilKabalUtenBrev,
    ) : KlageUtfallUtenBrev()

    @JsonTypeName("STADFESTE_VEDTAK")
    data class StadfesteVedtak(
        val innstilling: InnstillingTilKabalUtenBrev,
    ) : KlageUtfallUtenBrev()
}

data class Formkrav(
    val vedtaketKlagenGjelder: VedtaketKlagenGjelder?,
    val erKlagerPartISaken: JaNei,
    val erKlagenSignert: JaNei,
    val gjelderKlagenNoeKonkretIVedtaket: JaNei,
    val erKlagenFramsattInnenFrist: JaNei,
    val erFormkraveneOppfylt: JaNei,
) {
    companion object {
        fun erFormkravKonsistente(formkrav: Formkrav): Boolean {
            val alleSvar =
                listOf(
                    formkrav.erKlagenSignert,
                    formkrav.erKlagerPartISaken,
                    formkrav.erKlagenFramsattInnenFrist,
                    formkrav.gjelderKlagenNoeKonkretIVedtaket,
                )

            return if (formkrav.erFormkraveneOppfylt == JaNei.JA) {
                formkrav.vedtaketKlagenGjelder != null && alleSvar.all { it == JaNei.JA }
            } else {
                formkrav.vedtaketKlagenGjelder == null || alleSvar.any { it == JaNei.NEI }
            }
        }
    }
}

data class VedtaketKlagenGjelder(
    val id: String,
    val behandlingId: String,
    val datoAttestert: ZonedDateTime?,
    val vedtakType: VedtakType?,
)

data class FormkravMedBeslutter(
    val formkrav: Formkrav,
    val saksbehandler: Grunnlagsopplysning.Saksbehandler,
)

enum class KlageHendelseType {
    OPPRETTET,
    FERDIGSTILT,
}
