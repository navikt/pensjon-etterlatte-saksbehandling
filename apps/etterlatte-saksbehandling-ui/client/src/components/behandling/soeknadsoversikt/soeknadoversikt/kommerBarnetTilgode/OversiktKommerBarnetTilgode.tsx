import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { Beskrivelse, InfobokserWrapper, InfoWrapper, VurderingsContainerWrapper } from '../../styled'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { IPdlPerson } from '~shared/types/Person'
import { formaterKildePdl, svarTilStatusIcon } from '../../utils'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Info } from '../../Info'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LeggTilVurderingButton'
import { useState } from 'react'
import { Grunnlagsopplysning } from '~shared/types/grunnlag'
import { KildePdl } from '~shared/types/kilde'
import { IAdresse } from '~shared/types/IAdresse'

interface AdresseProps {
  label: string
  adresse: IAdresse
  kilde?: string
}

const AdresseKort = (props: AdresseProps) => {
  const adresse = `${props.adresse.adresseLinje1}, ${props.adresse.postnr} ${props.adresse.poststed || ''}`
  return (
    <InfoWrapper>
      <Info label={props.label} tekst={adresse} undertekst={props.kilde} />
    </InfoWrapper>
  )
}

interface Props {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  soeker: IPdlPerson | undefined
  gjenlevendeForelder: Grunnlagsopplysning<IPdlPerson, KildePdl> | undefined
  redigerbar: boolean
  behandlingId: string
}

export const OversiktKommerBarnetTilgode = ({
  kommerBarnetTilgode,
  redigerbar,
  soeker,
  gjenlevendeForelder,
  behandlingId,
}: Props) => {
  const [vurdert, setVurdert] = useState(kommerBarnetTilgode !== null)
  const bostedsadresse = soeker?.bostedsadresse?.find((adresse) => adresse.aktiv)
  const foreldersadresse = gjenlevendeForelder?.opplysning?.bostedsadresse?.find((adresse) => adresse.aktiv)

  return (
    <LovtekstMedLenke
      tittel="Kommer pensjonen barnet tilgode?"
      hjemler={[
        { lenke: 'https://lovdata.no/lov/1997-02-28-19/§18-1', tittel: 'Folketrygdloven § 18-1' },
        { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-1', tittel: 'Folketrygdloven § 22-1' },
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
            <AdresseKort label="Barnets adresse" adresse={bostedsadresse} kilde={bostedsadresse?.kilde} />
          )}
          {foreldersadresse && (
            <AdresseKort
              label="Gjenlevende forelders adresse"
              adresse={foreldersadresse}
              kilde={formaterKildePdl(gjenlevendeForelder?.kilde)}
            />
          )}
        </InfobokserWrapper>
      </div>

      <VurderingsContainerWrapper>
        {vurdert ? (
          <KommerBarnetTilGodeVurdering
            kommerBarnetTilgode={kommerBarnetTilgode}
            redigerbar={redigerbar}
            setVurdert={(visVurderingKnapp: boolean) => setVurdert(visVurderingKnapp)}
            behandlingId={behandlingId}
          />
        ) : (
          <LeggTilVurderingButton onClick={() => setVurdert(true)}>Legg til vurdering</LeggTilVurderingButton>
        )}
      </VurderingsContainerWrapper>
    </LovtekstMedLenke>
  )
}
