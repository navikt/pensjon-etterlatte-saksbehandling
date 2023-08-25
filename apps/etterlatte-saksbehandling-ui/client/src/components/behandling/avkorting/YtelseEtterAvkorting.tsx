import { IAvkortetYtelse, IAvkorting } from '~shared/types/IAvkorting'
import { Heading, Table } from '@navikt/ds-react'
import React from 'react'
import styled from 'styled-components'
import { ManglerRegelspesifikasjon } from '~components/behandling/felles/ManglerRegelspesifikasjon'
import { formaterStringDato, NOK } from '~utils/formattering'
import { YtelseEtterAvkortingDetaljer } from '~components/behandling/avkorting/YtelseEtterAvkortingDetaljer'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { Info } from '~components/behandling/soeknadsoversikt/Info'

const sorterNyligsteFoerstOgBakover = (a: IAvkortetYtelse, b: IAvkortetYtelse) =>
  new Date(b.fom).getTime() - new Date(a.fom).getTime()

export const YtelseEtterAvkorting = (props: {
  ytelser: IAvkortetYtelse[]
  tidligereYtelser: IAvkortetYtelse[]
  behandling: IBehandlingReducer
  setAvkorting: (avkorting: IAvkorting) => void
}) => {
  const ytelser = [...props.ytelser].sort(sorterNyligsteFoerstOgBakover)
  const tidligereYtelser = [...props.tidligereYtelser].sort(sorterNyligsteFoerstOgBakover)

  const finnTidligereTidligereYtelseIPeriode = (ytelse: IAvkortetYtelse) => {
    return tidligereYtelser.find(
      (tidligere) => ytelse.fom >= tidligere.fom && (tidligere.tom == null || ytelse.fom < tidligere.tom)
    )
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
                <Table.HeaderCell>Periode</Table.HeaderCell>
                <Table.HeaderCell>Avkorting</Table.HeaderCell>
                <Table.HeaderCell>Restanse</Table.HeaderCell>
                <Table.HeaderCell>Brutto stønad etter avkorting</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {ytelser.map((ytelse, key) => {
                const tidligereYtelse = finnTidligereTidligereYtelseIPeriode(ytelse)
                const restanseIKroner = (restanse: number) =>
                  restanse < 0 ? `+ ${NOK(restanse * -1)}` : `- ${NOK(restanse)}`

                return (
                  <Table.ExpandableRow
                    key={key}
                    shadeOnHover={false}
                    content={<YtelseEtterAvkortingDetaljer ytelse={ytelse} />}
                  >
                    <Table.DataCell>
                      {formaterStringDato(ytelse.fom)} - {ytelse.tom ? formaterStringDato(ytelse.tom) : ''}
                    </Table.DataCell>
                    <Table.DataCell>
                      <Info
                        tekst={NOK(ytelse.avkortingsbeloep)}
                        label={''}
                        undertekst={tidligereYtelse ? `${NOK(tidligereYtelse.avkortingsbeloep)} (Forrige vedtak)` : ''}
                      />
                    </Table.DataCell>
                    <Table.DataCell>
                      <Info
                        tekst={restanseIKroner(ytelse.restanse)}
                        label={''}
                        undertekst={
                          tidligereYtelse ? `${restanseIKroner(tidligereYtelse.restanse)} (Forrige vedtak)` : ''
                        }
                      />
                    </Table.DataCell>
                    <Table.DataCell>
                      <ManglerRegelspesifikasjon>
                        <Info
                          tekst={NOK(ytelse.ytelseEtterAvkorting)}
                          label={''}
                          undertekst={
                            tidligereYtelse ? `${NOK(tidligereYtelse.ytelseEtterAvkorting)} (Forrige vedtak)` : ''
                          }
                        />
                      </ManglerRegelspesifikasjon>
                    </Table.DataCell>
                  </Table.ExpandableRow>
                )
              })}
            </Table.Body>
          </Table>
        </TableWrapper>
      )}
    </>
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
