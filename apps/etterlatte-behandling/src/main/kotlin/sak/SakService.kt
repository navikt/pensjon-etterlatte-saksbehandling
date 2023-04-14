package no.nav.etterlatte.sak

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.IngenEnhetFunnetException
import no.nav.etterlatte.common.IngenGeografiskOmraadeFunnetForEnhet
import no.nav.etterlatte.common.klienter.PdlKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import org.slf4j.LoggerFactory

enum class SakServiceFeatureToggle(private val key: String) : FeatureToggle {
    OpprettMedEnhetId("pensjon-etterlatte.opprett-sak-med-enhet-id"),
    FiltrerMedEnhetId("pensjon-etterlatte.filtrer-saker-med-enhet-id");

    override fun key() = key
}

interface SakService {
    fun hentSaker(): List<Sak>
    fun finnSaker(person: String): List<Sak>
    fun finnEllerOpprettSak(fnr: String, type: SakType): Sak
    fun finnSak(person: String, type: SakType): Sak?
    fun finnSak(id: Long): Sak?
    fun slettSak(id: Long)
    fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean)
    fun finnEnhetForPersonOgTema(fnr: String, tema: String): ArbeidsFordelingEnhet?
    fun oppdaterEnhetForSaker(saker: List<GrunnlagsendringshendelseService.SakMedEnhet>)
}

class RealSakService(
    private val dao: SakDao,
    private val pdlKlient: PdlKlient,
    private val norg2Klient: Norg2Klient,
    private val featureToggleService: FeatureToggleService
) : SakService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun hentSaker(): List<Sak> {
        return dao.hentSaker().filterForEnheter()
    }

    private fun finnSakerForPerson(person: String) = dao.finnSaker(person)

    private fun finnSakerForPersonOgType(person: String, type: SakType) = finnSakerForPerson(person).find {
        it.sakType == type
    }

    override fun finnSaker(person: String): List<Sak> {
        return inTransaction {
            finnSakerForPerson(person).filterForEnheter()
        }
    }

    override fun slettSak(id: Long) {
        dao.slettSak(id)
    }

    override fun markerSakerMedSkjerming(sakIder: List<Long>, skjermet: Boolean) {
        inTransaction {
            dao.markerSakerMedSkjerming(sakIder, skjermet)
        }
    }

    override fun finnEllerOpprettSak(fnr: String, type: SakType) =
        finnSakerForPersonOgType(fnr, type) ?: dao.opprettSak(
            fnr,
            type,
            finnEnhetForPersonOgTema(fnr, type.tema)?.enhetNr
        )

    override fun oppdaterEnhetForSaker(saker: List<GrunnlagsendringshendelseService.SakMedEnhet>) {
        inTransaction {
            dao.oppdaterEnhet(saker)
        }
    }

    override fun finnSak(person: String, type: SakType): Sak? {
        return finnSakerForPersonOgType(person, type).sjekkEnhet()
    }

    override fun finnSak(id: Long): Sak? {
        return dao.hentSak(id).sjekkEnhet()
    }

    override fun finnEnhetForPersonOgTema(fnr: String, tema: String): ArbeidsFordelingEnhet? {
        if (featureToggleService.isEnabled(SakServiceFeatureToggle.OpprettMedEnhetId, false)) {
            val tilknytning = pdlKlient.hentGeografiskTilknytning(fnr)
            val geografiskTilknytning = tilknytning.geografiskTilknytning()

            return when {
                tilknytning.ukjent -> ArbeidsFordelingEnhet(Enheter.DEFAULT.navn, Enheter.DEFAULT.enhetNr)
                geografiskTilknytning == null -> throw IngenGeografiskOmraadeFunnetForEnhet(
                    Folkeregisteridentifikator.of(fnr),
                    tema
                ).also {
                    logger.warn(it.message)
                }

                else -> finnEnhetForTemaOgOmraade(tema, geografiskTilknytning)
            }
        } else {
            return null
        }
    }

    private fun finnEnhetForTemaOgOmraade(tema: String, omraade: String) =
        norg2Klient.hentEnheterForOmraade(tema, omraade).firstOrNull() ?: throw IngenEnhetFunnetException(omraade, tema)

    private fun Saksbehandler.enheterIds() = this.enheter().map { it.id }

    private fun List<Sak>.filterForEnheter() =
        if (featureToggleService.isEnabled(SakServiceFeatureToggle.FiltrerMedEnhetId, false)) {
            when (val user = Kontekst.get().AppUser) {
                is Saksbehandler -> {
                    val enheter = user.enheterIds()

                    this.filter { it.enhet?.let { enhet -> enheter.contains(enhet) } ?: true }
                }

                else -> this
            }
        } else {
            this
        }

    private fun Sak?.sjekkEnhet() = this?.let { sak ->
        listOf(sak).filterForEnheter().firstOrNull()
    }
}