import { OppgaveDTO } from '~shared/types/oppgave'
import { useState } from 'react'
import { Button, DatePicker, ErrorMessage, Heading, HStack, Modal, VStack } from '@navikt/ds-react'
import { PencilIcon } from '@navikt/aksel-icons'
import { StatusPaaOppgaveFrist } from '~components/oppgavebenk/frist/StatusPaaOppgaveFrist'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerFristApi } from '~shared/api/oppgaver'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { isPending } from '~shared/api/apiUtils'
import { datoIMorgen, datoToAarFramITid } from '~utils/formatering/dato'

interface Props {
  oppgave: OppgaveDTO
  oppdaterFrist: (oppgaveId: string, nyFrist: string) => void
}

export const SettNyOppgaveFristModal = ({ oppgave, oppdaterFrist }: Props) => {
  const [aapen, setAapen] = useState<boolean>(false)
  const [nyFrist, setNyFrist] = useState<Date | undefined>(undefined)
  const [fristFeilmelding, setFristFeilmelding] = useState<string>('')

  const [redigerFristResult, redigerFristFunc] = useApiCall(redigerFristApi)

  const settNyFristForOppgave = () => {
    if (nyFrist) {
      redigerFristFunc({ oppgaveId: oppgave.id, frist: nyFrist }, () => {
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
      <Button
        size="small"
        variant="tertiary"
        iconPosition="right"
        icon={<PencilIcon aria-hidden />}
        onClick={() => setAapen(!aapen)}
      >
        <StatusPaaOppgaveFrist oppgaveFrist={oppgave.frist} oppgaveStatus={oppgave.status} />
      </Button>

      <Modal open={aapen} onClose={() => setAapen(false)} aria-label="modal for å sette ny oppgave frist">
        <Modal.Body>
          <Modal.Header>
            <Heading size="medium" spacing>
              Sett ny frist
            </Heading>
          </Modal.Header>
          <VStack gap="space-2">
            <VStack gap="space-1">
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
            <HStack gap="space-2" justify="end">
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
