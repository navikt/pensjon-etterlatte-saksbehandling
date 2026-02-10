import { Box, Button, Detail, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import Spinner from '~shared/Spinner'
import { Journalpost, SideInfo, Tema } from '~shared/types/Journalpost'
import { ApiErrorAlert } from '~ErrorBoundary'
import { isPending, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import React, { useEffect, useState } from 'react'
import { hentDokumenter } from '~shared/api/dokument'
import { useApiCall } from '~shared/hooks/useApiCall'
import { DokumentFilter } from '~components/person/dokumenter/DokumentFilter'
import { SakMedBehandlinger } from '~components/person/typer'
import { DokumentRad } from './DokumentRad'
import { ArrowDownIcon } from '@navikt/aksel-icons'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { OpprettOppgaveTilJournalpost } from '~components/person/dokumenter/OpprettOppgaveTilJournalpost'

export const Dokumentliste = ({ fnr, sakResult }: { fnr: string; sakResult: Result<SakMedBehandlinger> }) => {
  const [filter, setFilter] = useState<DokumentFilter>({
    tema: [Tema.EYO, Tema.EYB],
    type: [],
    status: [],
  })

  const [journalposter, setJournalposter] = useState<Journalpost[]>([])
  const [sideInfo, setSideInfo] = useState<SideInfo>()

  const [dokumenter, hentDokumenterForBruker] = useApiCall(hentDokumenter)
  const opprettOppgaveTilJournalpostEnabled = useFeaturetoggle(FeatureToggle.oppgave_til_journalpost)

  useEffect(() => void hent(), [fnr, filter])

  const hent = (etter?: string) =>
    void hentDokumenterForBruker(
      {
        fnr,
        temaer: filter.tema,
        typer: filter.type,
        statuser: filter.status,
        foerste: 50,
        etter,
      },
      (response) => {
        if (etter) setJournalposter([...journalposter, ...response.journalposter])
        else setJournalposter(response.journalposter)

        setSideInfo(response.sideInfo)
      }
    )

  return (
    <Box padding="space-8">
      <VStack gap="space-2">
        <Heading size="medium">Dokumenter</Heading>

        <DokumentFilter filter={filter} setFilter={setFilter} />

        <Table zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell />
              <Table.HeaderCell>ID</Table.HeaderCell>
              <Table.HeaderCell>Tittel</Table.HeaderCell>
              <Table.HeaderCell>Avsender/Mottaker</Table.HeaderCell>
              <Table.HeaderCell>Dato</Table.HeaderCell>
              <Table.HeaderCell>Sak</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
              <Table.HeaderCell>Tema</Table.HeaderCell>
              <Table.HeaderCell>Type</Table.HeaderCell>
              <Table.HeaderCell />
            </Table.Row>
          </Table.Header>

          <Table.Body>
            {mapResult(dokumenter, {
              error: (error) => (
                <Table.Row>
                  <Table.DataCell colSpan={100}>
                    <ApiErrorAlert>
                      {error.detail || 'Det har oppstått en feil ved henting av dokumenter'}
                    </ApiErrorAlert>
                  </Table.DataCell>
                </Table.Row>
              ),
            })}

            {!isPending(dokumenter) && !journalposter.length ? (
              <Table.Row shadeOnHover={false}>
                <Table.DataCell colSpan={100}>
                  <Detail>
                    <i>Ingen dokumenter funnet</i>
                  </Detail>
                </Table.DataCell>
              </Table.Row>
            ) : (
              <>
                {journalposter.map((dokument) => (
                  <DokumentRad key={dokument.journalpostId} dokument={dokument} sakStatus={sakResult} />
                ))}
              </>
            )}

            {isPending(dokumenter) && (
              <Table.Row>
                <Table.DataCell colSpan={100}>
                  <Spinner margin="space-0" label="Henter dokumenter" />
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>

        <HStack justify="center" marginBlock="space-4">
          {sideInfo?.finnesNesteSide && (
            <Button variant="secondary" onClick={() => hent(sideInfo?.sluttpeker)} icon={<ArrowDownIcon aria-hidden />}>
              Last flere
            </Button>
          )}
        </HStack>

        {opprettOppgaveTilJournalpostEnabled &&
          mapSuccess(sakResult, (sakOgBehandlinger) => <OpprettOppgaveTilJournalpost sak={sakOgBehandlinger.sak} />)}
      </VStack>
    </Box>
  )
}
