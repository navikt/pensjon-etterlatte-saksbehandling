package no.nav.etterlatte.behandling

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Avkortingsinfo
import no.nav.etterlatte.brev.behandling.Innsender
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.mapInnsender
import no.nav.etterlatte.brev.behandling.mapSoeker
import no.nav.etterlatte.brev.behandling.mapSpraak
import no.nav.etterlatte.brev.hentVergeForSak
import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.kodeverk.LandDto
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import java.time.LocalDate

internal data class OmsVedtaksbrevGrunnlag(
    val virkningsdato: LocalDate,
    val avkortingsinfo: Avkortingsinfo,
    val beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
    val alleLand: List<LandDto>,
    val trygdetid: List<TrygdetidDto>,
    val sak: Sak,
    val grunnlag: Grunnlag,
    val avdoede: List<Avdoed>,
    val omsRettUtenTidsbegrensning: Boolean,
    val klage: Klage?,
    val etterbetaling: EtterbetalingDTO?,
    val brevutfall: BrevutfallDto,
) {
    fun innsender(): Innsender? = grunnlag.mapInnsender()

    fun soeker(): Soeker = grunnlag.mapSoeker(null)

    fun spraak(): Spraak = grunnlag.mapSpraak()

    fun verge(): Verge? = hentVergeForSak(sak.sakType, brevutfall, grunnlag)
}
