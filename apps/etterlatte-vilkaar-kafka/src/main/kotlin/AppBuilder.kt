package no.nav.etterlatte


import no.nav.etterlatte.model.VilkaarService


class AppBuilder(private val props: Map<String, String>) {


    fun createVilkaarService(): VilkaarService {
        return VilkaarService()
    }

}
