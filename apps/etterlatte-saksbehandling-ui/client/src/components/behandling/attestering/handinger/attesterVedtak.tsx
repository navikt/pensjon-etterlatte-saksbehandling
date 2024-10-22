import { useBehandling } from '~components/behandling/useBehandling'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import React from 'react'
import { AttesterYtelse } from '~components/behandling/attestering/handinger/attesterYtelse'
import { AttesterTilbakekreving } from '~components/behandling/attestering/handinger/attesterTilbakekreving'
import { useKlage } from '~components/klage/useKlage'
import { AttesterKlage } from '~components/behandling/attestering/handinger/attesterKlage'
import { Alert } from '@navikt/ds-react'
import { logger } from '~utils/logger'

export const AttesterVedtak = ({ kommentar }: { kommentar: string }) => {
  const behandling = useBehandling()
  const tilbakekreving = useTilbakekreving()
  const klage = useKlage()

  if (behandling) {
    return <AttesterYtelse behandling={behandling} kommentar={kommentar} />
  } else if (tilbakekreving) {
    return <AttesterTilbakekreving tilbakekreving={tilbakekreving} kommentar={kommentar} />
  } else if (klage) {
    return <AttesterKlage klage={klage} kommentar={kommentar} />
  } else {
    logger.generalWarning({ msg: 'Mangler behandling for attestering av vedtak ' })
    return <Alert variant="error">Mangler behandling for attestering av vedtak</Alert>
  }
}
