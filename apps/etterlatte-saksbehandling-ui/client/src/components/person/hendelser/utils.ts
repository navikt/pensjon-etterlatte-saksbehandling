import { IBehandlingsammendrag } from '~components/person/typer'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'

export const harAapenRevurdering = (behandlinger: IBehandlingsammendrag[]): boolean => {
  return (
    behandlinger
      .filter((behandling) => behandling.behandlingType === IBehandlingsType.REVURDERING)
      .filter((behandling) => !erFerdigBehandlet(behandling.status)).length > 0
  )
}
