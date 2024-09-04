package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.sak.Sak
import java.util.UUID

data class GenerellBrevData(
    val sak: Sak,
    val personerISak: PersonerISak,
    val behandlingId: UUID?,
    val forenkletVedtak: ForenkletVedtak?,
    val spraak: Spraak,
    val systemkilde: Vedtaksloesning,
    val utlandstilknytning: Utlandstilknytning? = null,
    val revurderingsaarsak: Revurderingaarsak? = null,
)

// KAN IKKE FJERNES FØR VI HAR EKSPLISITT AVKLART AT ALLE MANUELLE BEHANDLINGER ER FERDIGE
// Tidligere erMigrering - Vil si saker som er løpende i Pesys når det vedtas i Gjenny og opphøres etter vedtaket.
fun loependeIPesys(
    systemkilde: Vedtaksloesning,
    behandlingId: UUID?,
    revurderingsaarsak: Revurderingaarsak?,
) = systemkilde == Vedtaksloesning.PESYS && behandlingId != null && revurderingsaarsak == null

// TODO soeker.foreldreloes benyttes nå kun hvis valgt ved manuell behandling. Må også brukes ved ukjent forelder etter søknad
fun erForeldreloes(
    soeker: Soeker,
    avdoede: List<Avdoed>,
) = soeker.foreldreloes || avdoede.size > 1
