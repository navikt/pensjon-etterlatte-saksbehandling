package no.nav.etterlatte.behandling.etteroppgjoer

import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.AInntekt
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.util.UUID

data class Etteroppgjoer(
    val behandling: EtteroppgjoerBehandling,
    val opplysninger: EtteroppgjoerOpplysninger,
)

data class EtteroppgjoerBehandling(
    val id: UUID,
    val sekvensnummerSkatt: String,
    val status: String, // TODO enum
    val sak: Sak,
    val aar: Int,
    val opprettet: Tidspunkt,
)

data class EtteroppgjoerOpplysninger(
    val skatt: PensjonsgivendeInntektFraSkatt,
    val ainntekt: AInntekt,
    val tidligereAvkorting: AvkortingDto,
)
