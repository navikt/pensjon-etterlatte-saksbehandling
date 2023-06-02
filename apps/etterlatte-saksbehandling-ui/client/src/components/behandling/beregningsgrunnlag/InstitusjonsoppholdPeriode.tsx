import { Control, Controller, FieldArrayWithId, UseFormRegister, UseFormWatch } from 'react-hook-form'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { Button, Select, TextField } from '@navikt/ds-react'
import { InstitusjonsoppholdGrunnlag, Reduksjon } from '~shared/types/Beregning'
import React from 'react'
import styled from 'styled-components'

type InstitusjonsoppholdPerioder = {
  item: FieldArrayWithId<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlag }, 'institusjonsOppholdForm', 'id'>
  index: number
  control: Control<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlag }>
  register: UseFormRegister<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlag }>
  remove: (index: number) => void
  watch: UseFormWatch<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlag }>
}

const InstitusjonsoppholdPeriode = (props: InstitusjonsoppholdPerioder) => {
  const { item, index, control, register, remove, watch } = props
  watch(`institusjonsOppholdForm.${index}.data.reduksjon`)
  return (
    <>
      <div key={item.id}>
        <InstitusjonsperioderWrapper>
          <Controller
            name={`institusjonsOppholdForm.${index}.fom`}
            control={control}
            rules={{ required: true }}
            render={(fom) => (
              <MaanedVelger
                label="Fra og med"
                value={fom.field.value}
                onChange={(date: Date | null) => fom.field.onChange(date)}
              />
            )}
          />
          <Controller
            name={`institusjonsOppholdForm.${index}.tom`}
            control={control}
            render={(tom) => (
              <MaanedVelger
                label="Til og med"
                value={tom.field.value}
                onChange={(date: Date | null) => tom.field.onChange(date)}
              />
            )}
          />
          <Select
            label="Reduksjon"
            {...register(`institusjonsOppholdForm.${index}.data.reduksjon`, {
              required: true,
              validate: { notDefault: (v) => v !== 'VELG_REDUKSJON' },
            })}
          >
            <option value="">Velg reduksjon</option>
            {Object.entries(Reduksjon).map(([reduksjonsKey, reduksjontekst]) => (
              <option key={reduksjonsKey} value={reduksjonsKey}>
                {reduksjontekst}
              </option>
            ))}
          </Select>
          {item.data.reduksjon === 'JA_EGEN_PROSENT_AV_G' && (
            <TextField
              label="Reduksjonsbeløp"
              inputMode="numeric"
              pattern="[0-9]*"
              {...register(`institusjonsOppholdForm.${index}.data.egenReduksjon`)}
            />
          )}
        </InstitusjonsperioderWrapper>
        <TextField
          label="Begrunnelse for periode(hvis aktuelt)"
          {...register(`institusjonsOppholdForm.${index}.data.begrunnelse`)}
        />
        <Button onClick={() => remove(index)}>Fjern opphold</Button>
      </div>
    </>
  )
}

const InstitusjonsperioderWrapper = styled.div`
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

export default InstitusjonsoppholdPeriode
