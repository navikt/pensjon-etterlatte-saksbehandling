import java.io.File

fun main() {
    val dataDir = (System.getenv()["INPUT_DATA_DIR"] ?: "").trimEnd('/')
    val filnavnEtter = "$dataDir/bp-prod-utbetalinger-etter.csv"
    val linjerEtter: List<Utbetalingslinje> = utbetalingslinjer(filnavnEtter)

    val loepende = saksIder("$dataDir/bp-prod-loepende.csv")

    val sakerRegulert = linjerEtter.map { it.sak_id }

    loepende
        .filter { saksid -> !sakerRegulert.contains(saksid) }
        .forEach { println("Ikke regulert BP: $it") }

    File("./test.txt").writeText("abc")
}

private fun saksIder(filnavn: String): List<Long> {
    val linjer: List<Long> =
        File(filnavn)
            .useLines { linjer -> linjer.toList() }
            .map { linje -> linje.toLong() }
    return linjer
}

// 2023-03-24 09:01:13.149313+00
