package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.brevbaker.SoekerOgEventuellVerge
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

data class RedigerbarTekstRequest(
    val brukerTokenInfo: BrukerTokenInfo,
    val soekerOgEventuellVerge: SoekerOgEventuellVerge,
    val sakType: SakType,
    val forenkletVedtak: ForenkletVedtak?,
    val utlandstilknytningType: UtlandstilknytningType?,
    val revurderingaarsak: Revurderingaarsak?,
    val behandlingId: UUID?,
    val erForeldreloes: Boolean,
    val loependeIPesys: Boolean,
    val systemkilde: Vedtaksloesning,
    val avdoede: List<Avdoed>,
)
