import { IDetaljertBehandling, ManglerSkrivetilgangAarsak } from '~shared/types/IDetaljertBehandling'
import { statusErRedigerbar } from '~components/behandling/felles/utils'

export const STATUS = 'STATUS'

export type SkrivebeskyttetAarsak = typeof STATUS | ManglerSkrivetilgangAarsak

export interface Redigerbarhet {
  redigerbar: boolean
  aarsak: SkrivebeskyttetAarsak | null
}

/**
 * Én regel for om en behandling kan endres i UI: status må tillate endring (speiler
 * BehandlingStatus.kanEndres() i backend) og saksbehandler må ha skrivetilgang (fra backend).
 * Status sjekkes her, og ikke i backend, fordi status oppdateres lokalt i redux uten ny henting.
 */
export const vurderRedigerbarhet = (
  behandling: Pick<IDetaljertBehandling, 'status' | 'skrivetilgang'> | null
): Redigerbarhet => {
  if (!behandling) {
    return { redigerbar: false, aarsak: null }
  }
  if (!statusErRedigerbar(behandling.status)) {
    return { redigerbar: false, aarsak: STATUS }
  }
  if (!behandling.skrivetilgang.harSkrivetilgang) {
    return { redigerbar: false, aarsak: behandling.skrivetilgang.aarsak ?? ManglerSkrivetilgangAarsak.UKJENT_BRUKER }
  }
  return { redigerbar: true, aarsak: null }
}

export const useBehandlingRedigerbarhet = (
  behandling: Pick<IDetaljertBehandling, 'status' | 'skrivetilgang'> | null
): Redigerbarhet => vurderRedigerbarhet(behandling)
