import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { Box, Button, HStack, Table, Tag } from '@navikt/ds-react'
import { CheckmarkIcon, ExternalLinkIcon } from '@navikt/aksel-icons'
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
import { ApiError } from '~shared/api/apiClient'

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

  const feilkodehaandtering = (error: ApiError) => {
    switch (error.status) {
      case 404:
        return <ApiWarningAlert>Kan ikke hente notater: {error.detail}</ApiWarningAlert>
      case 403:
        return <ApiErrorAlert>Du mangler tilgang til saken: {error.detail}</ApiErrorAlert>
      default:
        return <ApiErrorAlert>{error.detail || 'Feil ved henting av sak'}</ApiErrorAlert>
    }
  }

  if (isFailure(sakResult)) {
    return <Box padding="space-8">{feilkodehaandtering(sakResult.error)}</Box>
  }

  return (
    <Box padding="space-8">
      {mapResult(notatStatus, {
        pending: <Spinner label="Henter notater for sak ..." />,
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
                          <CheckmarkIcon aria-hidden /> Journalført
                        </Tag>
                      ) : (
                        <Tag variant="neutral">Under arbeid</Tag>
                      )}
                    </Table.DataCell>
                    <Table.DataCell>{formaterDatoMedKlokkeslett(notat.opprettet)}</Table.DataCell>
                    <Table.DataCell>
                      <HStack gap="space-4" justify="end">
                        {!!notat.journalpostId ? (
                          <>
                            <Button
                              as="a"
                              href={`/api/notat/${notat.id}/pdf`}
                              target="_blank"
                              variant="secondary"
                              icon={<ExternalLinkIcon aria-hidden />}
                              size="small"
                            >
                              Åpne
                            </Button>

                            <NotatVisningModal notat={notat} />
                          </>
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
