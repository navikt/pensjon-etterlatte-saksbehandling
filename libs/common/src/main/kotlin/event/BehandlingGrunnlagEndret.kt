package no.nav.etterlatte.libs.common.event

interface IBehandlingGrunnlagEndret {
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

object BehandlingGrunnlagEndret : IBehandlingGrunnlagEndret