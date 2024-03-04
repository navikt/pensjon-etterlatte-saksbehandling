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
      <Heading size="medium">Samordningsmeldinger</Heading>

      <Spinner visible={isPending(hentet)} label="Henter data" />

      {isFailureHandler({
        apiResult: hentet,
        errorMessage: 'Kunne ikke hente data',
      })}

      {isSuccess(hentet) && samordningsdata && samordningsdata.length === 0 && (
        <Alert variant="info">Ingen samordningsdata</Alert>
      )}

      {isSuccess(hentet) && samordningsdata && (
        <Table zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>VedtaksID</Table.HeaderCell>
              <Table.HeaderCell>SamordningsID</Table.HeaderCell>
              <Table.HeaderCell>Tjenestepensjon</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
              <Table.HeaderCell>Sendt dato</Table.HeaderCell>
              <Table.HeaderCell>Mottatt dato</Table.HeaderCell>
              <Table.HeaderCell>Purret dato</Table.HeaderCell>
              <Table.HeaderCell>Refusjonskrav</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {!samordningsdata.length && (
              <Table.Row>
                <Table.DataCell colSpan={7}>Ingen samordningsmeldinger funnet</Table.DataCell>
              </Table.Row>
            )}
            {samordningsdata.map((vedtak) =>
              vedtak.samordningsmeldinger.map((mld) => (
                <Table.Row key={mld.samId}>
                  <Table.DataCell>{vedtak.vedtakId}</Table.DataCell>
                  <Table.DataCell>{mld.samId}</Table.DataCell>
                  <Table.DataCell>
                    {mld.tpNr} {mld.tpNavn}
                  </Table.DataCell>
                  <Table.DataCell>{mld.meldingstatusKode}</Table.DataCell>
                  <Table.DataCell>{formaterStringDato(mld.sendtDato)}</Table.DataCell>
                  <Table.DataCell>{mld.svartDato && formaterStringDato(mld.svartDato)}</Table.DataCell>
                  <Table.DataCell>{mld.purretDato && formaterStringDato(mld.purretDato)}</Table.DataCell>
                  <Table.DataCell>{mld.refusjonskrav}</Table.DataCell>
                </Table.Row>
              ))
            )}
          </Table.Body>
        </Table>
      )}
    </Container>
  )
}
