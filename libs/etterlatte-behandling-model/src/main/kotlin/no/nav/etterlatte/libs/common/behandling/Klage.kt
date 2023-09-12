package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.ZonedDateTime
import java.util.*

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
    FERDIGSTILT; // klagen er ferdig fra gjenny sin side

    companion object {
        fun kanOppdatereFormkrav(status: KlageStatus): Boolean {
            return status !== FERDIGSTILT
        }
    }
}

// Placeholder til vi vet mer om hvilken flyt vi har her
enum class KabalStatus

data class Klage(
    val id: UUID,
    val sak: Sak,
    val opprettet: Tidspunkt,
    val status: KlageStatus,
    val kabalStatus: KabalStatus?,
    val formkrav: FormkravMedBeslutter?,
    val utfall: KlageUtfall?
) {
    fun oppdaterFormkrav(formkrav: Formkrav, saksbehandlerIdent: String): Klage {
        if (!kanOppdatereFormkrav(this)) {
            throw IllegalStateException(
                "Kan ikke oppdatere formkrav i klagen med id=${this.id}, på grunn av " +
                    "tilstanden til klagen: ${this.status}"
            )
        }
        return this.copy(
            formkrav = FormkravMedBeslutter(
                formkrav = formkrav,
                saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandlerIdent)
            ),
            status = when (formkrav.erFormkraveneOppfylt) {
                JaNei.JA -> KlageStatus.FORMKRAV_OPPFYLT
                JaNei.NEI -> KlageStatus.FORMKRAV_IKKE_OPPFYLT
            }
        )
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
                utfall = null
            )
        }

        fun kanOppdatereFormkrav(klage: Klage): Boolean {
            return KlageStatus.kanOppdatereFormkrav(klage.status)
        }
    }
}

// Placeholdere for utfall / formkravene til klagen.
class KlageUtfall

data class Formkrav(
    val vedtaketKlagenGjelder: VedtaketKlagenGjelder?,
    val erKlagerPartISaken: JaNei,
    val erKlagenSignert: JaNei,
    val gjelderKlagenNoeKonkretIVedtaket: JaNei,
    val erKlagenFramsattInnenFrist: JaNei,
    val erFormkraveneOppfylt: JaNei
) {
    companion object {
        fun erFormkravKonsistente(formkrav: Formkrav): Boolean {
            val alleSvar = listOf(
                formkrav.erKlagenSignert,
                formkrav.erKlagerPartISaken,
                formkrav.erKlagenFramsattInnenFrist,
                formkrav.gjelderKlagenNoeKonkretIVedtaket
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
    val vedtakType: VedtakType?
)

data class FormkravMedBeslutter(
    val formkrav: Formkrav,
    val saksbehandler: Grunnlagsopplysning.Saksbehandler
)

enum class KlageHendelseType {
    OPPRETTET
}