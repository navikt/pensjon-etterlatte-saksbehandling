import { InfoCard } from '@navikt/ds-react'
import { PadlockLockedIcon } from '@navikt/aksel-icons'
import { IBehandlingStatus, IDetaljertBehandling, ManglerSkrivetilgangAarsak } from '~shared/types/IDetaljertBehandling'
import { ENHETER } from '~shared/types/Enhet'
import {
  SkrivebeskyttetAarsak,
  STATUS,
  useBehandlingRedigerbarhet,
} from '~components/behandling/useBehandlingRedigerbarhet'

export const forklaringSkrivebeskyttet = (
  aarsak: SkrivebeskyttetAarsak,
  behandling: Pick<IDetaljertBehandling, 'status' | 'sakEnhetId'>
): string => {
  switch (aarsak) {
    case STATUS:
      return forklaringStatus(behandling.status)
    case ManglerSkrivetilgangAarsak.ENHET_UTEN_SKRIVETILGANG:
      return `Saken tilhører enhet ${ENHETER[behandling.sakEnhetId] ?? behandling.sakEnhetId}, som du ikke har skrivetilgang til.`
    case ManglerSkrivetilgangAarsak.MANGLER_ROLLE:
      return 'Du har bare lesetilgang i Gjenny. Du trenger saksbehandler- eller attestantrollen for å endre behandlingen.'
    case ManglerSkrivetilgangAarsak.UKJENT_BRUKER:
      return 'Du har ikke skrivetilgang til behandlingen.'
  }
}

const forklaringStatus = (status: IBehandlingStatus): string => {
  switch (status) {
    case IBehandlingStatus.FATTET_VEDTAK:
      return 'Behandlingen er sendt til attestering.'
    case IBehandlingStatus.AVBRUTT:
      return 'Behandlingen er avbrutt.'
    default:
      return 'Behandlingen er ferdig behandlet.'
  }
}

export const SkrivebeskyttetVarsel = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const { redigerbar, aarsak } = useBehandlingRedigerbarhet(behandling)

  if (redigerbar || !aarsak) {
    return null
  }

  return (
    <InfoCard data-color="info" size="small" as="section" aria-label="Behandlingen kan ikke endres">
      <InfoCard.Header icon={<PadlockLockedIcon aria-hidden />}>
        <InfoCard.Title>Du kan ikke endre behandlingen</InfoCard.Title>
      </InfoCard.Header>
      <InfoCard.Content>{forklaringSkrivebeskyttet(aarsak, behandling)}</InfoCard.Content>
    </InfoCard>
  )
}
