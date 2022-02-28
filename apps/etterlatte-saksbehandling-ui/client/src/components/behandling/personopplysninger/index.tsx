import { useContext, useEffect, useState } from 'react'
import { Detail, Heading, RadioGroup, Radio, Textarea, Button, Link } from '@navikt/ds-react'
import { InfoWrapper, DetailWrapper, HeadingWrapper, RadioGroupWrapper } from './styled'
import { IPersonFraSak } from './types'
import { hentPersonerMedRelasjon } from '../../../shared/api/personopplysninger'
import { Content, ContentHeader } from '../../../shared/styled'
import { BehandlingsStatusSmall, IBehandlingsStatus } from '../behandlings-status'
import { BehandlingsTypeSmall, IBehandlingsType } from '../behandlings-type'
import { PersonInfo } from './PersonInfo'
import { AppContext } from '../../../store/AppContext'
import { OpplysningsType } from '../inngangsvilkaar/types'
import { KildeType } from '../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'




export const Personopplysninger = () => {
  const ctx = useContext(AppContext);
  const [personer, setPersoner] = useState<{ person: IPersonFraSak; foreldre: IPersonFraSak[] }>()
  const [soeknadGyldigBegrunnelse, setSoeknadGyldigBegrunnelse] = useState("")

  const grunnlag = ctx.state.behandlingReducer.grunnlag;

  const pdlPerson: any = grunnlag.find(g => g.opplysningType === OpplysningsType.soeker_personinfo && g.kilde.type === KildeType.pdl);
  const privatPerson: any = grunnlag.find(g => g.opplysningType === OpplysningsType.soeker_personinfo && g.kilde.type === KildeType.privatperson);
  const avdodPerson: any = grunnlag.find(g => g.opplysningType === OpplysningsType.avdoed_personinfo);
  const mottattDato = grunnlag.find(g => g.opplysningType === OpplysningsType.soeknad_mottatt);
  const sosken = grunnlag.find(g => g.opplysningType === OpplysningsType.relasjon_soksken);
  const dodsfall = grunnlag.find(g => g.opplysningType === OpplysningsType.avdoed_doedsfall);
  // const omsorg = grunnlag.find(g => g.opplysningType === OpplysningsType.omsorg);
  
  useEffect(() => {
    //TODO: Henter info om barn og foreldre fra PDL, type IPersonFraRegister er det som trengs per dags dato fra sketchene.
    hentPersonerMedRelasjon().then(
      (personer: { person: IPersonFraSak; foreldre: IPersonFraSak[]}) => {
        setPersoner(personer)
      }
    )
  }, [])
  

  console.log(grunnlag);
  console.log(sosken);
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
            <Detail size="medium" className="detail">{privatPerson.opplysning.fornavn} {privatPerson.opplysning.etternavn} ({pdlPerson?.opplysning.fornavn} {})</Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Søknad mottatt</Detail>
            <Detail size="medium" className="detail">{format(new Date(mottattDato?.opplysning.mottattDato), "dd.MM.yyyy")}</Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Dato for dødsfall</Detail>
            <Detail size="medium" className="detail">{format(new Date(dodsfall?.opplysning.doedsdato), "dd.MM.yyyy")}</Detail>
          </DetailWrapper>
          <DetailWrapper>
            <Detail size="small">Avdøde</Detail>
            <Detail size="medium" className="detail">{avdodPerson?.opplysning.fornavn} {avdodPerson?.opplysning.etternavn}</Detail>
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
