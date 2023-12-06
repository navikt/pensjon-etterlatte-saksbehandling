package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import java.time.LocalDateTime
import java.util.UUID

/**
 * TODO:
 *  8 av 18 felter her brukes KUN av statistikk.
 *  Burde kanskje vurdere om statistikk skulle hatt sin egen BehandlingDTO...?
 *  Eventuelt om vi kan fjerne og heller kalle på grunnlag fra statistikk?
 **/
data class DetaljertBehandling(
    val id: UUID,
    val sak: Long,
    val sakType: SakType,
    val behandlingOpprettet: LocalDateTime, // kun statistikk
    val soeknadMottattDato: LocalDateTime?, // kun statistikk
    val innsender: String?, // kun statistikk
    val soeker: String, // statistikk og vedtak
    val gjenlevende: List<String>?, // kun statistikk
    val avdoed: List<String>?, // kun statistikk
    val soesken: List<String>?, // kun statistikk
    val status: BehandlingStatus, // kun statistikk
    val behandlingType: BehandlingType,
    val virkningstidspunkt: Virkningstidspunkt?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val utlandstilknytning: Utlandstilknytning?,
    val revurderingsaarsak: Revurderingaarsak? = null,
    val revurderingInfo: RevurderingInfo?,
    val prosesstype: Prosesstype, // statistikk og trygdetid
    val enhet: String, // kun statistikk
    val kilde: Vedtaksloesning,
) {
    fun kanVedta(type: VedtakType): Boolean {
        return !(revurderingsaarsak.girOpphoer() && type != VedtakType.OPPHOER)
    }
}

fun Revurderingaarsak?.girOpphoer() = this != null && utfall.girOpphoer
