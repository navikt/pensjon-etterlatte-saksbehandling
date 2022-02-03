import { useEffect, useState } from 'react'
import { Detail, Heading, RadioGroup, Radio, Textarea, Button, Link } from '@navikt/ds-react'
import { InfoWrapper, DetailWrapper, HeadingWrapper, RadioGroupWrapper } from './styled'
import { IPersonFraSak } from './types'
import { hentPersonerMedRelasjon } from '../../../shared/api/personopplysninger'
import { Content, ContentHeader } from '../../../shared/styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../behandlings-type'
import { PersonInfo } from './PersonInfo'

export const Personopplysninger = () => {
  const [personer, setPersoner] = useState<{ person: IPersonFraSak; foreldre: IPersonFraSak[] }>()
  const [soeknadGyldigBegrunnelse, setSoeknadGyldigBegrunnelse] = useState("")

  useEffect(() => {
    //TODO: Henter info om barn og foreldre fra PDL, type IPersonFraRegister er det som trengs per dags dato fra sketchene.
    hentPersonerMedRelasjon().then(
      (personer: { person: IPersonFraSak; foreldre: IPersonFraSak[]}) => {
        setPersoner(personer)
      }
    )
  }, [])

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
            <Detail size="medium" className="detail">Lille My</Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Søknad mottatt</Detail>
            <Detail size="medium" className="detail">21.12.21</Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Dato for dødsfall</Detail>
            <Detail size="medium" className="detail">21.12.21</Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Avdøde</Detail>
            <Detail size="medium" className="detail">Ola Nordman (far)</Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Søknad fremsatt av</Detail>
            <Detail size="medium" className="detail">Gjenlevende mor</Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Foreldreansvar</Detail>
            <Detail size="medium" className="detail">Gjenlevende mor</Detail>
          </DetailWrapper>
        </InfoWrapper>

        <Heading spacing size="small" level="5">
          Familieforhold
        </Heading>
        {personer && (
          <>
            <PersonInfo person={personer.person} />
            {personer.foreldre.map((foreldre, key) => (
              <PersonInfo key={key} person={foreldre} />
            ))}      
          </>
        )}
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
        <Link href="#" className="link">Avbryt og behandle i pesys</Link>
        </RadioGroupWrapper>
      </ContentHeader>
    </Content>
  )
}
