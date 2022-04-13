package vilkaar

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.GrunnlagHendelseType
import no.nav.etterlatte.libs.common.vikaar.Grunnlagshendelse
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.Vilkaarsgrunnlag
import no.nav.etterlatte.libs.common.vikaar.VilkarIBehandling
import java.sql.Connection
import java.sql.Types
import java.util.*

class VurderteVilkaarDao(val connection: Connection) {

    fun hentOppVurderinger(behandling: UUID): List<VilkarIBehandling> {
        return connection.prepareStatement("SELECT *  FROM vurdertvilkaar where behandling = ?")
            .apply {
                setObject(1, behandling)
            }.executeQuery().toList {
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

    fun lagreGrunnlagshendelse(hendelser: List<Grunnlagshendelse>) {
        if (hendelser.isEmpty()) return
        connection.prepareStatement("INSERT INTO grunnlagshendelse(behandling, opplysning, kilde, opplysningtype, hendelsenummer, hendelsetype, hendelseref) VALUES(?, ?, ?, ?, ?, ?, ?)")
            .also { statement ->
                hendelser.forEach { hendelse ->
                    statement.setObject(1, hendelse.behandling)
                    statement.setString(2, hendelse.opplysning?.opplysning?.let { objectMapper.writeValueAsString(it) })
                    statement.setString(3, hendelse.opplysning?.kilde?.let { objectMapper.writeValueAsString(it) })
                    statement.setString(4, hendelse.opplysning?.opplysningType?.name)
                    statement.setLong(5, hendelse.hendelsenummer)
                    statement.setString(6, hendelse.hendelsetype.name)
                    hendelse.hendelsereferanse?.let { statement.setLong(7, it) } ?: statement.setNull(7, Types.BIGINT)
                    statement.addBatch()
                }
                require(statement.executeBatch().sum() == hendelser.size)
            }

    }

    fun hentGrunnlagsHendelser(behandling: UUID): List<Grunnlagshendelse> {
        return connection.prepareStatement("SELECT opplysning, kilde, opplysningtype, hendelsenummer, hendelsetype, hendelseref from grunnlagshendelse WHERE behandling = ?")
            .also {
                it.setObject(1, behandling)
            }
            .executeQuery().toList {
                Grunnlagshendelse(
                    behandling = behandling,
                    hendelsenummer = getLong("hendelsenummer"),
                    opplysning = lagVilkaarOpplysning(
                        getString("opplysningtype"),
                        getString("kilde"),
                        getString("opplysning")
                    ),
                    hendelsetype = GrunnlagHendelseType.valueOf(getString("hendelsetype")),
                    hendelsereferanse = getLong("hendelseref").takeIf { !wasNull() }

                )
            }

    }

    private fun lagVilkaarOpplysning(
        type: String?,
        kilde: String?,
        opplysning: String?
    ): VilkaarOpplysning<ObjectNode>? {
        return type?.let {
            kilde?.let {
                opplysning?.let {
                    VilkaarOpplysning(
                        opplysningType = Opplysningstyper.valueOf(type),
                        kilde = objectMapper.readValue(kilde),
                        opplysning = objectMapper.readTree(opplysning) as ObjectNode
                    )
                }
            }
        }
    }

    /*
    CREATE TABLE grunnlagshendelse(
    behandling UUID NOT NULL,
    opplysning TEXT,
    kilde TEXT,
    opplysningtype TEXT NOT NULL,
    hendelsenummer BIGINT NOT NULL,
    PRIMARY KEY(behandling, hendelsenummer)
);

    * */


    fun lagreVurdering(vilkarIBehandling: VilkarIBehandling) {
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
            }.executeUpdate().also { require(it == 1) }

    }
}