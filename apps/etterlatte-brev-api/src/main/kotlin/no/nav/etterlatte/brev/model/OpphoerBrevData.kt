package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.behandling.Navn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import java.time.LocalDate

abstract class OpphoerBrevData : BrevData() {
    companion object {
        fun assertRevurderingsaarsakErRiktig(behandling: Behandling, aarsak: RevurderingAarsak, type: String) {
            if (behandling.revurderingsaarsak != aarsak) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for $type når " +
                        "revurderingsårsak er $aarsak"
                )
            }
        }
    }
}

data class AdopsjonRevurderingBrevdata(val virkningsdato: LocalDate, val adoptertAv: Navn) :
    OpphoerBrevData() {
    companion object {
        fun fra(behandling: Behandling): AdopsjonRevurderingBrevdata {
            assertRevurderingsaarsakErRiktig(behandling, RevurderingAarsak.ADOPSJON, "adopsjon")
            if (behandling.revurderingInfo !is RevurderingInfo.Adopsjon) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for adopsjon når " +
                        "revurderingsinfo ikke er adopsjon"
                )
            }

            return AdopsjonRevurderingBrevdata(
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                adoptertAv = behandling.revurderingInfo.adoptertAv
            )
        }
    }
}

data class OmgjoeringAvFarskapRevurderingBrevdata(
    val vedtaksdato: LocalDate,
    val virkningsdato: LocalDate,
    val naavaerendeFar: Navn,
    var forrigeFar: Navn,
    val forrigeVedtaksdato: LocalDate
) : OpphoerBrevData() {
    companion object {
        fun fra(behandling: Behandling): OmgjoeringAvFarskapRevurderingBrevdata {
            assertRevurderingsaarsakErRiktig(
                behandling,
                RevurderingAarsak.OMGJOERING_AV_FARSKAP,
                "omgjøring av farskap"
            )
            if (behandling.revurderingInfo !is RevurderingInfo.OmgjoeringAvFarskap) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for omgjøring av farskap når " +
                        "revurderingsinfo ikke er omgjøring av farskap"
                )
            }

            return OmgjoeringAvFarskapRevurderingBrevdata(
                vedtaksdato = behandling.vedtak.vedtaksdato!!,
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                naavaerendeFar = behandling.revurderingInfo.naavaerendeFar,
                forrigeFar = behandling.revurderingInfo.forrigeFar,
                forrigeVedtaksdato = LocalDate.now() // TODO
            )
        }
    }
}