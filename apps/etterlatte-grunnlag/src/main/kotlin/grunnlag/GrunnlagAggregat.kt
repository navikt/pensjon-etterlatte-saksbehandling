package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import org.slf4j.LoggerFactory
import java.util.*

//class AvbruttBehandlingException(message: String) : RuntimeException(message) {}

class GrunnlagAggregat(
    private val saksid: Long,
    private val opplysninger: OpplysningDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GrunnlagAggregat::class.java)

        fun opprett(
            sak: Long,
            opplysninger: OpplysningDao
        ): GrunnlagAggregat {
            logger.info("Oppretter grunnlag for $sak")
            return Grunnlag(sak, emptyList())
                .also {
                    logger.info("Opprettet grunnlag for sak ${it.saksId}")
                }
                .let { GrunnlagAggregat(it.saksId, opplysninger) }
        }
    }

    private var lagredeOpplysninger = opplysninger.finnOpplysningerIGrunnlag(saksid)

    fun leggTilGrunnlagListe(nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>) {
        if (nyeOpplysninger.isEmpty()) return

        for (opplysning in nyeOpplysninger) {
            leggTilGrunnlagUtenVilkaarsprøving(opplysning.opplysning, opplysning.opplysningType, opplysning.kilde)
        }
    }

    fun leggTilGrunnlagUtenVilkaarsprøving(
        data: ObjectNode,
        type: Opplysningstyper,
        kilde: Grunnlagsopplysning.Kilde
    ): UUID {
        val grunnlagsopplysning = Grunnlagsopplysning(
            UUID.randomUUID(),
            //kildeFraRequestContekst(kilde),
            kilde,
            type,
            objectMapper.createObjectNode(),
            data
        )
        opplysninger.leggOpplysningTilGrunnlag(saksid, grunnlagsopplysning)
        lagredeOpplysninger = lagredeOpplysninger + grunnlagsopplysning
        logger.info("La til opplysning $type i sak $saksid")
        return grunnlagsopplysning.id
    }

    //TODO tukler ikke med kilde ennå
/*
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


 */

    fun serialiserbarUtgave() = Grunnlag(saksId = saksid, grunnlag = lagredeOpplysninger)
}

