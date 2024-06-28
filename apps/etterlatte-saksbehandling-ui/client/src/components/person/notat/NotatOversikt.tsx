import { useApiCall } from '~shared/hooks/useApiCall'
import { useEffect, useState } from 'react'
import { Box, HStack, Table, Tag } from '@navikt/ds-react'
import { CheckmarkIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'
import { SakMedBehandlinger } from '~components/person/typer'
import { isFailure, isSuccess, mapResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { hentNotaterForSak, Notat } from '~shared/api/notat'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { NotatRedigeringModal } from '~components/person/notat/NotatRedigeringModal'
import { NyttNotatModal } from '~components/person/notat/NyttNotatModal'
import { NotatVisningModal } from '~components/person/notat/NotatVisningModal'
import { SlettNotatModal } from '~components/person/notat/SlettNotatModal'

export default function NotatOversikt({ sakResult }: { sakResult: Result<SakMedBehandlinger> }) {
  const [notater, setNotater] = useState<Array<Notat>>([])

  const [notatStatus, hentNotater] = useApiCall(hentNotaterForSak)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentNotater(sakResult.data.sak.id, (notater) => {
        setNotater(notater)
      })
    }
  }, [sakResult])

  if (isFailure(sakResult)) {
    return (
      <Box padding="8">
        {sakResult.error.status === 404 ? (
          <ApiWarningAlert>Kan ikke hente notater: {sakResult.error.detail}</ApiWarningAlert>
        ) : (
          <ApiErrorAlert>{sakResult.error.detail || 'Feil ved henting av notater'}</ApiErrorAlert>
        )}
      </Box>
    )
  }

  return (
    <Box padding="8">
      {mapResult(notatStatus, {
        pending: <Spinner visible label="Henter notater for sak ..." />,
        error: () => <ApiErrorAlert>Feil ved henting av notater...</ApiErrorAlert>,
        success: () => (
          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell>ID</Table.HeaderCell>
                <Table.HeaderCell>Tittel</Table.HeaderCell>
                <Table.HeaderCell>Status</Table.HeaderCell>
                <Table.HeaderCell>Opprettet</Table.HeaderCell>
                <Table.HeaderCell />
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {!notater.length ? (
                <Table.Row>
                  <Table.DataCell colSpan={6}>Ingen notater funnet</Table.DataCell>
                </Table.Row>
              ) : (
                notater.map((notat) => (
                  <Table.Row key={notat.id}>
                    <Table.DataCell>{notat.id}</Table.DataCell>
                    <Table.DataCell>{notat.tittel}</Table.DataCell>
                    <Table.DataCell>
                      {!!notat.journalpostId ? (
                        <Tag variant="success">
                          <CheckmarkIcon /> Journalført
                        </Tag>
                      ) : (
                        <Tag variant="neutral">Under arbeid</Tag>
                      )}
                    </Table.DataCell>
                    <Table.DataCell>{formaterDatoMedKlokkeslett(notat.opprettet)}</Table.DataCell>
                    <Table.DataCell>
                      <HStack gap="4" justify="end">
                        {!!notat.journalpostId ? (
                          <NotatVisningModal notat={notat} />
                        ) : (
                          <>
                            <SlettNotatModal
                              notat={notat}
                              fjernNotat={(id) => setNotater(notater.filter((notat) => notat.id !== id))}
                            />
                            <NotatRedigeringModal notat={notat} />
                          </>
                        )}
                      </HStack>
                    </Table.DataCell>
                  </Table.Row>
                ))
              )}
            </Table.Body>
          </Table>
        ),
      })}

      <br />

      {mapSuccess(sakResult, ({ sak }) => (
        <NyttNotatModal sakId={sak.id} leggTilNotat={(notat) => setNotater([...notater, notat])} />
      ))}
    </Box>
  )
}
