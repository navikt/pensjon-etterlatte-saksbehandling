import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { Beskrivelse, InfoWrapper, InfobokserWrapper, VurderingsContainerWrapper } from '../../styled'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { IPdlPerson } from '~shared/types/Person'
import { formaterKildePdl, svarTilStatusIcon } from '../../utils'
import { Soeknadsvurdering } from '../SoeknadsVurdering'
import { Info } from '../../Info'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LeggTilVurderingButton'
import { useState } from 'react'
import { Grunnlagsopplysning } from '~shared/types/Grunnlagsopplysning'
import { KildePdl } from '~shared/types/kilde'

interface AdresseProps {
  label: string
  adresse: string
  kilde?: string
}

const AdresseKort = (props: AdresseProps) => (
  <InfoWrapper>
    <Info label={props.label} tekst={props.adresse} undertekst={props.kilde} />
  </InfoWrapper>
)

interface Props {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  søker: IPdlPerson | undefined
  forelder: Grunnlagsopplysning<IPdlPerson, KildePdl> | undefined
  redigerbar: boolean
  behandlingId: string
}

export const OversiktKommerBarnetTilgode = ({
  kommerBarnetTilgode,
  redigerbar,
  søker,
  forelder,
  behandlingId,
}: Props) => {
  const [vurder, setVurder] = useState(kommerBarnetTilgode !== null)
  const bostedsadresse = søker?.bostedsadresse?.find((adresse) => adresse.aktiv === true)
  const foreldersadresse = forelder?.opplysning?.bostedsadresse?.find((adresse) => adresse.aktiv === true)

  return (
    <Soeknadsvurdering
      tittel="Kommer pensjonen barnet tilgode?"
      hjemler={[
        { lenke: 'https://lovdata.no/lov/1997-02-28-19/§18-1', tittel: 'Folketrygdeloven § 18-1' },
        { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-1', tittel: 'Folketrygdeloven § 22-1' },
        { lenke: 'https://lovdata.no/lov/2010-03-26-9/§9', tittel: 'Vergemålsloven § 9, § 16 og § 19' },
        { lenke: 'https://lovdata.no/lov/1981-04-08-7/§30', tittel: 'Barneloven § 30 og § 38' },
      ]}
      status={svarTilStatusIcon(kommerBarnetTilgode?.svar)}
    >
      <div>
        <Beskrivelse>
          Undersøk om boforholdet er avklart og det er sannsynlig at pensjonen kommer barnet til gode.
        </Beskrivelse>

        <InfobokserWrapper>
          {bostedsadresse && (
            <AdresseKort label="Barnets adresse" adresse={bostedsadresse.adresseLinje1} kilde={bostedsadresse?.kilde} />
          )}
          {foreldersadresse && (
            <AdresseKort
              label="Forelders adresse"
              adresse={foreldersadresse.adresseLinje1}
              kilde={formaterKildePdl(forelder?.kilde)}
            />
          )}
        </InfobokserWrapper>
      </div>

      <VurderingsContainerWrapper>
        {vurder ? (
          <KommerBarnetTilGodeVurdering
            kommerBarnetTilgode={kommerBarnetTilgode}
            redigerbar={redigerbar}
            setVurder={(visVurderingKnapp: boolean) => setVurder(visVurderingKnapp)}
            behandlingId={behandlingId}
          />
        ) : (
          <LeggTilVurderingButton onClick={() => setVurder(true)}>Legg til vurdering</LeggTilVurderingButton>
        )}
      </VurderingsContainerWrapper>
    </Soeknadsvurdering>
  )
}
