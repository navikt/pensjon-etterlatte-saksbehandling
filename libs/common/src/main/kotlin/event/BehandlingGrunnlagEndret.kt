package no.nav.etterlatte.libs.common.event

object PackageMessageName {
    val nyOpplysning get() = "OPPLYSNING:NY"
}

interface IBehandlingGrunnlagEndret {
    val eventName get() = "BEHANDLING:GRUNNLAGENDRET"
    val behandlingObjectKey get() = "behandling"
    val sakObjectKey get() = "sak"
    val behandlingIdKey get() = "behandling.id"
    val sakIdKey get() = "sakId"
    val fnrSoekerKey get() = "fnrSoeker"
    val behandlingOpprettetKey get() = "behandlingOpprettet"
    val persongalleriKey get() = "persongalleri"
    val revurderingAarsakKey get() = "revurderingsaarsak"
    val manueltOpphoerAarsakKey get() = "manueltOpphoerAarsak"
    val manueltOpphoerfritekstAarsakKey get() = "manueltOpphoerFritekstAarsak"
}

interface IMedGrunnlag {
    val grunnlagKey get() = "grunnlag"
}

object BehandlingGrunnlagEndret : IBehandlingGrunnlagEndret
object BehandlingGrunnlagEndretMedGrunnlag : IBehandlingGrunnlagEndret, IMedGrunnlag