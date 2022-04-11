package vilkaar

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.objectMapper
import vilkaar.grunnlag.Vilkaarsgrunnlag
import vilkaar.grunnlag.VilkarIBehandling
import java.sql.Connection
import java.util.*

class VurderteVilkaarDao(val connection: Connection) {

    fun hentOppVurderinger(behandling: UUID): List<VilkarIBehandling>{
        return connection.prepareStatement("SELECT *  FROM vurdertvilkaar where behandling = ?")
            .apply {
                setObject(1, behandling)
            }.executeQuery().toList{
                VilkarIBehandling(
                    behandling,
                    Vilkaarsgrunnlag(
                        avdoedSoeknad = getString("avdoedSoeknad")?.let { objectMapper.readValue(it) },
                        soekerSoeknad = getString("soekerSoeknad")?.let { objectMapper.readValue(it) },
                        soekerPdl = getString("soekerPdl")?.let { objectMapper.readValue(it) },
                        avdoedPdl = getString("avdoedPdl")?.let { objectMapper.readValue(it) },
                        gjenlevendePdl = getString("gjenlevendePdl")?.let { objectMapper.readValue(it) }
                    ),
                    getLong("versjon"),
                    objectMapper.readValue(getString("vilkaarResultat"))
                )

            }
    }

    fun lagreVurdering(vilkarIBehandling: VilkarIBehandling){
        connection.prepareStatement("INSERT INTO vurdertvilkaar(behandling, versjon, vilkaarResultat, avdoedSoeknad, soekerSoeknad, soekerPdl, avdoedPdl, gjenlevendePdl) values(?, ?, ?, ?, ?, ?, ?, ?)")
            .also {
                it.setObject(1, vilkarIBehandling.behandling)
                it.setLong(2, vilkarIBehandling.versjon)
                it.setString(3, objectMapper.writeValueAsString(vilkarIBehandling.vilkaarResultat))
                it.setString(4, vilkarIBehandling.grunnlag.avdoedSoeknad?.let { objectMapper.writeValueAsString(it) })
                it.setString(5, vilkarIBehandling.grunnlag.soekerSoeknad?.let { objectMapper.writeValueAsString(it) })
                it.setString(6, vilkarIBehandling.grunnlag.soekerPdl?.let { objectMapper.writeValueAsString(it) })
                it.setString(7, vilkarIBehandling.grunnlag.avdoedPdl?.let { objectMapper.writeValueAsString(it) })
                it.setString(8, vilkarIBehandling.grunnlag.gjenlevendePdl?.let { objectMapper.writeValueAsString(it) })
            }.executeUpdate().also { require(it==1) }

    }
}