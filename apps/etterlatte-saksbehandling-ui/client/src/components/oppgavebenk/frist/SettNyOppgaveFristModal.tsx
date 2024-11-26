import { erOppgaveRedigerbar, OppgaveDTO } from '~shared/types/oppgave'
import { useState } from 'react'
import { Button, DatePicker, ErrorMessage, Heading, HStack, Modal, VStack } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import { add } from 'date-fns'
import { StatusPaaOppgaveFrist } from '~components/oppgavebenk/frist/StatusPaaOppgaveFrist'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerFristApi } from '~shared/api/oppgaver'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'

interface Props {
  oppgave: OppgaveDTO
  oppdaterFrist: (oppgaveId: string, nyFrist: string) => void
}

export const SettNyOppgaveFristModal = ({ oppgave, oppdaterFrist }: Props) => {
  const [aapen, setAapen] = useState<boolean>(false)
  const [nyFrist, setNyFrist] = useState<Date | undefined>(undefined)
  const [fristFeilmelding, setFristFeilmelding] = useState<string>('')

  const [redigerFristResult, redigerFristFunc] = useApiCall(redigerFristApi)

  const datoIMorgen = (): Date => {
    return add(new Date(), { days: 1 })
  }

  const datoToAarFramITid = (): Date => {
    return add(new Date(), { years: 2 })
  }

  const settNyFristForOppgave = () => {
    if (nyFrist) {
      redigerFristFunc({ oppgaveId: oppgave.id, redigerFristRequest: { frist: nyFrist, versjon: null } }, () => {
        oppdaterFrist(oppgave.id, nyFrist.toISOString())
        setFristFeilmelding('')
        setAapen(false)
      })
    } else {
      setFristFeilmelding('Du må velge en ny frist')
    }
  }

  return (
    <>
      {erOppgaveRedigerbar(oppgave.status) ? (
        <Button
          size="small"
          variant="tertiary"
          iconPosition="right"
          icon={<PencilIcon aria-hidden />}
          onClick={() => setAapen(!aapen)}
        >
          <StatusPaaOppgaveFrist oppgaveFrist={oppgave.frist} oppgaveStatus={oppgave.status} />
        </Button>
      ) : (
        <StatusPaaOppgaveFrist oppgaveFrist={oppgave.frist} oppgaveStatus={oppgave.status} />
      )}

      <Modal open={aapen} onClose={() => setAapen(false)} aria-labelledby="modal for å sette ny oppgave frist">
        <Modal.Body>
          <Modal.Header>
            <Heading size="medium" spacing>
              Sett ny frist
            </Heading>
          </Modal.Header>
          <VStack gap="2">
            <VStack gap="1">
              <DatePicker.Standalone
                onSelect={(frist) => frist && setNyFrist(frist)}
                selected={nyFrist}
                fromDate={datoIMorgen()}
                toDate={datoToAarFramITid()}
                dropdownCaption
              />
              {fristFeilmelding && <ErrorMessage>{fristFeilmelding}</ErrorMessage>}
            </VStack>
            {isFailureHandler({ apiResult: redigerFristResult, errorMessage: 'Kunne ikke sette ny frist' })}
            <HStack gap="2" justify="end">
              <Button variant="secondary" onClick={() => setAapen(false)} disabled={isPending(redigerFristResult)}>
                Avbryt
              </Button>
              <Button loading={isPending(redigerFristResult)} onClick={settNyFristForOppgave}>
                Sett ny Frist
              </Button>
            </HStack>
          </VStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
