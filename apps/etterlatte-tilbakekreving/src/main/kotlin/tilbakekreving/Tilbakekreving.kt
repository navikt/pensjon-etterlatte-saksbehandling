package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.BehandlingId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.Kravgrunnlag
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.KravgrunnlagId
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.NavIdent
import no.nav.etterlatte.tilbakekreving.kravgrunnlag.SakId


object Tilbakekrevingsvedtak

class Attestasjon(
    val attestant: NavIdent,
    val attestertTidspunkt: Tidspunkt,
)

enum class TilbakekrevingHendelse {
    MOTTATT_KRAVGRUNNLAG,
    OPPDATERT_KRAVGRUNNLAG,
    VEDTAK_FATTET,
    VEDTAK_ATTESTERT
}

sealed interface Tilbakekreving {
    val sakId: SakId
    val behandlingId: BehandlingId
    val kravgrunnlagId: KravgrunnlagId
    val opprettet: Tidspunkt
    val kravgrunnlag: Kravgrunnlag

    data class MottattKravgrunnlag(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val kravgrunnlagId: KravgrunnlagId,
        override val opprettet: Tidspunkt,
        override val kravgrunnlag: Kravgrunnlag,
    ) : Tilbakekreving

    data class FattetVedtak(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val kravgrunnlagId: KravgrunnlagId,
        override val opprettet: Tidspunkt,
        override val kravgrunnlag: Kravgrunnlag,
        val vedtak: Tilbakekrevingsvedtak
    ) : Tilbakekreving

    data class AttestertVedtak(
        override val sakId: SakId,
        override val behandlingId: BehandlingId,
        override val kravgrunnlagId: KravgrunnlagId,
        override val opprettet: Tidspunkt,
        override val kravgrunnlag: Kravgrunnlag,
        val vedtak: Tilbakekrevingsvedtak,
        val attestasjon: Attestasjon,
    ) : Tilbakekreving
}
