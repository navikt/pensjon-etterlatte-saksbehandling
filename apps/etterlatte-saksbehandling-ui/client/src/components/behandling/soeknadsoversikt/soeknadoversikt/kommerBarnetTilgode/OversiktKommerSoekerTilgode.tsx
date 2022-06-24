import { Adresse } from './Adresse'
import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { IKommerSoekerTilgode } from '../../../../../store/reducers/BehandlingReducer'
import { InfobokserWrapper, Header, InfoWrapper, SoeknadOversiktWrapper, VurderingsWrapper } from '../../styled'

export const OversiktKommerSoekerTilgode = ({
  kommerSoekerTilgode,
}: {
  kommerSoekerTilgode: IKommerSoekerTilgode | undefined
}) => {
  return (
    <>
      <Header>Vurdering om pensjonen kommer barnet til gode</Header>
      {kommerSoekerTilgode ? (
        <SoeknadOversiktWrapper>
          <InfobokserWrapper>
            <InfoWrapper>
              <Adresse kommerSoekerTilgodeVurdering={kommerSoekerTilgode.kommerSoekerTilgodeVurdering} />
            </InfoWrapper>
          </InfobokserWrapper>
          <VurderingsWrapper>
            <KommerBarnetTilGodeVurdering
              kommerSoekerTilgodeVurdering={kommerSoekerTilgode.kommerSoekerTilgodeVurdering}
            />
          </VurderingsWrapper>
        </SoeknadOversiktWrapper>
      ) : (
        <div style={{ color: 'red' }}>Kunne ikke hente ut data om pensjon kommer barnet tilgode</div>
      )}
    </>
  )
}
