package no.nav.etterlatte.behandling.jobs.sjekkadressebeskyttelse

import no.nav.etterlatte.common.ConnectionAutoclosing
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.database.toList

data class SakIdMedAdressebeskyttelse(
    val sakId: SakId,
    val sakType: SakType,
    val ident: String,
    val adressebeskyttelse: AdressebeskyttelseGradering?,
)

class SjekkAdressebeskyttelseJobDao(
    private val connectionAutoclosing: ConnectionAutoclosing,
) {
    fun hentSakerMedAdressebeskyttelse(): List<SakIdMedAdressebeskyttelse> =
        connectionAutoclosing.hentConnection { connection ->
            with(connection) {
                val statement = prepareStatement("SELECT id, sakType, fnr, adressebeskyttelse from sak")
                statement
                    .executeQuery()
                    .toList {
                        SakIdMedAdressebeskyttelse(
                            sakId = SakId(getLong("id")),
                            sakType = SakType.valueOf(getString("sakType")),
                            ident = getString("fnr"),
                            adressebeskyttelse =
                                getString("adressebeskyttelse")?.let {
                                    AdressebeskyttelseGradering.valueOf(it)
                                },
                        )
                    }
            }
        }
}
