package no.nav.etterlatte.libs.common.event

interface IBehandlingGrunnlagEndret {
    val eventName get() = "BEHANDLING:GRUNNLAGENDRET"
    val behandlingObjectKey get() = "behandling"
    val behandlingIdKey get() = "behandlingId"
    val sakIdKey get() = "sakId"
    val fnrSoekerKey get() = "fnrSoeker"
    val behandlingOpprettetKey get() = "behandlingOpprettet"
    val persongalleriKey get() = "persongalleri"
    val revurderingAarsakKey get() = "revurderingsaarsak"
}
interface IMedGrunnlag {
    val grunnlagKey get() = "grunnlag"
}


object BehandlingGrunnlagEndret: IBehandlingGrunnlagEndret
object BehandlingGrunnlagEndretMedGrunnlag: IBehandlingGrunnlagEndret, IMedGrunnlag