import React, { useEffect, useState } from 'react'
import { Box, Heading, Link, Table } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSamordningsdataForSak } from '~shared/api/vedtaksvurdering'
import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { formaterStringDato } from '~utils/formattering'
import { SakMedBehandlinger } from '~components/person/typer'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Samordningsvedtak } from '~components/vedtak/typer'
import SamordningOppdaterMeldingModal from '~components/person/SamordningOppdaterMeldingModal'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

export const SamordningSak = ({ fnr, sakResult }: { fnr: string; sakResult: Result<SakMedBehandlinger> }) => {
  const [samordningdataStatus, hent] = useApiCall(hentSamordningsdataForSak)
  const [sakId, setSakId] = useState<number>()
  const visRedigeringsmulighet = useFeatureEnabledMedDefault('samordning-rediger-melding', false)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      setSakId(sakResult.data.sak.id)
      hent(sakResult.data.sak.id)
    }
  }, [sakResult])

  return (
    <Box padding="8">
      <Heading size="medium">Samordningsmeldinger</Heading>

      {mapResult(samordningdataStatus, {
        success: (data) => (
          <SamordningTabell
            fnr={fnr}
            sakId={sakId!}
            samordningsdata={data}
            refresh={() => hent(sakId!)}
            redigerbar={visRedigeringsmulighet}
          />
        ),
        pending: <Spinner visible={true} label="Henter samordningsdata" />,
        error: () => <ApiErrorAlert>Kunne ikke hente samordningsdata</ApiErrorAlert>,
      })}
    </Box>
  )
}

function SamordningTabell({
  fnr,
  sakId,
  samordningsdata,
  refresh,
  redigerbar,
}: {
  fnr: string
  sakId: number
  samordningsdata: Array<Samordningsvedtak>
  refresh: () => void
  redigerbar: boolean
}) {
  return samordningsdata.length == 0 ? (
    <p>Ingen samordningsmeldinger</p>
  ) : (
    <Table zebraStripes>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>VedtaksID</Table.HeaderCell>
          <Table.HeaderCell>Virkningstidspunkt</Table.HeaderCell>
          <Table.HeaderCell>SamordningsID</Table.HeaderCell>
          <Table.HeaderCell>Tjenestepensjon</Table.HeaderCell>
          <Table.HeaderCell>Status</Table.HeaderCell>
          <Table.HeaderCell>Sendt dato</Table.HeaderCell>
          <Table.HeaderCell>Mottatt dato</Table.HeaderCell>
          <Table.HeaderCell>Purret dato</Table.HeaderCell>
          <Table.HeaderCell>Refusjonskrav</Table.HeaderCell>
          {redigerbar && <Table.HeaderCell>Overstyr</Table.HeaderCell>}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {samordningsdata
          .sort((v1, v2) => v2.vedtakId - v1.vedtakId)
          .map((vedtak) =>
            vedtak.samordningsmeldinger.map((mld) => (
              <Table.Row key={mld.samId}>
                <Table.DataCell>
                  <Link href={`/behandling/${vedtak.behandlingId}`}>{vedtak.vedtakId}</Link>
                </Table.DataCell>
                <Table.DataCell>{vedtak.virkningFom}</Table.DataCell>
                <Table.DataCell>{mld.samId}</Table.DataCell>
                <Table.DataCell>
                  {mld.tpNr} {mld.tpNavn}
                </Table.DataCell>
                <Table.DataCell>{mld.meldingstatusKode}</Table.DataCell>
                <Table.DataCell>{formaterStringDato(mld.sendtDato)}</Table.DataCell>
                <Table.DataCell>{mld.svartDato && formaterStringDato(mld.svartDato)}</Table.DataCell>
                <Table.DataCell>{mld.purretDato && formaterStringDato(mld.purretDato)}</Table.DataCell>
                <Table.DataCell>{mld.svartDato && (mld.refusjonskrav ? 'Ja' : 'Nei')}</Table.DataCell>
                {redigerbar && (
                  <Table.DataCell>
                    {!mld.svartDato && (
                      <SamordningOppdaterMeldingModal
                        fnr={fnr}
                        sakId={sakId}
                        mld={mld}
                        vedtakId={vedtak.vedtakId}
                        refresh={refresh}
                      />
                    )}
                  </Table.DataCell>
                )}
              </Table.Row>
            ))
          )}
      </Table.Body>
    </Table>
  )
}
