import React, { Dispatch, SetStateAction } from 'react'
import { Table } from '@navikt/ds-react'
import { ILand, ITrygdetid, ITrygdetidGrunnlag } from '~shared/api/trygdetid'
import { FaktiskTrygdetidExpandableRow } from '~components/behandling/trygdetid/faktiskTrygdetid/FaktiskTrygdetidExpandableRow'
import { VisRedigerTrygdetid } from '~components/behandling/trygdetid/faktiskTrygdetid/FaktiskTrygdetid'
import { Result } from '~shared/api/apiUtils'

interface Props {
  faktiskTrygdetidPerioder: Array<ITrygdetidGrunnlag>
  slettTrygdetid: (trygdetidGrunnlagId: string) => void
  slettTrygdetidResult: Result<ITrygdetid>
  setVisRedigerTrydgetid: Dispatch<SetStateAction<VisRedigerTrygdetid>>
  landListe: ILand[]
  redigerbar: boolean
}

export const FaktiskTrygdetidTable = ({
  faktiskTrygdetidPerioder,
  setVisRedigerTrydgetid,
  slettTrygdetid,
  slettTrygdetidResult,
  landListe,
  redigerbar,
}: Props) => {
  return (
    <Table size="small">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell />
          <Table.HeaderCell scope="col">Land</Table.HeaderCell>
          <Table.HeaderCell scope="col">Fra dato</Table.HeaderCell>
          <Table.HeaderCell scope="col">Til dato</Table.HeaderCell>
          <Table.HeaderCell scope="col">Faktisk trygdetid</Table.HeaderCell>
          <Table.HeaderCell scope="col">Kilde</Table.HeaderCell>
          {redigerbar && <Table.HeaderCell scope="col" />}
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {!!faktiskTrygdetidPerioder?.length ? (
          faktiskTrygdetidPerioder.map((faktiskTrygdetidPeriode: ITrygdetidGrunnlag, index: number) => {
            return (
              <FaktiskTrygdetidExpandableRow
                key={index}
                faktiskTrygdetidPeriode={faktiskTrygdetidPeriode}
                slettTrygdetid={slettTrygdetid}
                slettTrygdetidResult={slettTrygdetidResult}
                setVisRedigerTrydgetid={setVisRedigerTrydgetid}
                landListe={landListe}
                redigerbar={redigerbar}
              />
            )
          })
        ) : (
          <Table.Row>
            <Table.DataCell colSpan={redigerbar ? 7 : 6}>Ingen perioder for faktisk trygdetid</Table.DataCell>
          </Table.Row>
        )}
      </Table.Body>
    </Table>
  )
}
