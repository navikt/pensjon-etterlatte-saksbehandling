package vilkaarsvurdering

import no.nav.etterlatte.vilkaarsvurdering.OpprettVilkaarsvurderingFraBehandling

object VilkaarsvurderingAPI {
    const val basisrute = "api/vilkaarsvurdering"
//    const kopierRute =

    fun kopier(behandlingsId: String) =
        Route("/$behandlingsId/kopier", OpprettVilkaarsvurderingFraBehandling::class.java)

    fun opprett(behandlingsId: String) = Route("/$behandlingsId/opprett", Unit::class.java)
}

data class Route<T>(
    val url: String,
    val clazz: Class<T>
) {
    fun medBody(baseURL: String, f: () -> T) = Invocation("$baseURL/$VilkaarsvurderingAPI.basisrute/$url", f())
}

data class Invocation<T>(val url: String, val body: T)