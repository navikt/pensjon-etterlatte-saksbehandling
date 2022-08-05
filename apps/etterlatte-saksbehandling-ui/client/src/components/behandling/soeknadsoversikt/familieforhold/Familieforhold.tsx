import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'
import { AvdoedForelder } from './personer/AvdoedForelder'
import { GjenlevendeForelder } from './personer/GjenlevendeForelder'
import { Barn } from './personer/Barn'
import { ContentHeader } from '../../../../shared/styled'
import { Border, DashedBorder } from '../styled'
import {
  GyldigFramsattType,
  IDetaljertBehandling,
  IGyldighetproving,
  VurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'
import {Soesken} from "./personer/Soesken";

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const Familieforhold: React.FC<PropsFamilieforhold> = ({ behandling }) => {
  const innsenderErGjenlevende =
    behandling.gyldighetsprøving?.vurderinger.find(
      (g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER
    )?.resultat === VurderingsResultat.OPPFYLT

  if (behandling.kommerSoekerTilgode == null) {
    return <div style={{ color: 'red' }}>Familieforhold kan ikke hentes ut</div>
  }

  const doedsdato = behandling.kommerSoekerTilgode.familieforhold.avdoed.doedsdato

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
            <Barn
              person={behandling.kommerSoekerTilgode.familieforhold.soeker}
              doedsdato={doedsdato}
            />
            <DashedBorder />
            <GjenlevendeForelder
              person={behandling.kommerSoekerTilgode.familieforhold.gjenlevendeForelder}
              innsenderErGjenlevendeForelder={innsenderErGjenlevende}
              doedsdato={doedsdato}
            />
            <AvdoedForelder person={behandling.kommerSoekerTilgode.familieforhold.avdoed} />
            <br />
            <DashedBorder />
            <Soesken
              soeker={behandling.kommerSoekerTilgode.familieforhold.soeker}
              avdoedesBarn={behandling.avdoedesBarn}
              avdoed={behandling.kommerSoekerTilgode.familieforhold.avdoed}
            />
          </FamilieforholdWrapper>
          <Border />
        </>
      ) : (
        <>
          <FamilieforholdWrapper>
            <GjenlevendeForelder
              person={behandling.kommerSoekerTilgode.familieforhold.gjenlevendeForelder}
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
