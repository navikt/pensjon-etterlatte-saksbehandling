package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.*
import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

//class AvbruttBehandlingException(message: String) : RuntimeException(message) {}

class GrunnlagAggregat(
    private val saksid: Long,
    private val grunnlagDao: GrunnlagDao,
    private val opplysninger: OpplysningDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GrunnlagAggregat::class.java)

        fun opprett(
            sak: Long,
            grunnlag: GrunnlagDao,
            opplysninger: OpplysningDao
        ): GrunnlagAggregat {
            logger.info("Oppretter en behandling på ${sak}")
            return Grunnlag(UUID.randomUUID(), sak, emptyList())
                .also {
                    grunnlag.opprett(it)
                    logger.info("Opprettet behandling ${it.id} i sak ${it.saksId}")
                }
                .let { GrunnlagAggregat(it.saksId, grunnlag, opplysninger) }
        }
    }

    //TODO trengs noe sånt?
    /*
    private object TilgangDao {
        fun sjekkOmBehandlingTillatesEndret(grunnlag: Grunnlag): Boolean {
            return grunnlag.status in listOf(
                BehandlingStatus.GYLDIGHETSPRØVD,
                BehandlingStatus.VILKÅRSPRØVD,
                BehandlingStatus.OPPRETTET,
                BehandlingStatus.FASTSATT,
                BehandlingStatus.BEREGNET
            )
        }
    }

     */

    var lagretGrunnlag = requireNotNull(grunnlagDao.hent(saksid))
    private var lagredeOpplysninger = opplysninger.finnOpplysningerIGrunnlag(saksid)

    fun leggTilGrunnlagListe(nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>) {
        if (nyeOpplysninger.isEmpty()) return

        for (opplysning in nyeOpplysninger) {
            leggTilGrunnlagUtenVilkårsprøving(opplysning.opplysning, opplysning.opplysningType, opplysning.kilde)
        }
        //vilkårsprøv()
    }

    fun leggTilGrunnlagUtenVilkårsprøving(
        data: ObjectNode,
        type: Opplysningstyper,
        kilde: Grunnlagsopplysning.Kilde?
    ): UUID {
        /*if (!TilgangDao.sjekkOmBehandlingTillatesEndret(lagretBehandling)) {
            throw AvbruttBehandlingException(
                "Det tillattes ikke å legge til grunnlag uten vilkårsprøving for Behandling med id ${lagretBehandling.id} og status: ${lagretBehandling.status}"
            )
        }

         */
        val grunnlagsopplysning = Grunnlagsopplysning(
            UUID.randomUUID(),
            kildeFraRequestContekst(kilde),
            type,
            objectMapper.createObjectNode(),
            data
        )
        opplysninger.nyOpplysning(grunnlagsopplysning)
        opplysninger.leggOpplysningTilGrunnlag(lagretGrunnlag.id, grunnlagsopplysning.id)
        lagredeOpplysninger += grunnlagsopplysning
        logger.info("La til opplysning $type i behandling ${lagretGrunnlag.id}")
        return grunnlagsopplysning.id
    }


    private fun kildeFraRequestContekst(oppgittKilde: Grunnlagsopplysning.Kilde?): Grunnlagsopplysning.Kilde {
        return if (Kontekst.get().AppUser.kanSetteKilde() && oppgittKilde != null) oppgittKilde else when (Kontekst.get().AppUser) {
            is Saksbehandler -> if (oppgittKilde == null) Grunnlagsopplysning.Saksbehandler(Kontekst.get().AppUser.name()) else throw IllegalArgumentException()
            is Kunde -> if (oppgittKilde == null) Grunnlagsopplysning.Privatperson(
                Kontekst.get().AppUser.name(),
                Instant.now()
            ) else throw IllegalArgumentException()
            else -> throw IllegalArgumentException()
        }
    }


    fun serialiserbarUtgave() = lagretGrunnlag.copy(grunnlag = lagredeOpplysninger)
}