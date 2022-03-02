import { useState } from 'react'
import { Detail, Heading, RadioGroup, Radio, Textarea, Button, Link } from '@navikt/ds-react'
import { InfoWrapper, DetailWrapper, HeadingWrapper, RadioGroupWrapper } from './styled'
import { IPersonFraSak, PersonStatus, RelatertPersonsRolle } from './types'
import { Content, ContentHeader } from '../../../shared/styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../behandlings-type'
import { PersonInfo } from './PersonInfo'
import { format } from 'date-fns'
import { usePersonInfoFromBehandling } from './usePersonInfoFromBehandling'

export const Personopplysninger = () => {
  const [soeknadGyldigBegrunnelse, setSoeknadGyldigBegrunnelse] = useState('')

  const {
    sosken,
    soekerPdl,
    mottattDato,
    dodsfall,
    avdodPersonPdl,
    avdodPersonSoknad,
    innsender,
    gjenlevendePdl,
    gjenlevendeSoknad,
  } = usePersonInfoFromBehandling()

  const soskenListe: IPersonFraSak[] = sosken?.opplysning.soesken.map((opplysning: any) => {
    return {
      navn: `${opplysning.fornavn} ${opplysning.etternavn}`,
      personStatus: PersonStatus.ETTERLATT,
      rolle: RelatertPersonsRolle.BARN,
      adressenavn: 'annet',
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
            <Detail size="small">Mottaker</Detail>
            <Detail size="medium" className="detail">
              {soekerPdl.opplysning.fornavn} {soekerPdl.opplysning.etternavn} ({soekerPdl?.opplysning.fornavn} {})
            </Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Søknad mottatt</Detail>
            <Detail size="medium" className="detail">
              {format(new Date(mottattDato?.opplysning.mottattDato), 'dd.MM.yyyy')}
            </Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Dato for dødsfall</Detail>
            <Detail size="medium" className="detail">
              {format(new Date(dodsfall?.opplysning.doedsdato), 'dd.MM.yyyy')}
            </Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Avdøde</Detail>
            <Detail size="medium" className="detail">
              {avdodPersonPdl?.opplysning.fornavn} {avdodPersonPdl?.opplysning.etternavn} (
              {avdodPersonSoknad?.opplysning.fornavn} {avdodPersonSoknad?.opplysning.etternavn})
            </Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Søknad fremsatt av</Detail>
            <Detail size="medium" className="detail">
              {innsender?.opplysning.fornavn} {innsender?.opplysning.etternavn}
            </Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Foreldreansvar</Detail>
            <Detail size="medium" className="detail">
              {gjenlevendePdl?.opplysning.fornavn} {gjenlevendePdl?.opplysning.etternavn} (
              {gjenlevendeSoknad?.opplysning.fornavn} {gjenlevendeSoknad?.opplysning.etternavn})
            </Detail>
          </DetailWrapper>
        </InfoWrapper>

        <Heading spacing size="small" level="5">
          Familieforhold
        </Heading>
        <PersonInfo
          person={{
            navn: `${soekerPdl?.opplysning.fornavn} ${soekerPdl?.opplysning.etternavn}`,
            personStatus: PersonStatus.ETTERLATT,
            rolle: RelatertPersonsRolle.BARN,
            adressenavn: 'annet',
            fnr: soekerPdl?.opplysning.foedselsnummer,
          }}
        />
        <PersonInfo
          person={{
            navn: `${gjenlevendePdl?.opplysning.fornavn} ${gjenlevendePdl?.opplysning.etternavn}`,
            personStatus: PersonStatus.LEVENDE,
            rolle: RelatertPersonsRolle.FAR,
            adressenavn: 'annet',
            fnr: gjenlevendePdl?.opplysning.foedselsnummer,
          }}
        />
        <PersonInfo
          person={{
            navn: `${avdodPersonPdl?.opplysning.fornavn} ${avdodPersonPdl?.opplysning.etternavn} (${avdodPersonSoknad?.opplysning.fornavn} ${avdodPersonSoknad?.opplysning.etternavn})`,
            personStatus: PersonStatus.DØD,
            rolle: RelatertPersonsRolle.FAR,
            adressenavn: 'annet',
            fnr: `${avdodPersonPdl?.opplysning.foedselsnummer} (${avdodPersonSoknad?.opplysning.foedselsnummer})`,
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
          <Button variant="primary" size="medium" className="button">
            Bekreft og gå videre
          </Button>
          <Link href="#" className="link">
            Avbryt og behandle i pesys
          </Link>
        </RadioGroupWrapper>
      </ContentHeader>
    </Content>
  )
}
