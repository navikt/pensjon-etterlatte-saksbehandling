import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import { avbrytOppgaveMedMerknad } from '~shared/api/oppgaver'
import { Alert, Button } from '@navikt/ds-react'
import { isPending } from '~shared/api/apiUtils'
import React from 'react'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export function AvbrytAktivitetspliktOppgave() {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const { oppgave } = useAktivitetspliktOppgaveVurdering()

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  const [avbrytOppgaveMedMerknadResult, avbrytOppgaveMedMerknadRequest] = useApiCall(avbrytOppgaveMedMerknad)

  const avbryt = () => {
    avbrytOppgaveMedMerknadRequest({ id: oppgave.id, merknad: 'Oppgaven er avbrutt av saksbehandler' })
  }

  return (
    <>
      {kanRedigeres && erTildeltSaksbehandler ? (
        <Button variant="danger" onClick={avbryt} loading={isPending(avbrytOppgaveMedMerknadResult)}>
          Avbryt oppgave
        </Button>
      ) : (
        <Alert variant="warning">Du må tildele deg oppgaven for å utføre handlinger</Alert>
      )}
    </>
  )
}
