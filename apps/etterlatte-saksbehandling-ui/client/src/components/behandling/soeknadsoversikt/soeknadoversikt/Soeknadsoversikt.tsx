import styled from 'styled-components'
import { OversiktGyldigFramsatt } from './gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { OversiktKommerSoekerTilgode } from './kommerBarnetTilgode/OversiktKommerSoekerTilgode'
import {
  IGyldighetResultat,
  IKommerSoekerTilgode,
  VurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'

export interface PropsOmSoeknad {
  gyldigFramsatt: IGyldighetResultat
  kommerSoekerTilgode: IKommerSoekerTilgode
}

export const SoeknadOversikt: React.FC<PropsOmSoeknad> = ({ gyldigFramsatt, kommerSoekerTilgode }) => {
  return (
    <SoeknadOversiktWrapper>
      <div>
        <OversiktGyldigFramsatt gyldigFramsatt={gyldigFramsatt} />
        {gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT && (
          <OversiktKommerSoekerTilgode kommerSoekerTilgode={kommerSoekerTilgode} />
        )}
      </div>
    </SoeknadOversiktWrapper>
  )
}

export const SoeknadOversiktWrapper = styled.div`
  flex-wrap: wrap;
  margin-bottom: 2em;
  padding-left: 5em;
`

export const InfoWrapper = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);

  > * {
    width: 180px;
  }
  height: 120px;
  flex-grow: 1;
`
