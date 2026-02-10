import { Oppgavestatus } from '~shared/types/oppgave'
import { Alert, BodyShort, VStack } from '@navikt/ds-react'
import React from 'react'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/AktivitetspliktOppgaveVurderingRoutes'

import { InfobrevKnapperad } from '~components/aktivitetsplikt/brev/VurderingInfoBrevOgOppsummering'
import { FerdigstillAktivitetspliktOppgaveModal } from '~components/aktivitetsplikt/brev/FerdigstillAktivitetspliktOppgaveModal'

export function UtenBrevVisning() {
  const { oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const oppgaveErFerdigstilt = oppgave.status === Oppgavestatus.FERDIGSTILT
  const oppgaveKanFerdigstilles = !oppgaveErFerdigstilt && !!aktivtetspliktbrevdata && !aktivtetspliktbrevdata.brevId

  return (
    <VStack gap="space-4" justify="center">
      {oppgaveErFerdigstilt ? (
        <>
          <Alert variant="success">Oppgaven er ferdigstilt</Alert>
          <InfobrevKnapperad />
        </>
      ) : oppgaveKanFerdigstilles ? (
        <>
          <BodyShort>Brev skal ikke sendes for denne oppgaven, du kan nå ferdigstille oppgaven.</BodyShort>

          <InfobrevKnapperad>
            <FerdigstillAktivitetspliktOppgaveModal />
          </InfobrevKnapperad>
        </>
      ) : (
        <>
          <Alert variant="error">
            Brev er ikke opprettet for oppgaven. Du må gå tilbake til forrige steg for å opprette brevet
          </Alert>
          <InfobrevKnapperad />
        </>
      )}
    </VStack>
  )
}
