import { useEffect, useState } from 'react'
import { Table } from '@navikt/ds-react'
import { PeriodeVisning } from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdPerioder'
import OverstyrBeregningPeriode, { OverstyrBeregningPerioder } from './OverstyrBeregningPeriode'

const OverstyrBeregningTableWrapper = (props: OverstyrBeregningPerioder) => {
  const { item, index, control, register, remove, watch, visFeil, feil, behandles } = props
  const content = (
    <OverstyrBeregningPeriode
      key={item.id}
      item={item}
      index={index}
      control={control}
      register={register}
      remove={remove}
      watch={watch}
      visFeil={visFeil}
      feil={feil}
      behandles={behandles}
    />
  )

  const [open, setIsOpen] = useState<boolean>(true)
  useEffect(() => {
    if (visFeil !== undefined) {
      setIsOpen(true)
    }
  }, [visFeil])
  return (
    <>
      <Table.ExpandableRow
        key={index + item.id}
        content={content}
        expandOnRowClick={true}
        open={open}
        onOpenChange={(openInternal) => setIsOpen(openInternal)}
      >
        <Table.DataCell scope="row">
          <PeriodeVisning fom={item.fom} tom={item.tom} />
        </Table.DataCell>
        <Table.DataCell>{item.data.utbetaltBeloep}</Table.DataCell>
        <Table.DataCell>{item.data.trygdetid}</Table.DataCell>
        <Table.DataCell>
          {item.data.prorataBroekTeller} / {item.data.prorataBroekNevner}
        </Table.DataCell>
        <Table.DataCell>{item.data.beskrivelse}</Table.DataCell>
      </Table.ExpandableRow>
    </>
  )
}

export default OverstyrBeregningTableWrapper
