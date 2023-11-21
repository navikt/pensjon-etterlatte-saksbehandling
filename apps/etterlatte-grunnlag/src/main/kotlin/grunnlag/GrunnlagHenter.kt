package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.grunnlag.adresse.VergeAdresse
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import java.util.UUID

class GrunnlagHenter(
    private val pdltjenesterKlient: PdlTjenesterKlientImpl,
    private val persondataKlient: PersondataKlient,
) {
    suspend fun hentGrunnlagsdata(opplysningsbehov: Opplysningsbehov): HentetGrunnlag {
        return coroutineScope {
            val persongalleri = opplysningsbehov.persongalleri
            val sakType = opplysningsbehov.sakType
            val requesterAvdoed =
                persongalleri.avdoed.map {
                    hentPersonAsync(it, PersonRolle.AVDOED, sakType)
                }
            val requesterGjenlevende =
                persongalleri.gjenlevende.map {
                    hentPersonAsync(it, PersonRolle.GJENLEVENDE, opplysningsbehov.sakType)
                }
            val soeker = hentPersonAsync(persongalleri.soeker, soekerRolle(sakType), opplysningsbehov.sakType)
            val innsender =
                persongalleri.innsender
                    ?.takeIf { it != Vedtaksloesning.PESYS.name }
                    ?.let { innsenderFnr ->
                        hentPersonAsync(innsenderFnr, PersonRolle.INNSENDER, sakType)
                    }

            val soekerPersonInfo =
                soeker.let { (person, personDTO) ->
                    personopplysning(person, personDTO, Opplysningstype.SOEKER_PDL_V1, soekerRolle(sakType))
                }
            val vergeadresserRequester = hentVergeadresserAsync(soekerPersonInfo)

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

            val vergeAdresseMap =
                vergeadresserRequester.associate { (fnr, adresse) -> fnr to adresse.await() }
                    .filter { it.value != null }

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

            val saksopplysninger = mutableListOf(opplysningsbehov.persongalleri.tilGrunnlagsopplysning())
            if (vergeAdresseMap.isNotEmpty()) {
                saksopplysninger.add(vergeAdresserOpplysning(vergeAdresseMap))
            }

            HentetGrunnlag(personopplysninger, saksopplysninger)
        }
    }

    private fun CoroutineScope.hentVergeadresserAsync(soekerPersonInfo: GrunnlagsopplysningerPersonPdl) =
        (soekerPersonInfo.person.vergemaalEllerFremtidsfullmakt ?: emptyList())
            .mapNotNull { it.vergeEllerFullmektig.motpartsPersonident }
            .map { Pair(it, async { hentVergeadresse(it) }) }

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

    private fun hentVergeadresse(folkeregisteridentifikator: Folkeregisteridentifikator): VergeAdresse? {
        return persondataKlient.hentAdresseForVerge(folkeregisteridentifikator)?.toVergeAdresse()
    }

    private fun vergeAdresserOpplysning(vergeadresserMap: Map<Folkeregisteridentifikator, VergeAdresse?>): Grunnlagsopplysning<JsonNode> =
        Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde =
                Grunnlagsopplysning.Persondata(
                    tidspunktForInnhenting = Tidspunkt.now(),
                    registersReferanse = null,
                    opplysningId = null,
                ),
            opplysningType = Opplysningstype.VERGES_ADRESSER,
            meta = objectMapper.createObjectNode(),
            opplysning = vergeadresserMap.toJsonNode(),
            fnr = null,
            periode = null,
        )

    private fun Persongalleri.tilGrunnlagsopplysning(): Grunnlagsopplysning<JsonNode> {
        return Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde =
                if (this.innsender == Vedtaksloesning.PESYS.name) {
                    Grunnlagsopplysning.Pesys.create()
                } else {
                    Grunnlagsopplysning.Privatperson(this.innsender!!, Tidspunkt.now())
                },
            opplysningType = Opplysningstype.PERSONGALLERI_V1,
            meta = objectMapper.createObjectNode(),
            opplysning = this.toJsonNode(),
            attestering = null,
            fnr = null,
            periode = null,
        )
    }
}
