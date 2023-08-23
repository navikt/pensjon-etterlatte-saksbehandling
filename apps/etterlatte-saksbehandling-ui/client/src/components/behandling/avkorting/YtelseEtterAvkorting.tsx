import { IAvkortetYtelse, IAvkorting } from '~shared/types/IAvkorting'
import { Heading, Table, TextField } from '@navikt/ds-react'
import React, { useState } from 'react'
import styled from 'styled-components'
import { ManglerRegelspesifikasjon } from '~components/behandling/felles/ManglerRegelspesifikasjon'
import { formaterStringDato, NOK } from '~utils/formattering'
import { YtelseEtterAvkortingDetaljer } from '~components/behandling/avkorting/YtelseEtterAvkortingDetaljer'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreManuellRestanse } from '~shared/api/avkorting'

export const YtelseEtterAvkorting = (props: {
  ytelser: IAvkortetYtelse[]
  tidligereYtelser: IAvkortetYtelse[]
  behandling: IBehandlingReducer
  setAvkorting: (avkorting: IAvkorting) => void
}) => {
  const ytelser = [...props.ytelser]
  ytelser.sort((a, b) => new Date(b.fom).getTime() - new Date(a.fom).getTime())

  const [manuellRestanseStatus, requestLagreManuellRestanse] = useApiCall(lagreManuellRestanse)

  const submitRestanse = (avkortetYtelseId: string, nyRestanse: number) =>
    requestLagreManuellRestanse(
      {
        behandlingId: props.behandling.id,
        avkortetYtelseId: avkortetYtelseId,
        nyRestanse: nyRestanse,
      },
      (respons) => {
        props.setAvkorting(respons)
      }
    )

  const erTilbakeITid = () => {
    const vedtak = props.behandling.vedtak?.datoAttestert
    const virkningstidspunkt = props.behandling.virkningstidspunkt?.dato
    if (!virkningstidspunkt) throw new Error('Mangler virkningstidspunkt')
    return new Date(virkningstidspunkt).getTime() < (vedtak ? new Date(vedtak) : new Date()).getTime()
  }

  return (
    <>
      {ytelser.length > 0 && (
        <TableWrapper>
          <Heading spacing size="small" level="2">
            Beregning etter avkorting
          </Heading>
          <Table className="table" zebraStripes>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell />
                {erTilbakeITid() ? <Table.HeaderCell>Type</Table.HeaderCell> : ''}
                <Table.HeaderCell>Periode</Table.HeaderCell>
                <Table.HeaderCell>Avkorting</Table.HeaderCell>
                <Table.HeaderCell>Restanse</Table.HeaderCell>
                <Table.HeaderCell>Brutto stønad etter avkorting</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {ytelser.map((ytelse, key) => {
                const tidligereYtelse = props.tidligereYtelser.find((tidligere) => tidligere.fom === ytelse.fom)
                // TODO Flett liste fordi dette fører til uniqe key feil
                return (
                  <>
                    <YtelseRad
                      key={ytelse.type + '-' + key}
                      radKey={ytelse.type + '-' + key}
                      erTilbakeITid={erTilbakeITid()}
                      ytelse={ytelse}
                      submit={submitRestanse}
                    />
                    {erTilbakeITid() ? (
                      <YtelseRad
                        key={tidligereYtelse!.type + '-' + key}
                        radKey={tidligereYtelse!.type + '-' + key}
                        erTilbakeITid={true}
                        ytelse={tidligereYtelse!}
                        submit={submitRestanse}
                      />
                    ) : (
                      ''
                    )}
                  </>
                )
              })}
            </Table.Body>
          </Table>
        </TableWrapper>
      )}
    </>
  )
}

const YtelseRad = (props: {
  radKey: string
  erTilbakeITid: boolean
  ytelse: IAvkortetYtelse
  submit: (avkortetYtelseId: string, nyRestanse: number) => void
}) => {
  const { radKey, erTilbakeITid, ytelse } = props
  const [manuellRestanse, setManuellRestanse] = useState<number>(ytelse.restanse)
  const restanseIKroner = ytelse.restanse < 0 ? `+ ${NOK(ytelse.restanse * -1)}` : `- ${NOK(ytelse.restanse)}`
  return (
    <Table.ExpandableRow key={radKey} shadeOnHover={false} content={<YtelseEtterAvkortingDetaljer ytelse={ytelse} />}>
      {erTilbakeITid ? <Table.DataCell>{ytelse.type}</Table.DataCell> : ''}
      <Table.DataCell>
        {formaterStringDato(ytelse.fom)} - {ytelse.tom ? formaterStringDato(ytelse.tom) : ''}
      </Table.DataCell>
      <Table.DataCell>{NOK(ytelse.avkortingsbeloep)}</Table.DataCell>
      <Table.DataCell>
        {erTilbakeITid && ytelse.type.toUpperCase() === 'NY' ? (
          <>
            <TextField
              label={''}
              size="small"
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={manuellRestanse}
              onChange={(e) => setManuellRestanse(Number(e.target.value))}
            />
            <button onClick={() => props.submit(ytelse.id, manuellRestanse)}>test</button>
          </>
        ) : (
          restanseIKroner
        )}
      </Table.DataCell>
      <Table.DataCell>
        <ManglerRegelspesifikasjon>{NOK(ytelse.ytelseEtterAvkorting)}</ManglerRegelspesifikasjon>
      </Table.DataCell>
    </Table.ExpandableRow>
  )
}

const TableWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  max-width: 1000px;

  .table {
    max-width: 1000px;

    .tableCell {
      max-width: 100px;
    }
  }
`
