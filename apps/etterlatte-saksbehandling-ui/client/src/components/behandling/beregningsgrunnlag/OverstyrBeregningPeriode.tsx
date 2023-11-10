import { Control, Controller, FieldArrayWithId, UseFormRegister, UseFormWatch } from 'react-hook-form'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { Button, TextField } from '@navikt/ds-react'
import { OverstyrBeregingsperiodeGrunnlagData } from '~shared/types/Beregning'
import styled from 'styled-components'
import { TrashIcon } from '@navikt/aksel-icons'
import { FeilIPeriodeGrunnlagAlle, teksterFeilIPeriode } from './OverstyrBeregningGrunnlag'

export type OverstyrBeregningPerioder = {
  item: FieldArrayWithId<{ overstyrBeregningForm: OverstyrBeregingsperiodeGrunnlagData }, 'overstyrBeregningForm', 'id'>
  index: number
  control: Control<{ overstyrBeregningForm: OverstyrBeregingsperiodeGrunnlagData }>
  register: UseFormRegister<{ overstyrBeregningForm: OverstyrBeregingsperiodeGrunnlagData }>
  remove: (index: number) => void
  watch: UseFormWatch<{ overstyrBeregningForm: OverstyrBeregingsperiodeGrunnlagData }>
  visFeil: boolean
  feil: [number, FeilIPeriodeGrunnlagAlle][]
  behandles: boolean
}

const OverstyrBeregningPeriode = (props: OverstyrBeregningPerioder) => {
  const { item, index, control, register, remove, visFeil, feil, behandles } = props

  const mineFeil = [...feil.filter(([feilIndex]) => feilIndex === index).flatMap((a) => a[1])]

  return (
    <>
      <div key={item.id} id={`minimumBeregningsperiode.${index}`}>
        <MinimimBeregningsperiodeWrapper>
          <Controller
            name={`overstyrBeregningForm.${index}.fom`}
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
            name={`overstyrBeregningForm.${index}.tom`}
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
          <VerdiFelt label="Utbetalt Beløp" {...register(`overstyrBeregningForm.${index}.data.utbetaltBeloep`)} />
          <VerdiFelt label="Anvendt Trygdetid" {...register(`overstyrBeregningForm.${index}.data.trygdetid`)} />
        </MinimimBeregningsperiodeWrapper>
        {mineFeil.length > 0 && visFeil ? <FeilForPeriode feil={mineFeil} /> : null}
        {behandles && (
          <ButtonMarginTop
            icon={<TrashIcon />}
            variant="tertiary"
            onClick={() => {
              remove(index)
            }}
          >
            Fjern beregningsperiode
          </ButtonMarginTop>
        )}
      </div>
    </>
  )
}

const FeilForPeriode = (props: { feil: FeilIPeriodeGrunnlagAlle[] }) => {
  return (
    <>
      {props.feil.map((feil) => (
        <FeilContainer key={feil}>{teksterFeilIPeriode[feil]}</FeilContainer>
      ))}
    </>
  )
}

const FeilContainer = styled.span`
  margin-top: 0.5em;
  word-wrap: break-word;
  display: block;
`

const ButtonMarginTop = styled(Button)`
  margin-top: 1rem;
`

const DatoSection = styled.section`
  display: grid;
  gap: 0.5em;
`

const MinimimBeregningsperiodeWrapper = styled.div`
  width: 100%;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`

const VerdiFelt = styled(TextField)`
  margin-bottom: 12px;
`

export default OverstyrBeregningPeriode
