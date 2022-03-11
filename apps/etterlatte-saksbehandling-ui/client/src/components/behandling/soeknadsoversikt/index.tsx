import { useState, useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { Detail, Heading, RadioGroup, Radio, Textarea, Button } from '@navikt/ds-react'
import { InfoWrapper, DetailWrapper, HeadingWrapper, RadioGroupWrapper, Border, WarningText } from './styled'
import { IPersonFraSak, PersonStatus, RelatertPersonsRolle } from './types'
import { Content, ContentHeader } from '../../../shared/styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../behandlings-type'
import { PersonInfo } from './PersonInfo'
import { format } from 'date-fns'
import { usePersonInfoFromBehandling } from './usePersonInfoFromBehandling'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { hentAlderVedDoedsdato, sjekkDataFraSoeknadMotPdl, WarningAlert } from './utils'
import {
  IKriterie,
  Kriterietype,
  VilkaarsType,
  VilkaarVurderingsResultat,
} from '../../../store/reducers/BehandlingReducer'

export const Soeknadsoversikt = () => {
  const [soeknadGyldigBegrunnelse, setSoeknadGyldigBegrunnelse] = useState('')
  const { next } = useBehandlingRoutes()
  const ctx = useContext(AppContext)
  const vilkaar = ctx.state.behandlingReducer.vilkårsprøving

  const {
    sosken,
    soekerPdl,
    soekerSoknad,
    mottattDato,
    dodsfall,
    avdodPersonPdl,
    avdodPersonSoknad,
    innsender,
    gjenlevendePdl,
    gjenlevendeSoknad,
    soekerBostedadresserPdl,
    avdoedBostedadresserPdl,
    gjenlevendeBostedadresserPdl,
    soekerFoedseldato,
    gjenlevendeForelderInfo,
  } = usePersonInfoFromBehandling()

  const doedsfallVilkaar: any = vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.DOEDSFALL_ER_REGISTRERT)
  const avdoedErForelderVilkaar =
    doedsfallVilkaar.kriterier.find((krit: IKriterie) => krit.navn === Kriterietype.AVDOED_ER_FORELDER).resultat ===
    VilkaarVurderingsResultat.OPPFYLT
  const avdoedErLikISoeknad = dodsfall?.opplysning.foedselsnummer === avdodPersonSoknad.opplysning.foedselsnummer

  const soskenListe: IPersonFraSak[] = sosken?.opplysning.soesken.map((opplysning: any) => {
    return {
      navn: `${opplysning.fornavn} ${opplysning.etternavn}`,
      personStatus: PersonStatus.ETTERLATT,
      rolle: RelatertPersonsRolle.BARN,
      fnr: '332',
    }
  })

  return (
    <Content>
      <ContentHeader>
        <h1>Søknadsoversikt</h1>
        <HeadingWrapper>
          <Heading spacing size="small" level="5">
            Om søknaden
          </Heading>
          <div className="details">
            <BehandlingsStatusSmall status={IBehandlingsStatus.FORSTEGANG} />
            <BehandlingsTypeSmall type={IBehandlingsType.BARNEPENSJON} />
          </div>
        </HeadingWrapper>

        <InfoWrapper>
          <DetailWrapper>
            <Detail size="medium">Mottaker</Detail>
            {sjekkDataFraSoeknadMotPdl(
              `${soekerPdl?.opplysning.fornavn} ${soekerPdl?.opplysning.etternavn}`,
              `${soekerSoknad?.opplysning.fornavn} ${soekerSoknad?.opplysning.etternavn}`
            )}
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="medium">Avdød forelder</Detail>
            {avdoedErForelderVilkaar ? (
              sjekkDataFraSoeknadMotPdl(
                `${avdodPersonPdl?.opplysning.fornavn} ${avdodPersonPdl?.opplysning.etternavn}`,
                `${avdodPersonSoknad?.opplysning.fornavn} ${avdodPersonSoknad?.opplysning.etternavn}`
              )
            ) : (
              <WarningText>Ingen foreldre er død</WarningText>
            )}
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="medium">Søknad fremsatt av</Detail>
            {innsender?.opplysning.fornavn} {innsender?.opplysning.etternavn}
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="medium">Søknad mottatt</Detail>
            {format(new Date(mottattDato?.opplysning.mottattDato), 'dd.MM.yyyy')}
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="medium">Dato for dødsfall</Detail>
            {format(new Date(dodsfall?.opplysning.doedsdato), 'dd.MM.yyyy')}
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="medium">Første mulig virkningstidspunkt</Detail>
            01.01.22
          </DetailWrapper>
          {avdoedErForelderVilkaar &&
            !avdoedErLikISoeknad &&
            WarningAlert(
              `I PDL er det oppgitt ${avdodPersonPdl?.opplysning.fornavn} ${avdodPersonPdl?.opplysning.etternavn} som avdød forelder, men i søknad er det oppgitt ${avdodPersonSoknad?.opplysning.fornavn} ${avdodPersonSoknad?.opplysning.etternavn}. Må avklares.`
            )}
          {!avdoedErForelderVilkaar && WarningAlert('Oppgitt avdød i søknad er ikke forelder til barnet. Må avklares.')}
        </InfoWrapper>

        <Heading spacing size="small" level="5">
          Familieforhold
        </Heading>
        <Border />
        <PersonInfo
          person={{
            navn: `${soekerPdl?.opplysning.fornavn} ${soekerPdl?.opplysning.etternavn}`,
            personStatus: PersonStatus.BARN,
            rolle: RelatertPersonsRolle.BARN,
            adresser: soekerBostedadresserPdl?.opplysning.bostedadresse,
            fnr: soekerPdl?.opplysning.foedselsnummer,
            fnrFraSoeknad: soekerSoknad?.opplysning.foedselsnummer,
            statsborgerskap: 'NO',
            alderEtterlatt: hentAlderVedDoedsdato(
              soekerFoedseldato?.opplysning.foedselsdato,
              dodsfall?.opplysning.doedsdato
            ),
          }}
        />
        <PersonInfo
          person={{
            navn: `${gjenlevendePdl?.opplysning.fornavn} ${gjenlevendePdl?.opplysning.etternavn}`,
            personStatus: PersonStatus.GJENLEVENDE_FORELDER,
            rolle: RelatertPersonsRolle.FORELDER,
            adresser: gjenlevendeBostedadresserPdl?.opplysning.bostedadresse,
            fnr: gjenlevendePdl?.opplysning.foedselsnummer,
            fnrFraSoeknad: gjenlevendeSoknad?.opplysning.foedselsnummer,
            adresseFraSoeknad: gjenlevendeForelderInfo?.opplysning.adresse,
            statsborgerskap: 'NO',
          }}
        />
        <PersonInfo
          person={{
            navn: `${avdodPersonPdl?.opplysning.fornavn} ${avdodPersonPdl?.opplysning.etternavn}`,
            personStatus: PersonStatus.AVDOED,
            rolle: RelatertPersonsRolle.FORELDER,
            adresser: avdoedBostedadresserPdl?.opplysning.bostedadresse,
            fnr: `${avdodPersonPdl?.opplysning.foedselsnummer}`,
            fnrFraSoeknad: avdodPersonSoknad?.opplysning.foedselsnummer,
            datoForDoedsfall: dodsfall?.opplysning.doedsdato,
            statsborgerskap: 'NO',
          }}
        />

        {soskenListe?.map((person) => (
          <PersonInfo key={`soesken_`} person={person} />
        ))}
        <RadioGroupWrapper>
          <RadioGroup legend="Er søknaden gyldig fremsatt?" size="small" className="radioGroup">
            <Radio value="10">Ja</Radio>
            <Radio value="20">Nei</Radio>
          </RadioGroup>
          <Textarea
            label="Begrunnelse (hvis aktuelt)"
            value={soeknadGyldigBegrunnelse}
            onChange={(e) => setSoeknadGyldigBegrunnelse(e.target.value)}
            minRows={2}
            maxLength={400}
            size="small"
          />
          <Button variant="primary" size="medium" className="button" onClick={next}>
            Lagre
          </Button>
        </RadioGroupWrapper>
      </ContentHeader>
    </Content>
  )
}
