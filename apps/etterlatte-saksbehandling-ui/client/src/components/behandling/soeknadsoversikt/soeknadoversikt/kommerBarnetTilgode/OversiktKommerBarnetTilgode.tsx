import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { Beskrivelse, InfoWrapper, InfobokserWrapper, VurderingsContainerWrapper } from '../../styled'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { IPdlPerson } from '~shared/types/Person'
import { svarTilVurderingsstatus } from '../../utils'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { JaNei } from '~shared/types/ISvar'
import { Soeknadsvurdering } from '../SoeknadsVurdering'
import { Info } from '../../Info'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LeggTilVurderingButton'
import { useState } from 'react'

interface AdresseProps {
  label: string
  adresse: string
}

const AdresseKort = (props: AdresseProps) => (
  <InfoWrapper>
    <Info label={props.label} tekst={props.adresse} />
  </InfoWrapper>
)

interface Props {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  søker: IPdlPerson | undefined
  forelder: IPdlPerson | undefined
  redigerbar: boolean
}

export const OversiktKommerBarnetTilgode = ({ kommerBarnetTilgode, redigerbar, søker, forelder }: Props) => {
  const [vurder, setVurder] = useState(kommerBarnetTilgode !== null)
  const bostedsadresse = søker?.bostedsadresse?.find((adresse) => adresse.aktiv === true)
  const foreldersadresse = forelder?.bostedsadresse?.find((adresse) => adresse.aktiv === true)

  return (
    <Soeknadsvurdering
      tittel="Kommer pensjonen barnet tilgode?"
      vurderingsResultat={
        kommerBarnetTilgode?.svar
          ? svarTilVurderingsstatus(kommerBarnetTilgode.svar)
          : VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
      }
      hjemler={[
        { lenke: 'https://lovdata.no/lov/1997-02-28-19/§18-1', tittel: 'Folketrygdeloven § 18-1 og § 22-1' },
        { lenke: 'https://lovdata.no/lov/2010-03-26-9/§9', tittel: 'Vergemålsloven § 9, § 16 og § 19' },
        { lenke: 'https://lovdata.no/lov/1981-04-08-7/§30', tittel: 'Barneloven § 30 og § 38' },
      ]}
      status={kommerBarnetTilgode?.svar === JaNei.JA ? 'success' : 'warning'}
    >
      <div>
        <Beskrivelse>
          Undersøk om boforholdet er avklart og det er sannsynlig at pensjonen kommer barnet til gode.
        </Beskrivelse>

        <InfobokserWrapper>
          {bostedsadresse && <AdresseKort label="Barnets adresse" adresse={bostedsadresse.adresseLinje1} />}
          {foreldersadresse && <AdresseKort label="Forelders adresse" adresse={foreldersadresse.adresseLinje1} />}
        </InfobokserWrapper>
      </div>

      <VurderingsContainerWrapper>
        {vurder ? (
          <KommerBarnetTilGodeVurdering
            kommerBarnetTilgode={kommerBarnetTilgode}
            redigerbar={redigerbar}
            setVurder={(visVurderingKnapp: boolean) => setVurder(visVurderingKnapp)}
          />
        ) : (
          <LeggTilVurderingButton onClick={() => setVurder(true)}>Legg til vurdering</LeggTilVurderingButton>
        )}
      </VurderingsContainerWrapper>
    </Soeknadsvurdering>
  )
}
