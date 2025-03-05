import { PersonOversiktFane } from '~components/person/Person'
import { lagrePersonLocationState } from '~components/person/lenker/usePersonLocationState'

export const navigerTilPersonOversikt = (fnr: string, fane?: PersonOversiktFane) => {
  const key = window.crypto.randomUUID()

  const params = new URLSearchParams({
    key: key || '',
    fane: fane || PersonOversiktFane.SAKER,
  })

  lagrePersonLocationState(key, fnr)

  window.location.href = `/person?${params}`
}
