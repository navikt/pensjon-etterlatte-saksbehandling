import { PropsOmSoeknad } from '../props'
import styled from 'styled-components'
import { SoeknadOversiktGyldigFramsatt } from './SoeknadsoversiktDel1'
import { SoeknadOversiktKommerSoekerTilgode } from './SoeknadsoversiktDel2'
import { VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'

export const SoeknadOversikt: React.FC<PropsOmSoeknad> = ({ gyldigFramsatt, kommerSoekerTilgode }) => {
  return (
    <SoeknadOversiktWrapper>
      <div>
        <SoeknadOversiktGyldigFramsatt gyldigFramsatt={gyldigFramsatt} />
        {gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT && (
          <SoeknadOversiktKommerSoekerTilgode kommerSoekerTilgode={kommerSoekerTilgode} />
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
