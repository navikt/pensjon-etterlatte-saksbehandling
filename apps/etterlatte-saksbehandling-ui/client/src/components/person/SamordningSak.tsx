import React, { useEffect, useState } from 'react'
import { Alert, Heading, Table } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSamordningsdataForSak } from '~shared/api/vedtaksvurdering'
import { Samordningsvedtak } from '~components/vedtak/typer'
import { isInitial, isPending, isSuccess, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { formaterStringDato } from '~utils/formattering'
import { SakMedBehandlinger } from '~components/person/typer'
import { Container } from '~shared/styled'

export const SamordningSak = ({ sakStatus }: { sakStatus: Result<SakMedBehandlinger> }) => {
  const [samordningsdata, setSamordningsdata] = useState<Samordningsvedtak[]>()
  const [hentet, hent] = useApiCall(hentSamordningsdataForSak)

  useEffect(() => {
    if (isSuccess(sakStatus) && isInitial(hentet)) {
      hent(sakStatus.data.sak.id, (result) => setSamordningsdata(result))
    }
  })

  return (
    <Container>
      <Spinner visible={isPending(hentet)} label="Henter data" />

      {isFailureHandler({
        apiResult: hentet,
        errorMessage: 'Kunne ikke hente data',
      })}

      {isSuccess(hentet) && samordningsdata && samordningsdata.length === 0 && (
        <Alert variant="info">Ingen samordningsdata</Alert>
      )}

      {isSuccess(hentet) && samordningsdata && (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>VedtakID</Table.HeaderCell>
              <Table.HeaderCell>SamordningID</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
              <Table.HeaderCell>Etterbetaling</Table.HeaderCell>
              <Table.HeaderCell>Utvidet samordningsfrist</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {!samordningsdata.length && (
              <Table.Row>
                <Table.DataCell colSpan={5}>Ingen samordningsvedtak</Table.DataCell>
              </Table.Row>
            )}
            {samordningsdata.map((vedtak) => (
              <Table.Row key={vedtak.vedtakId}>
                <Table.DataCell>{vedtak.vedtakId}</Table.DataCell>
                <Table.DataCell>{vedtak.samordningVedtakId}</Table.DataCell>
                <Table.DataCell>{vedtak.vedtakstatusKode}</Table.DataCell>
                <Table.DataCell>{vedtak.etterbetaling ? 'Ja' : 'Nei'}</Table.DataCell>
                <Table.DataCell>{vedtak.utvidetSamordningsfrist ? 'Ja' : 'Nei'}</Table.DataCell>
                <div key={vedtak.samordningVedtakId}>
                  <Heading size="small" id="modal-heading">
                    Samordningsmeldinger
                  </Heading>

                  <Table>
                    <Table.Header>
                      <Table.Row>
                        <Table.HeaderCell>ID</Table.HeaderCell>
                        <Table.HeaderCell>Tjenestepensjon</Table.HeaderCell>
                        <Table.HeaderCell>Status</Table.HeaderCell>
                        <Table.HeaderCell>Sendt dato</Table.HeaderCell>
                        <Table.HeaderCell>Mottatt dato</Table.HeaderCell>
                        <Table.HeaderCell>Purret dato</Table.HeaderCell>
                        <Table.HeaderCell>Refusjonskrav</Table.HeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {!vedtak.samordningsmeldinger.length && (
                        <Table.Row>
                          <Table.DataCell colSpan={7}>Ingen samordningsmeldinger funnet</Table.DataCell>
                        </Table.Row>
                      )}
                      {vedtak.samordningsmeldinger.map((mld) => (
                        <Table.Row key={mld.samId}>
                          <Table.DataCell>{mld.samId}</Table.DataCell>
                          <Table.DataCell>
                            {mld.tpNr} {mld.tpNavn}
                          </Table.DataCell>
                          <Table.DataCell>{mld.meldingstatusKode}</Table.DataCell>
                          <Table.DataCell>{mld.sendtDato}</Table.DataCell>
                          <Table.DataCell>{mld.svartDato && formaterStringDato(mld.svartDato)}</Table.DataCell>
                          <Table.DataCell>{mld.purretDato && formaterStringDato(mld.purretDato)}</Table.DataCell>
                          <Table.DataCell>{mld.refusjonskrav}</Table.DataCell>
                        </Table.Row>
                      ))}
                    </Table.Body>
                  </Table>
                </div>
              </Table.Row>
            ))}
          </Table.Body>
        </Table>
      )}
    </Container>
  )
}
