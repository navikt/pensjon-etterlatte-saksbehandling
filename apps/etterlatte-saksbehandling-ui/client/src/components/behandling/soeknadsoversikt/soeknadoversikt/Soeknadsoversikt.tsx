import styled from 'styled-components'
import { OversiktGyldigFramsatt } from './gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { OversiktKommerSoekerTilgode } from './kommerBarnetTilgode/OversiktKommerSoekerTilgode'
import {
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerSoekerTilgode,
  VurderingsResultat,
} from '../../../../store/reducers/BehandlingReducer'
import { Behandling } from '../../../person/typer'

export interface PropsOmSoeknad {
  behandling: IDetaljertBehandling
}

export const SoeknadOversikt: React.FC<PropsOmSoeknad> = ({ behandling }) => {
  return (
    <SoeknadOversiktWrapper>
      <div>
        <OversiktGyldigFramsatt gyldigFramsatt={behandling.gyldighetsprøving} />
        {behandling.gyldighetsprøving.resultat === VurderingsResultat.OPPFYLT && (
          <OversiktKommerSoekerTilgode kommerSoekerTilgode={behandling.kommerSoekerTilgode} />
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
