package vilkaarsvurdering

object VilkaarsvurderingAPI {
    const val basisrute = "api/vilkaarsvurdering"

    fun kopier(behandlingsId: String) = "/$behandlingsId/kopier"
    fun opprett(behandlingsId: String) = "/$behandlingsId/opprett"
}