package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.DevOgProd
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.IngenMiljoe
import no.nav.etterlatte.libs.common.behandling.KanBrukesIMiljoe.KunIDev
import no.nav.etterlatte.libs.common.clusternavn

private val SAKTYPE_OMS = listOf(SakType.OMSTILLINGSSTOENAD)
private val SAKTYPE_BP = listOf(SakType.BARNEPENSJON)
private val SAKTYPE_BP_OMS = SAKTYPE_BP + SAKTYPE_OMS

sealed class KanBrukesIMiljoe {
    abstract val prod: Boolean
    abstract val dev: Boolean

    object KunIDev : KanBrukesIMiljoe() {
        override val prod = false
        override val dev = true
    }

    object DevOgProd : KanBrukesIMiljoe() {
        override val prod = true
        override val dev = true
    }

    object IngenMiljoe : KanBrukesIMiljoe() {
        override val prod = false
        override val dev = false
    }
}

enum class RevurderingAarsak(
    private val gyldigFor: List<SakType>,
    private val miljoe: KanBrukesIMiljoe,
    val girOpphoer: Boolean,
    val skalSendeBrev: Boolean
) {
    ANSVARLIGE_FORELDRE(SAKTYPE_BP, IngenMiljoe, false, true),
    SOESKENJUSTERING(SAKTYPE_BP, KunIDev, false, true),
    UTLAND(SAKTYPE_BP, IngenMiljoe, false, true),
    BARN(SAKTYPE_BP, IngenMiljoe, false, true),
    VERGEMAAL_ELLER_FREMTIDSFULLMAKT(SAKTYPE_BP, IngenMiljoe, false, true),
    REGULERING(SAKTYPE_BP_OMS, DevOgProd, false, false),
    DOEDSFALL(SAKTYPE_BP_OMS, KunIDev, true, false),
    INNTEKTSENDRING(SAKTYPE_OMS, KunIDev, false, true),
    OMGJOERING_AV_FARSKAP(SAKTYPE_BP, KunIDev, true, true);

    fun kanBrukesIMiljo(): Boolean = when (clusternavn()) {
        null -> true
        GcpEnv.PROD.name -> miljoe.prod
        GcpEnv.DEV.name -> miljoe.dev
        else -> miljoe.dev
    }

    fun gyldigForSakType(sakType: SakType): Boolean = gyldigFor.any { it == sakType }
}

enum class GcpEnv(val env: String) {
    PROD("prod-gcp"),
    DEV("dev-gcp")
}