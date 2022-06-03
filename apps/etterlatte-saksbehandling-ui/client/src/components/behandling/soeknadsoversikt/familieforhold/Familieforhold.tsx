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
  IGyldighetproving,
  IGyldighetResultat,
  IKommerSoekerTilgode,
  VurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'

export interface PropsFamilieforhold {
  kommerSoekerTilgode: IKommerSoekerTilgode
  gyldigFramsatt: IGyldighetResultat
}

export const Familieforhold: React.FC<PropsFamilieforhold> = ({ kommerSoekerTilgode, gyldigFramsatt }) => {
  const innsenderErGjenlevende =
    gyldigFramsatt.vurderinger.find((g: IGyldighetproving) => g.navn === GyldigFramsattType.INNSENDER_ER_FORELDER)
      ?.resultat === VurderingsResultat.OPPFYLT

  return (
    <>
      {gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT ? (
        <>
          <ContentHeader>
            <Heading spacing size="medium" level="5">
              Familieforhold
            </Heading>
          </ContentHeader>
          <FamilieforholdWrapper>
            <DashedBorder />
            <Barn
              person={kommerSoekerTilgode.familieforhold.soeker}
              alderVedDoedsdato={hentAlderVedDoedsdato(
                kommerSoekerTilgode.familieforhold.soeker.foedselsdato,
                kommerSoekerTilgode.familieforhold.avdoed.doedsdato
              )}
            />
            <DashedBorder />
            <GjenlevendeForelder
              person={kommerSoekerTilgode.familieforhold.gjenlevendeForelder}
              innsenderErGjenlevendeForelder={innsenderErGjenlevende}
            />
            <DashedBorder />
            <AvdoedForelder person={kommerSoekerTilgode.familieforhold.avdoed} />
          </FamilieforholdWrapper>
          <Border />
        </>
      ) : (
        { innsenderErGjenlevende } && (
          <>
            <FamilieforholdWrapper>
              <GjenlevendeForelder
                person={kommerSoekerTilgode.familieforhold.gjenlevendeForelder}
                innsenderErGjenlevendeForelder={innsenderErGjenlevende}
              />
            </FamilieforholdWrapper>
            <Border />
          </>
        )
      )}
    </>
  )
}

export const FamilieforholdWrapper = styled.div`
  padding: 0em 5em;
`
