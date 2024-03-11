import { Alert, Button, Heading, Label, Textarea } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { FristHandlinger } from '~components/oppgavebenk/frist/FristHandlinger'
import { erOppgaveRedigerbar, OppgaveDTO, settOppgavePaaVentApi } from '~shared/api/oppgaver'
import { ClockDashedIcon, ClockIcon } from '@navikt/aksel-icons'
import { formaterStringDato } from '~utils/formattering'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { isPending } from '~shared/api/apiUtils'

export const SettPaaVent = ({
  oppgave,
  redigerbar,
  refreshOppgave,
}: {
  oppgave: OppgaveDTO
  redigerbar: boolean
  refreshOppgave: () => void
}) => {
  const [frist, setFrist] = useState<string>(oppgave.frist)
  const [merknad, setMerknad] = useState<string>(oppgave.merknad || '')
  const [settPaaVent, setVisPaaVent] = useState(false)

  const [settPaaVentStatus, settOppgavePaaVent] = useApiCall(settOppgavePaaVentApi)

  const oppdater = () => {
    settOppgavePaaVent(
      {
        oppgaveId: oppgave.id,
        settPaaVentRequest: {
          merknad,
          paaVent: !(oppgave.status === 'PAA_VENT'),
        },
      },
      () => {
        refreshOppgave()
        setVisPaaVent(false)
      }
    )
  }

  return (
    <div>
      {settPaaVent && (
        <>
          <hr />
          <Textarea label="Merknad" value={merknad} onChange={(e) => setMerknad(e.target.value)} />

          <br />
          {oppgave.status !== 'PAA_VENT' && (
            <>
              <Label spacing>Frist</Label>

              <FlexRow $spacing>
                <FristHandlinger
                  orginalFrist={frist}
                  oppgaveId={oppgave.id}
                  oppdaterFrist={(_, frist: string) => setFrist(frist)}
                  erRedigerbar={erOppgaveRedigerbar(oppgave.status)}
                  oppgaveVersjon={oppgave.versjon}
                  type={oppgave.type}
                />
              </FlexRow>
            </>
          )}

          <FlexRow justify="right">
            <Button size="small" variant="secondary" onClick={() => setVisPaaVent(false)}>
              Avbryt
            </Button>
            <Button size="small" variant="primary" onClick={oppdater} loading={isPending(settPaaVentStatus)}>
              Lagre
            </Button>
          </FlexRow>
        </>
      )}

      {!settPaaVent && (
        <>
          {oppgave?.status === 'PAA_VENT' && (
            <>
              <Alert variant="warning" size="small">
                <Heading size="xsmall" spacing>
                  Oppgaven står på vent!
                </Heading>

                <Info label="Merknad" tekst={oppgave.merknad || 'Ingen'} />
                <Info label="Ny frist" tekst={formaterStringDato(oppgave.frist)} />
              </Alert>
            </>
          )}

          <br />
          {redigerbar && (
            <FlexRow justify="right">
              <Button
                size="small"
                variant="secondary"
                onClick={() => setVisPaaVent(true)}
                icon={oppgave.status === 'PAA_VENT' ? <ClockDashedIcon /> : <ClockIcon />}
                iconPosition="right"
              >
                {oppgave.status === 'PAA_VENT' ? 'Ta av vent' : 'Sett på vent'}
              </Button>
            </FlexRow>
          )}
        </>
      )}
    </div>
  )
}
