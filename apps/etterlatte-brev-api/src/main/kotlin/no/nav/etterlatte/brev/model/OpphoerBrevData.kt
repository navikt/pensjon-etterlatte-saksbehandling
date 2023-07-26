package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.libs.common.behandling.Navn
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import java.time.LocalDate

abstract class OpphoerBrevData : BrevData() {
    companion object {
        fun assertRevurderingsaarsakStemmerMedTypen(behandling: Behandling, aarsak: RevurderingAarsak, type: String) {
            if (behandling.revurderingsaarsak != aarsak) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for $type når " +
                        "revurderingsårsak er $aarsak - mismatch mellom type og årsak, disse må stemme overens."
                )
            }
        }
    }
}

data class AdopsjonRevurderingBrevdata(
    val virkningsdato: LocalDate,
    val adopsjonsdato: LocalDate,
    val adoptertAv1: Navn,
    val adoptertAv2: Navn? = null
) :
    OpphoerBrevData() {
    companion object {
        fun fra(behandling: Behandling): AdopsjonRevurderingBrevdata {
            assertRevurderingsaarsakStemmerMedTypen(behandling, RevurderingAarsak.ADOPSJON, "adopsjon")
            if (behandling.revurderingInfo !is RevurderingInfo.Adopsjon) {
                throw IllegalArgumentException(
                    "Kan ikke opprette et revurderingsbrev for adopsjon når " +
                        "revurderingsinfo ikke er adopsjon"
                )
            }

            return AdopsjonRevurderingBrevdata(
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                adopsjonsdato = behandling.adopsjonsdato!!,
                adoptertAv1 = behandling.revurderingInfo.adoptertAv1,
                adoptertAv2 = behandling.revurderingInfo.adoptertAv2
            )
        }
    }
}

data class OmgjoeringAvFarskapRevurderingBrevdata(
    val virkningsdato: LocalDate,
    val naavaerendeFar: Navn,
    var forrigeFar: Navn,
    val opprinneligInnvilgelsesdato: LocalDate
) : OpphoerBrevData() {
    companion object {
        fun fra(behandling: Behandling): OmgjoeringAvFarskapRevurderingBrevdata {
            assertRevurderingsaarsakStemmerMedTypen(
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
                virkningsdato = behandling.virkningsdato!!.atDay(1),
                naavaerendeFar = behandling.revurderingInfo.naavaerendeFar,
                forrigeFar = behandling.revurderingInfo.forrigeFar,
                opprinneligInnvilgelsesdato = behandling.innvilgelsesdato!!
            )
        }
    }
}