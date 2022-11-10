import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'
import { AvdoedForelder } from './personer/AvdoedForelder'
import { GjenlevendeForelder } from './personer/GjenlevendeForelder'
import { Barn } from './personer/Barn'
import { ContentHeader } from '~shared/styled'
import { Border, DashedBorder } from '../styled'
import {
  GyldigFramsattType,
  IDetaljertBehandling,
  IGyldighetproving,
  VurderingsResultat,
} from '~store/reducers/BehandlingReducer'
import { SoeskenListe } from './personer/Soesken'

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const Familieforhold: React.FC<PropsFamilieforhold> = ({ behandling }) => {
  const innsenderErGjenlevende =
    behandling.gyldighetsprøving?.vurderinger.find(
      (g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER
    )?.resultat === VurderingsResultat.OPPFYLT

  if (behandling.familieforhold == null || behandling.søker == null) {
    return <div style={{ color: 'red' }}>Familieforhold kan ikke hentes ut</div>
  }

  const doedsdato = behandling.familieforhold.avdoede.opplysning.doedsdato

  return (
    <>
      {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT ? (
        <>
          <ContentHeader>
            <Heading spacing size="medium" level="5">
              Familieforhold
            </Heading>
          </ContentHeader>
          <FamilieforholdWrapper>
            <Barn person={behandling.søker} doedsdato={doedsdato} />
            <DashedBorder />
            <GjenlevendeForelder
              person={behandling.familieforhold.gjenlevende.opplysning}
              innsenderErGjenlevendeForelder={innsenderErGjenlevende}
              doedsdato={doedsdato}
            />
            <AvdoedForelder person={behandling.familieforhold.avdoede.opplysning} />
            <br />
            <DashedBorder />
            <SoeskenListe soekerFnr={behandling.søker.foedselsnummer} familieforhold={behandling.familieforhold!!} />
          </FamilieforholdWrapper>
          <Border />
        </>
      ) : (
        <>
          <FamilieforholdWrapper>
            <GjenlevendeForelder
              person={behandling.familieforhold?.gjenlevende.opplysning}
              innsenderErGjenlevendeForelder={innsenderErGjenlevende}
              doedsdato={doedsdato}
            />
          </FamilieforholdWrapper>
          <Border />
        </>
      )}
    </>
  )
}

export const FamilieforholdWrapper = styled.div`
  padding: 0em 5em;
`
