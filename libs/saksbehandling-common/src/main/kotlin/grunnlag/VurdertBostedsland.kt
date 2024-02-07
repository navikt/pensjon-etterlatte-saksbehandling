package no.nav.etterlatte.grunnlag

data class VurdertBostedsland(val landkode: String) {
    fun erNorge() = landkode == Bostedsland.NOR.name

    companion object {
        val finsIkkeIPDL = VurdertBostedsland(Bostedsland.FinsIkkeIPDL.name)
    }
}

enum class Bostedsland(val land: String) {
    NOR("Norge"),
    XUK("Brukeren er vurdert bosatt i utlandet, men vi vet ikke hvor i utlandet personen er bosatt."),
    FinsIkkeIPDL("Identen finnes ikke i PDL"),
}
