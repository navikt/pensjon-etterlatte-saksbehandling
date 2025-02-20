package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import org.slf4j.LoggerFactory
import java.util.UUID

class GrunnlagHenter(
    private val pdltjenesterKlient: PdlTjenesterKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentGrunnlagsdata(opplysningsbehov: Opplysningsbehov): HentetGrunnlag =
        coroutineScope {
            val persongalleri = opplysningsbehov.persongalleri
            val persongalleriFraPdl =
                pdltjenesterKlient.hentPersongalleri(
                    opplysningsbehov.persongalleri.soeker.value,
                    opplysningsbehov.sakType,
                    opplysningsbehov.persongalleri.innsender?.value,
                )
            val sakType = opplysningsbehov.sakType
            val requesterAvdoed =
                persongalleri.avdoed.map {
                    hentPersonAsync(it.value, PersonRolle.AVDOED, sakType)
                }
            val requesterGjenlevende =
                persongalleri.gjenlevende.map {
                    hentPersonAsync(it.value, PersonRolle.GJENLEVENDE, opplysningsbehov.sakType)
                }
            val soeker = hentPersonAsync(persongalleri.soeker.value, soekerRolle(sakType), opplysningsbehov.sakType)
            val innsender =
                persongalleri.innsender
                    ?.let { innsenderFnr ->
                        hentPersonAsync(innsenderFnr.value, PersonRolle.INNSENDER, sakType)
                    }

            val soekerPersonInfo =
                soeker.let { (person, personDTO) ->
                    personopplysning(person, personDTO, Opplysningstype.SOEKER_PDL_V1, soekerRolle(sakType))
                }

            val innsenderPersonInfo =
                innsender?.let { (person, personDTO) ->
                    personopplysning(person, personDTO, Opplysningstype.INNSENDER_PDL_V1, PersonRolle.INNSENDER)
                }
            val avdoedePersonInfo =
                requesterAvdoed.map { (person, personDTO) ->
                    personopplysning(person, personDTO, Opplysningstype.AVDOED_PDL_V1, PersonRolle.AVDOED)
                }
            val gjenlevendePersonInfo =
                requesterGjenlevende.map { (person, personDTO) ->
                    personopplysning(person, personDTO, GJENLEVENDE_FORELDER_PDL_V1, PersonRolle.GJENLEVENDE)
                }

            val opplysningList =
                listOfNotNull(soekerPersonInfo, innsenderPersonInfo)
                    .plus(gjenlevendePersonInfo)
                    .plus(avdoedePersonInfo)
            val personopplysninger =
                opplysningList.map {
                    it.personDto.foedselsnummer.verdi to
                        lagEnkelopplysningerFraPDL(
                            it.person,
                            it.personDto,
                            it.opplysningstype,
                            it.personDto.foedselsnummer.verdi,
                            it.personRolle,
                        )
                }

            val saksopplysninger =
                listOfNotNull(
                    opplysningsbehov.persongalleri.tilGrunnlagsopplysningFraSoeknad(overstyrtKilde = opplysningsbehov.kilde),
                    persongalleriFraPdl?.tilGrunnlagsopplysningFraPdl(),
                )

            HentetGrunnlag(personopplysninger, saksopplysninger)
        }

    private suspend fun personopplysning(
        person: Deferred<Person>,
        personDTO: Deferred<PersonDTO>,
        opplysningstype: Opplysningstype,
        rolle: PersonRolle,
    ) = GrunnlagsopplysningerPersonPdl(
        person.await(),
        personDTO.await(),
        opplysningstype,
        rolle,
    )

    private fun soekerRolle(sakType: SakType): PersonRolle {
        val soekerRolle =
            when (sakType) {
                SakType.OMSTILLINGSSTOENAD -> PersonRolle.GJENLEVENDE
                SakType.BARNEPENSJON -> PersonRolle.BARN
            }
        return soekerRolle
    }

    private fun CoroutineScope.hentPersonAsync(
        it: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): Pair<Deferred<Person>, Deferred<PersonDTO>> =
        Pair(
            async {
                pdltjenesterKlient.hentPerson(
                    it,
                    rolle,
                    sakType,
                )
            },
            async {
                pdltjenesterKlient.hentOpplysningsperson(
                    it,
                    rolle,
                    sakType,
                )
            },
        )

    private fun Persongalleri.tilGrunnlagsopplysningFraSoeknad(overstyrtKilde: Grunnlagsopplysning.Kilde): Grunnlagsopplysning<JsonNode> {
        val opplysningid = UUID.randomUUID()
        return Grunnlagsopplysning(
            id = opplysningid,
            kilde = overstyrtKilde,
            opplysningType = Opplysningstype.PERSONGALLERI_V1,
            meta = objectMapper.createObjectNode(),
            opplysning = this.toJsonNode(),
            attestering = null,
            fnr = null,
            periode = null,
        )
    }

    private fun Persongalleri.tilGrunnlagsopplysningFraPdl(): Grunnlagsopplysning<JsonNode> =
        Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
            opplysningType = Opplysningstype.PERSONGALLERI_PDL_V1,
            meta = objectMapper.createObjectNode(),
            opplysning = this.toJsonNode(),
            attestering = null,
            fnr = null,
            periode = null,
        )
}
