import { Alert, Box, Heading } from '@navikt/ds-react'
import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isFailure } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isPending } from '@reduxjs/toolkit'
import { opprettAktivitetspliktsbrev } from '~shared/api/aktivitetsplikt'
import { Aktivitetspliktbrev } from '~components/aktivitetsplikt/brev/AktivitetspliktBrev'
import { UtenBrevVisning } from '~components/aktivitetsplikt/brev/UtenBrevVisning'

export function VurderingInfoBrevOgOppsummering({ fetchOppgave }: { fetchOppgave: () => void }) {
  useSidetittel('Aktivitetsplikt brev og oppsummering')

  const { oppgave, aktivtetspliktbrevdata } = useAktivitetspliktOppgaveVurdering()
  const [opprettBrevStatus, opprettBrevApiCall] = useApiCall(opprettAktivitetspliktsbrev)
  const brevdataFinnes = !!aktivtetspliktbrevdata

  const [brevId, setBrevid] = useState<number | undefined>(aktivtetspliktbrevdata?.brevId)
  const [brevErKlart, setBrevErKlart] = useState<boolean>(false)

  useEffect(() => {
    if (brevdataFinnes) {
      if (aktivtetspliktbrevdata?.skalSendeBrev) {
        if (aktivtetspliktbrevdata.brevId) {
          setBrevid(aktivtetspliktbrevdata.brevId)
          setBrevErKlart(true)
        } else {
          opprettBrevApiCall({ oppgaveId: oppgave.id }, (brevIdDto) => {
            setBrevid(brevIdDto.brevId)
            setBrevErKlart(true)
          })
        }
      } else {
        setBrevErKlart(false)
      }
    }
  }, [])

  return (
    <Box paddingInline="16" paddingBlock="16">
      <Heading size="large">Vurdering av {aktivtetspliktbrevdata?.skalSendeBrev ? 'brev' : 'oppgave'}</Heading>
      {brevdataFinnes ? (
        <>
          {aktivtetspliktbrevdata?.skalSendeBrev ? (
            <>
              {isPending(opprettBrevStatus) && <Spinner label="Oppretter brev" />}
              {isFailure(opprettBrevStatus) && (
                <ApiErrorAlert>Kunne ikke opprette brev {opprettBrevStatus.error.detail}</ApiErrorAlert>
              )}
              {brevErKlart && brevId && (
                <Aktivitetspliktbrev brevId={brevId} sakId={oppgave.sakId} oppgaveid={oppgave.id} />
              )}
            </>
          ) : (
            <UtenBrevVisning oppgave={oppgave} fetchOppgave={fetchOppgave} />
          )}
        </>
      ) : (
        <>
          <Alert variant="error">
            Brevdata finnes ikke for denne oppgaven, gå tilbake til vurderingssiden for å endre dette
          </Alert>
        </>
      )}
    </Box>
  )
}
