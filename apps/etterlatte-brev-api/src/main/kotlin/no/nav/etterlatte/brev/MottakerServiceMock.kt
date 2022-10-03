package no.nav.etterlatte.brev


class MottakerServiceMock:MottakerService {

    override suspend fun hentStatsforvalterListe(): List<Enhet> = listOf(Enhet(organisasjonsnummer = "11057523044",navn ="STATSFORVALTEREN I AGDER"))

}

