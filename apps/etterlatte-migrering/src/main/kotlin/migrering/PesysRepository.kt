package migrering

internal class PesysRepository {

    fun hentSaker(): List<Pesyssak> = listOf(Pesyssak(id = "1234"))
}

data class Pesyssak(val id: String)