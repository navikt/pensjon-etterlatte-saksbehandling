import { BodyShort } from '@navikt/ds-react'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'

export const ReturnertVisning = ({
  utlandsBehandling,
}: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  return (
    <>
      <BodyShort>Behandling ble returnert av attestant. Begrunnelse:</BodyShort>
      <BodyShort>{utlandsBehandling.kommentar}</BodyShort>
    </>
  )
}
