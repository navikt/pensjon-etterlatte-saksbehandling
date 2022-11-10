import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { IKommerBarnetTilgode } from '~store/reducers/BehandlingReducer'
import { Header, InfobokserWrapper, InfoWrapper, SoeknadOversiktWrapper, VurderingsWrapper } from '../../styled'

interface Props {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
}

export const OversiktKommerBarnetTilgode: React.FC<Props> = ({ kommerBarnetTilgode }) => (
  <>
    <Header>Vurdering om pensjonen kommer barnet til gode</Header>
    <SoeknadOversiktWrapper>
      <InfobokserWrapper>
        <InfoWrapper>Har barnet samme adresse som den gjenlevende forelder?</InfoWrapper>
      </InfobokserWrapper>
      <VurderingsWrapper>
        <KommerBarnetTilGodeVurdering kommerBarnetTilgode={kommerBarnetTilgode} />
      </VurderingsWrapper>
    </SoeknadOversiktWrapper>
  </>
)
