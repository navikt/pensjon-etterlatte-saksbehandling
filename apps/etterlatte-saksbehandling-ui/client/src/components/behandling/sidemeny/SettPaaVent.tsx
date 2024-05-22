import { Alert, Button, Heading, Label, Select, Textarea } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { FristHandlinger } from '~components/oppgavebenk/frist/FristHandlinger'
import { settOppgavePaaVentApi } from '~shared/api/oppgaver'
import { ClockDashedIcon, ClockIcon } from '@navikt/aksel-icons'
import { formaterStringDato } from '~utils/formattering'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { isPending } from '~shared/api/apiUtils'
import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/types/oppgave'
import { useAppDispatch } from '~store/Store'
import { settOppgave } from '~store/reducers/OppgaveReducer'
import { Text } from '~components/behandling/attestering/styled'

interface Props {
  oppgave: OppgaveDTO | null
  redigerbar: boolean
}

enum PaaventAarsak {
  OPPLYSNING_FRA_BRUKER = 'Opplysning fra bruker',
  OPPLYSNING_FRA_ANDRE = 'Opplysning fra andre',
  KRAVGRUNNLAG_SPERRET = 'Kravgrunnlag sperret',
  ANNET = 'Annet',
}

export const SettPaaVent = ({ oppgave, redigerbar }: Props) => {
  if (!oppgave || !erOppgaveRedigerbar(oppgave?.status)) return null

  const dispatch = useAppDispatch()
  type settPaaVentTyper = Array<keyof typeof PaaventAarsak>

  const [frist, setFrist] = useState<string>(oppgave.frist)
  const [merknad, setMerknad] = useState<string>(oppgave.merknad || '')
  const [settPaaVent, setVisPaaVent] = useState(false)
  const [aarsak, setAarsak] = useState<settPaaVentTyper[number]>()
  const [aarsakError, setAarsakError] = useState<boolean>(false)

  const [settPaaVentStatus, settOppgavePaaVent] = useApiCall(settOppgavePaaVentApi)

  const oppdater = () => {
    const paaVent = !(oppgave.status === 'PAA_VENT')
    if (paaVent && !aarsak) {
      setAarsakError(true)
      return
    }

    settOppgavePaaVent(
      {
        oppgaveId: oppgave.id,
        settPaaVentRequest: {
          aarsak: aarsak,
          merknad,
          paaVent: paaVent,
        },
      },
      (oppgave) => {
        dispatch(settOppgave(oppgave))

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
              <Text>Årsak for å sette på vent</Text>
              <Select
                label="Årsak for å sette på vent"
                hideLabel={true}
                value={aarsak || ''}
                onChange={(e) => {
                  setAarsakError(false)
                  setAarsak(e.target.value as settPaaVentTyper[number])
                }}
                error={aarsakError && 'Du må velge en årsak'}
              >
                <option value="" disabled={true}>
                  Velg
                </option>
                {(Object.keys(PaaventAarsak) as settPaaVentTyper).map((option) => (
                  <option key={option} value={option}>
                    {PaaventAarsak[option]}
                  </option>
                ))}
              </Select>
              <Label spacing>Frist</Label>

              <FlexRow $spacing>
                <FristHandlinger
                  orginalFrist={frist}
                  oppgaveId={oppgave.id}
                  oppdaterFrist={(_, frist: string) => setFrist(frist)}
                  erRedigerbar={erOppgaveRedigerbar(oppgave.status)}
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
