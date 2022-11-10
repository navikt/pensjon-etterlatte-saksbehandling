import { OversiktGyldigFramsatt } from './gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { OversiktKommerBarnetTilgode } from './kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import { IDetaljertBehandling, VurderingsResultat } from '~store/reducers/BehandlingReducer'
import { Innhold } from '../styled'
import Virkningstidspunkt from './virkningstidspunkt/Virkningstidspunkt'

export interface PropsOmSoeknad {
  behandling: IDetaljertBehandling
}

export const SoeknadOversikt: React.FC<PropsOmSoeknad> = ({ behandling }) => {
  return (
    <Innhold>
      <OversiktGyldigFramsatt gyldigFramsatt={behandling.gyldighetsprøving} />
      {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
        <OversiktKommerBarnetTilgode kommerBarnetTilgode={behandling.kommerBarnetTilgode} />
      )}
      <Virkningstidspunkt />
    </Innhold>
  )
}
