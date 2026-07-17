import React, { useEffect, useState } from 'react'
import {
  Alert,
  BodyShort,
  Box,
  Button,
  Detail,
  Heading,
  HStack,
  Loader,
  Select,
  Table,
  Tag,
  VStack,
} from '@navikt/ds-react'
import { ArrowsCirclepathIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import {
  hentProsesseringTasks,
  kanRekjores,
  opprettFeilbarDemoTask,
  PROSESSERING_STATUSER,
  ProsesseringStatus,
  ProsesseringTask,
  rekjorProsesseringTask,
} from '~components/prosessering/prosesseringApi'

const statusVariant = (status: ProsesseringStatus): React.ComponentProps<typeof Tag>['variant'] => {
  switch (status) {
    case 'FULLFØRT':
      return 'success'
    case 'KJØRER':
      return 'info'
    case 'KLAR':
      return 'neutral'
    case 'STOPPET':
      return 'error'
    case 'AVBRUTT':
      return 'warning'
  }
}

export const ProsesseringTasks = () => {
  const [statusFilter, setStatusFilter] = useState<ProsesseringStatus | ''>('')

  const [tasksResult, hentTasks] = useApiCall(hentProsesseringTasks)
  const [rekjorResult, rekjor] = useApiCall(rekjorProsesseringTask)
  const [demoResult, opprettDemo] = useApiCall(opprettFeilbarDemoTask)

  const oppdater = () => hentTasks({ status: statusFilter || undefined })

  useEffect(() => {
    oppdater()
  }, [statusFilter])

  const rekjorTask = (task: ProsesseringTask) => rekjor(task.id, () => oppdater())

  const opprettDemoTask = () => opprettDemo({}, () => oppdater())

  return (
    <Box padding="space-32" maxWidth="80rem">
      <VStack gap="space-24">
        <VStack gap="space-8">
          <Heading size="large">Prosessering – task-kø</Heading>
          <BodyShort textColor="subtle">
            Operatør-innsyn i prosessering-køen (kun tilgjengelig i dev). Se status og kjør stoppede eller avbrutte
            tasker på nytt.
          </BodyShort>
        </VStack>

        <HStack gap="space-16" align="end">
          <Select
            label="Filtrer på status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as ProsesseringStatus | '')}
          >
            <option value="">Alle</option>
            {PROSESSERING_STATUSER.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </Select>
          <Button variant="secondary" onClick={oppdater} loading={isPending(tasksResult)}>
            Oppdater
          </Button>
          <Button variant="primary" onClick={opprettDemoTask} loading={isPending(demoResult)}>
            Opprett demo-task (feiler, funker ved rekjør)
          </Button>
        </HStack>

        <BodyShort textColor="subtle">
          Demo-tasken simulerer en nedstrøms-avhengighet som er «nede» i ~20 sekunder: den feiler og havner i STOPPET.
          Vent til vinduet har gått og trykk «Rekjør» — da fullfører den.
        </BodyShort>

        {mapResult(demoResult, {
          error: (error) => <ApiErrorAlert>Kunne ikke opprette demo-task: {error.detail}</ApiErrorAlert>,
          success: (demo) => (
            <Alert variant="success">
              Opprettet demo-task {demo.taskId}. Simulert avhengighet er «oppe» fra{' '}
              {formaterDatoMedKlokkeslett(demo.simulertOppeFra)}.
            </Alert>
          ),
        })}

        {mapResult(rekjorResult, {
          error: (error) => <ApiErrorAlert>Kunne ikke rekjøre task: {error.detail}</ApiErrorAlert>,
        })}

        {mapResult(tasksResult, {
          pending: <Loader size="large" />,
          error: () => <ApiErrorAlert>Kunne ikke hente tasker</ApiErrorAlert>,
          success: (tasks) =>
            tasks.length === 0 ? (
              <Alert variant="info">Ingen tasker å vise.</Alert>
            ) : (
              <Table size="small">
                <Table.Header>
                  <Table.Row>
                    <Table.HeaderCell>Id</Table.HeaderCell>
                    <Table.HeaderCell>Type</Table.HeaderCell>
                    <Table.HeaderCell>Status</Table.HeaderCell>
                    <Table.HeaderCell>Feil</Table.HeaderCell>
                    <Table.HeaderCell>Stoppårsak</Table.HeaderCell>
                    <Table.HeaderCell>Trigger</Table.HeaderCell>
                    <Table.HeaderCell>Opprettet</Table.HeaderCell>
                    <Table.HeaderCell>Payload</Table.HeaderCell>
                    <Table.HeaderCell />
                  </Table.Row>
                </Table.Header>
                <Table.Body>
                  {tasks.map((task) => (
                    <Table.Row key={task.id}>
                      <Table.DataCell>{task.id}</Table.DataCell>
                      <Table.DataCell>{task.type}</Table.DataCell>
                      <Table.DataCell>
                        <Tag variant={statusVariant(task.status)} size="small">
                          {task.status}
                        </Tag>
                      </Table.DataCell>
                      <Table.DataCell>{task.antallFeil}</Table.DataCell>
                      <Table.DataCell>{task.stoppaarsak ?? '-'}</Table.DataCell>
                      <Table.DataCell>{formaterDatoMedKlokkeslett(task.triggerTid)}</Table.DataCell>
                      <Table.DataCell>{formaterDatoMedKlokkeslett(task.opprettetTid)}</Table.DataCell>
                      <Table.DataCell>
                        {task.payload ? (
                          <Detail as="code" style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                            {task.payload}
                          </Detail>
                        ) : (
                          '-'
                        )}
                      </Table.DataCell>
                      <Table.DataCell>
                        {kanRekjores(task.status) && (
                          <Button
                            variant="secondary"
                            size="small"
                            icon={<ArrowsCirclepathIcon aria-hidden />}
                            loading={isPending(rekjorResult)}
                            onClick={() => rekjorTask(task)}
                          >
                            Rekjør
                          </Button>
                        )}
                      </Table.DataCell>
                    </Table.Row>
                  ))}
                </Table.Body>
              </Table>
            ),
        })}
      </VStack>
    </Box>
  )
}
