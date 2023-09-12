import InstitusjonsoppholdPeriode, {
  InstitusjonsoppholdPerioder,
} from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdPeriode'
import { useEffect, useState } from 'react'
import { Table } from '@navikt/ds-react'
import { PeriodeVisning } from '~components/behandling/beregningsgrunnlag/InstitusjonsoppholdPerioder'
import { Reduksjon } from '~shared/types/Beregning'

const InstitusjonsoppholdTableWrapper = (props: InstitusjonsoppholdPerioder) => {
  const { item, index, control, register, remove, watch, setVisFeil, errors, behandles } = props
  const content = (
    <InstitusjonsoppholdPeriode
      key={item.id}
      item={item}
      index={index}
      control={control}
      register={register}
      remove={remove}
      watch={watch}
      setVisFeil={setVisFeil}
      errors={errors}
      behandles={behandles}
    />
  )

  const [open, setIsOpen] = useState<boolean>(true)
  useEffect(() => {
    if (errors !== undefined) {
      setIsOpen(true)
    }
  }, [errors])
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
        <Table.DataCell>
          {Reduksjon[item.data.reduksjon]}
          {item.data.egenReduksjon && <p>Egen reduksjon: {item.data.egenReduksjon}</p>}
        </Table.DataCell>
        <Table.DataCell>{item.data.begrunnelse}</Table.DataCell>
      </Table.ExpandableRow>
    </>
  )
}

export default InstitusjonsoppholdTableWrapper
