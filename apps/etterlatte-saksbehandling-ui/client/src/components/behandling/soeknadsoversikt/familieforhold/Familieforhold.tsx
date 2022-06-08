import styled from 'styled-components'
import { Heading } from '@navikt/ds-react'
import { hentAlderVedDoedsdato } from '../utils'
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

export interface PropsFamilieforhold {
  behandling: IDetaljertBehandling
}

export const Familieforhold: React.FC<PropsFamilieforhold> = ({ behandling }) => {
  const innsenderErGjenlevende =
    behandling.gyldighetsprøving.vurderinger.find(
      (g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER
    )?.resultat === VurderingsResultat.OPPFYLT
  const doedsdato = behandling.kommerSoekerTilgode.familieforhold.avdoed.doedsdato

  return (
    <>
      {behandling.gyldighetsprøving.resultat === VurderingsResultat.OPPFYLT ? (
        <>
          <ContentHeader>
            <Heading spacing size="medium" level="5">
              Familieforhold
            </Heading>
          </ContentHeader>
          <FamilieforholdWrapper>
            <DashedBorder />
            <Barn
              person={behandling.kommerSoekerTilgode.familieforhold.soeker}
              alderVedDoedsdato={hentAlderVedDoedsdato(
                behandling.kommerSoekerTilgode.familieforhold.soeker.foedselsdato,
                doedsdato
              )}
              doedsdato={doedsdato}
            />
            <DashedBorder />
            <GjenlevendeForelder
              person={behandling.kommerSoekerTilgode.familieforhold.gjenlevendeForelder}
              innsenderErGjenlevendeForelder={innsenderErGjenlevende}
              doedsdato={doedsdato}
            />
            <DashedBorder />
            <AvdoedForelder person={behandling.kommerSoekerTilgode.familieforhold.avdoed} />
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
