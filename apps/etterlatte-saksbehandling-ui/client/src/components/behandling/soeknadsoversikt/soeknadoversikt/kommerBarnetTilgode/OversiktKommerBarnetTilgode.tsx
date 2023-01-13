import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { InfobokserWrapper, InfoWrapper, SoeknadOversiktWrapper, VurderingsWrapper } from '../../styled'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { Heading } from '@navikt/ds-react'

interface Props {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  redigerbar: boolean
}

export const OversiktKommerBarnetTilgode = ({ kommerBarnetTilgode, redigerbar }: Props) => (
  <>
    <Heading size={'medium'} level={'2'}>
      Vurdering om pensjonen kommer barnet til gode
    </Heading>
    <SoeknadOversiktWrapper>
      <InfobokserWrapper>
        <InfoWrapper>Har barnet samme adresse som den gjenlevende forelder?</InfoWrapper>
      </InfobokserWrapper>
      <VurderingsWrapper>
        <KommerBarnetTilGodeVurdering kommerBarnetTilgode={kommerBarnetTilgode} redigerbar={redigerbar} />
      </VurderingsWrapper>
    </SoeknadOversiktWrapper>
  </>
)
