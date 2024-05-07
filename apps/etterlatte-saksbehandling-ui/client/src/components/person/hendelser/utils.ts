import { IBehandlingsammendrag } from '~components/person/typer'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { behandlingErIverksatt, enhetErSkrivbar, erFerdigBehandlet } from '~components/behandling/felles/utils'

export const harAapenRevurdering = (behandlinger: IBehandlingsammendrag[]): boolean => {
  return (
    behandlinger
      .filter((behandling) => behandling.behandlingType === IBehandlingsType.REVURDERING)
      .filter((behandling) => !erFerdigBehandlet(behandling.status)).length > 0
  )
}

export const revurderingKanOpprettes = (
  behandlinger: IBehandlingsammendrag[],
  enhetId: string,
  enheter: string[]
): Boolean => {
  return (
    behandlinger.filter((behandling) => behandlingErIverksatt(behandling.status)).length > 0 &&
    enhetErSkrivbar(enhetId, enheter)
  )
}
