package no.nav.etterlatte.intern

import no.nav.etterlatte.avkorting.Avkorting
import no.nav.etterlatte.beregning.AnvendtTrygdetidRegelresultat
import no.nav.etterlatte.beregning.grunnlag.GrunnlagMedPeriode
import no.nav.etterlatte.beregning.regler.AnvendtTrygdetid
import no.nav.etterlatte.libs.common.beregning.Sanksjon
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.sanksjon.LagreSanksjon

data class LagreAnvendtTrygdetidRequest(
    val anvendt: List<GrunnlagMedPeriode<List<AnvendtTrygdetid>>>,
    val utrekning: List<AnvendtTrygdetidRegelresultat>,
)

data class LagreAvkortingRequest(
    val sakId: SakId,
    val avkorting: Avkorting,
)

data class OpprettSanksjonRequest(
    val sakId: SakId,
    val saksbehandlerIdent: String,
    val sanksjon: LagreSanksjon,
)

data class OpprettSanksjonFraKopiRequest(
    val sakId: SakId,
    val sanksjon: Sanksjon,
)

data class OppdaterSanksjonRequest(
    val saksbehandlerIdent: String,
    val sanksjon: LagreSanksjon,
)
