import {
  Control,
  Controller,
  FieldArrayWithId,
  FieldError,
  FieldErrorsImpl,
  Merge,
  UseFormRegister,
  UseFormWatch,
} from 'react-hook-form'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { Button, Select, TextField } from '@navikt/ds-react'
import { InstitusjonsoppholdGrunnlagData, Reduksjon } from '~shared/types/Beregning'
import React from 'react'
import styled from 'styled-components'

type InstitusjonsoppholdPerioder = {
  item: FieldArrayWithId<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlagData }, 'institusjonsOppholdForm', 'id'>
  index: number
  control: Control<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlagData }>
  register: UseFormRegister<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlagData }>
  remove: (index: number) => void
  watch: UseFormWatch<{ institusjonsOppholdForm: InstitusjonsoppholdGrunnlagData }>
  setVisFeil: (truefalse: boolean) => void
  errors:
    | Merge<
        FieldError,
        FieldErrorsImpl<{
          fom: Date
          tom: Date
          data: { reduksjon: string; egenReduksjon: number; begrunnelse: string }
        }>
      >
    | undefined
}

const InstitusjonsoppholdPeriode = (props: InstitusjonsoppholdPerioder) => {
  const { item, index, control, register, remove, watch, setVisFeil, errors } = props
  watch(`institusjonsOppholdForm.${index}.data.reduksjon`)
  return (
    <>
      <div key={item.id} id={`institusjonsopphold.${index}`}>
        <InstitusjonsperioderWrapper>
          <Controller
            name={`institusjonsOppholdForm.${index}.fom`}
            control={control}
            rules={{ required: true }}
            render={(fom) => (
              <DatoSection>
                <MaanedVelger
                  label="Fra og med"
                  value={fom.field.value}
                  onChange={(date: Date | null) => fom.field.onChange(date)}
                />
              </DatoSection>
            )}
          />
          <Controller
            name={`institusjonsOppholdForm.${index}.tom`}
            control={control}
            render={(tom) => (
              <DatoSection>
                <MaanedVelger
                  label="Til og med"
                  value={tom.field.value}
                  onChange={(date: Date | null) => tom.field.onChange(date)}
                />
              </DatoSection>
            )}
          />
          <Select
            error={errors?.data?.reduksjon?.message}
            label="Reduksjon"
            {...register(`institusjonsOppholdForm.${index}.data.reduksjon`, {
              required: { value: true, message: 'Feltet er påkrevd' },
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
              error={errors?.data?.egenReduksjon?.message}
              label="Reduksjonsbeløp(oppgi i % av G)"
              type="text"
              {...register(`institusjonsOppholdForm.${index}.data.egenReduksjon`, {
                validate: (beloep) => {
                  if (!beloep?.match(/[0-9]*/)?.[0]) {
                    return 'Beløp kan kun være heltall'
                  }
                },
              })}
            />
          )}
        </InstitusjonsperioderWrapper>
        <TextField
          label="Begrunnelse for periode(hvis aktuelt)"
          {...register(`institusjonsOppholdForm.${index}.data.begrunnelse`)}
        />
        <Button
          onClick={() => {
            setVisFeil(false)
            remove(index)
          }}
        >
          Fjern opphold
        </Button>
      </div>
    </>
  )
}

const DatoSection = styled.section`
  display: grid;
  gap: 0.5em;
`

const InstitusjonsperioderWrapper = styled.div`
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

export default InstitusjonsoppholdPeriode
