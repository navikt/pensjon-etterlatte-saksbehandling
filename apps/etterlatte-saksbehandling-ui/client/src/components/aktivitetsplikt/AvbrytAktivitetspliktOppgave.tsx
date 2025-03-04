import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'
import { useApiCall } from '~shared/hooks/useApiCall'
import { avbrytOppgaveMedMerknad } from '~shared/api/oppgaver'
import { Button } from '@navikt/ds-react'
import { isPending } from '~shared/api/apiUtils'
import React from 'react'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useForm } from 'react-hook-form'

export function AvbrytAktivitetspliktOppgave() {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const { oppgave } = useAktivitetspliktOppgaveVurdering()

  const erTildeltSaksbehandler = innloggetSaksbehandler.ident === oppgave.saksbehandler?.ident
  const kanRedigeres = erOppgaveRedigerbar(oppgave.status)

  const [avbrytOppgaveStatus, avbrytOppgave] = useApiCall(avbrytOppgaveMedMerknad)

  const { handleSubmit } = useForm<{ kommentar: string }>({ defaultValues: { kommentar: '' } })

  const avbryt = () => {
    avbrytOppgave({ id: oppgave.id, merknad: 'Oppgaven er avbrutt av saksbehandler' })
  }

  return (
    <>
      {kanRedigeres && erTildeltSaksbehandler ? (
        <Button variant="danger" onClick={handleSubmit(avbryt)} loading={isPending(avbrytOppgaveStatus)}>
          Avbryt oppgave
        </Button>
      ) : (
        <p>Kun saksbehandler kan gjøre handlinger på denne oppgaven</p>
      )}
    </>
  )
}
