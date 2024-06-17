import { IAvkortetYtelse } from '~shared/types/IAvkorting'
import { Box, Heading, Table } from '@navikt/ds-react'
import React from 'react'
import styled from 'styled-components'
import { formaterDato, formaterStringDato, NOK } from '~utils/formattering'
import { YtelseEtterAvkortingDetaljer } from '~components/behandling/avkorting/YtelseEtterAvkortingDetaljer'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { lastDayOfMonth } from 'date-fns'
import { useAppSelector } from '~store/Store'

const sorterNyligsteFoerstOgBakover = (a: IAvkortetYtelse, b: IAvkortetYtelse) =>
  new Date(b.fom).getTime() - new Date(a.fom).getTime()

export const YtelseEtterAvkorting = () => {
  const avkorting = useAppSelector((state) => state.behandlingReducer.behandling?.avkorting)
  const ytelser = [...(avkorting?.avkortetYtelse ?? [])].sort(sorterNyligsteFoerstOgBakover)
  const tidligereYtelser = [...(avkorting?.tidligereAvkortetYtelse ?? [])].sort(sorterNyligsteFoerstOgBakover)

  const finnTidligereTidligereYtelseIPeriode = (ytelse: IAvkortetYtelse) => {
    return tidligereYtelser.find(
      (tidligere) => ytelse.fom >= tidligere.fom && (tidligere.tom == null || ytelse.tom <= tidligere.tom)
    )
  }

  return (
    <>
      {ytelser.length > 0 && (
        <TableBox>
          <Heading spacing size="small" level="2">
            Beregning etter avkorting
          </Heading>
          <Table className="table" zebraStripes>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell />
                <Table.HeaderCell>Periode</Table.HeaderCell>
                <Table.HeaderCell>Beregning</Table.HeaderCell>
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
                      <BredCelle>
                        {formaterStringDato(ytelse.fom)} -{' '}
                        {ytelse.tom ? formaterDato(lastDayOfMonth(new Date(ytelse.tom))) : ''}
                      </BredCelle>
                    </Table.DataCell>
                    <Table.DataCell>
                      <SmalCelle>
                        <Info
                          tekst={NOK(ytelse.ytelseFoerAvkorting)}
                          label=""
                          undertekst={
                            tidligereYtelse ? `${NOK(tidligereYtelse.ytelseFoerAvkorting)} (Forrige vedtak)` : ''
                          }
                        />
                      </SmalCelle>
                    </Table.DataCell>
                    <Table.DataCell>
                      <SmalCelle>
                        <Info
                          tekst={NOK(ytelse.avkortingsbeloep)}
                          label=""
                          undertekst={
                            tidligereYtelse ? `${NOK(tidligereYtelse.avkortingsbeloep)} (Forrige vedtak)` : ''
                          }
                        />
                      </SmalCelle>
                    </Table.DataCell>
                    <Table.DataCell>
                      <SmalCelle>
                        <Info
                          tekst={restanseIKroner(ytelse.restanse)}
                          label=""
                          undertekst={
                            tidligereYtelse ? `${restanseIKroner(tidligereYtelse.restanse)} (Forrige vedtak)` : ''
                          }
                        />
                      </SmalCelle>
                    </Table.DataCell>
                    <Table.DataCell>
                      <BredCelle>
                        <Info
                          tekst={NOK(ytelse.ytelseEtterAvkorting)}
                          label=""
                          undertekst={
                            tidligereYtelse ? `${NOK(tidligereYtelse.ytelseEtterAvkorting)} (Forrige vedtak)` : ''
                          }
                        />
                      </BredCelle>
                    </Table.DataCell>
                  </Table.ExpandableRow>
                )
              })}
            </Table.Body>
          </Table>
        </TableBox>
      )}
    </>
  )
}

const TableBox = styled(Box)`
  max-width: 44rem;
`

const SmalCelle = styled(Box)`
  max-width: 9.35rem;
`

const BredCelle = styled(Box)`
  min-width: 14.05rem;
`
