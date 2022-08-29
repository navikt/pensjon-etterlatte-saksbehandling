package no.nav.etterlatte.libs.common.event

interface IGyldigSoeknadVurdert {
    val sakIdKey get() = "sakId"
    val behandlingIdKey get() = "behandlingId"
    val gyldigInnsenderKey get() = "gyldigInnsender"
}

object GyldigSoeknadVurdert : ISoeknadInnsendt, IFordelerFordelt, IGyldigSoeknadVurdert {
    val eventName: String get() = "GYLDIG_SOEKNAD:VURDERT"
}
