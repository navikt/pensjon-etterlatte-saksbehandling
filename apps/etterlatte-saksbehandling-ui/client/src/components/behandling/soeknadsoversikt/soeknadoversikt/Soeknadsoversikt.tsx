import { OversiktGyldigFramsatt } from './gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { OversiktKommerBarnetTilgode } from './kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import { Innhold } from '../styled'
import Virkningstidspunkt from './virkningstidspunkt/Virkningstidspunkt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'

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
