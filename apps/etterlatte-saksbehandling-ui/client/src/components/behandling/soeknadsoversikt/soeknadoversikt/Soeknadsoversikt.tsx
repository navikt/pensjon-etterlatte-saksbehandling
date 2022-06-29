import { OversiktGyldigFramsatt } from './gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { OversiktKommerSoekerTilgode } from './kommerBarnetTilgode/OversiktKommerSoekerTilgode'
import { IDetaljertBehandling, VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import { Innhold } from '../styled'

export interface PropsOmSoeknad {
  behandling: IDetaljertBehandling
}

export const SoeknadOversikt: React.FC<PropsOmSoeknad> = ({ behandling }) => {
  return (
    <Innhold>
      <OversiktGyldigFramsatt gyldigFramsatt={behandling.gyldighetsprøving} />
      {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
        <OversiktKommerSoekerTilgode kommerSoekerTilgode={behandling.kommerSoekerTilgode} />
      )}
    </Innhold>
  )
}
