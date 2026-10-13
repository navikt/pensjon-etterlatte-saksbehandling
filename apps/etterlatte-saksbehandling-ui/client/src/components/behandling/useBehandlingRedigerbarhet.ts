import { IDetaljertBehandling, ManglerSkrivetilgangAarsak } from '~shared/types/IDetaljertBehandling'
import { statusErRedigerbar } from '~components/behandling/felles/utils'

export const STATUS = 'STATUS'

export type SkrivebeskyttetAarsak = typeof STATUS | ManglerSkrivetilgangAarsak

export interface Redigerbarhet {
  redigerbar: boolean
  aarsak: SkrivebeskyttetAarsak | null
}

type BehandlingForRedigerbarhet = Pick<IDetaljertBehandling, 'status' | 'skrivetilgang'>

/**
 * Én regel for om en behandling kan endres i UI: status må tillate endring (speiler
 * BehandlingStatus.kanEndres() i backend) og saksbehandler må ha skrivetilgang (fra backend).
 * Status sjekkes her, og ikke i backend, fordi status oppdateres lokalt i redux uten ny henting.
 */
export const vurderRedigerbarhet = (behandling: BehandlingForRedigerbarhet | null): Redigerbarhet => {
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

export const useBehandlingRedigerbarhet = (behandling: BehandlingForRedigerbarhet | null): Redigerbarhet =>
  vurderRedigerbarhet(behandling)
