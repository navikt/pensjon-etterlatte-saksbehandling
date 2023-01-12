import { OversiktGyldigFramsatt } from './gyldigFramsattSoeknad/OversiktGyldigFramsatt'
import { OversiktKommerBarnetTilgode } from './kommerBarnetTilgode/OversiktKommerBarnetTilgode'
import { Innhold } from '../styled'
import Virkningstidspunkt from './virkningstidspunkt/Virkningstidspunkt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'

interface Props {
  behandling: IDetaljertBehandling
  kunLesetilgang: boolean
}

export const SoeknadOversikt = ({ behandling, kunLesetilgang }: Props) => (
  <Innhold>
    <OversiktGyldigFramsatt gyldigFramsatt={behandling.gyldighetsprøving} />
    {behandling.gyldighetsprøving?.resultat === VurderingsResultat.OPPFYLT && (
      <>
        <OversiktKommerBarnetTilgode
          kommerBarnetTilgode={behandling.kommerBarnetTilgode}
          kunLesetilgang={kunLesetilgang}
        />
        <Virkningstidspunkt kunLesetilgang={kunLesetilgang} />
      </>
    )}
  </Innhold>
)
