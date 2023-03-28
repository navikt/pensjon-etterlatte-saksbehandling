package no.nav.etterlatte.libs.common.event

interface IGyldigSoeknadVurdert {
    val sakIdKey get() = "sakId"
    val behandlingIdKey get() = "behandlingId"
}

object GyldigSoeknadVurdert : ISoeknadInnsendt, IFordelerFordelt, IGyldigSoeknadVurdert