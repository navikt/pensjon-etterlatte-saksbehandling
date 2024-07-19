import { Control, Controller, FieldArrayWithId, UseFormRegister, UseFormWatch } from 'react-hook-form'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { Button, TextField, Textarea, Select } from '@navikt/ds-react'
import { OverstyrBeregingsperiodeGrunnlagData, OverstyrtAarsakType } from '~shared/types/Beregning'
import styled from 'styled-components'
import { TrashIcon } from '@navikt/aksel-icons'
import { FeilIPeriodeGrunnlagAlle, teksterFeilIPeriode } from './overstyrGrunnlagsBeregning/OverstyrBeregningGrunnlag'

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
  aarsaker: OverstyrtAarsakType
}

const OverstyrBeregningPeriode = (props: OverstyrBeregningPerioder) => {
  const { item, index, control, register, remove, visFeil, feil, behandles, aarsaker } = props

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
          <VerdiFelt>
            <TextField label="Utbetalt Beløp" {...register(`overstyrBeregningForm.${index}.data.utbetaltBeloep`)} />
          </VerdiFelt>
        </MinimimBeregningsperiodeWrapper>

        <MinimimBeregningsperiodeWrapper>
          <VerdiFelt>
            <TextField label="Anvendt Trygdetid" {...register(`overstyrBeregningForm.${index}.data.trygdetid`)} />
            <TextField
              label="Trygdetid tilhører FNR"
              {...register(`overstyrBeregningForm.${index}.data.trygdetidForIdent`)}
            />
          </VerdiFelt>
          <VerdiFelt>
            <TextField
              required
              label="Prorata Broek (valgfritt)"
              {...register(`overstyrBeregningForm.${index}.data.prorataBroekTeller`)}
            />
            <Deler>/</Deler>
            <TextField label="" {...register(`overstyrBeregningForm.${index}.data.prorataBroekNevner`)} />
          </VerdiFelt>
        </MinimimBeregningsperiodeWrapper>

        <MinimimBeregningsperiodeWrapper>
          <Select
            label="Årsak"
            {...register(`overstyrBeregningForm.${index}.data.aarsak`, {
              required: { value: true, message: 'Feltet er påkrevd' },
              validate: { notDefault: (v) => v !== 'VELG_AARSAK' },
            })}
          >
            {Object.entries(aarsaker).map(([key, value]) => (
              <option key={key} value={key}>
                {value}
              </option>
            ))}
          </Select>
        </MinimimBeregningsperiodeWrapper>

        <MinimimBeregningsperiodeWrapper>
          <Beskrivelse
            label="Beskrivelse"
            size="medium"
            {...register(`overstyrBeregningForm.${index}.data.beskrivelse`)}
          />
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
  justify-content: start;
  align-items: end;
  gap: 10px;
`

const VerdiFelt = styled.div`
  margin-bottom: 12px;
  display: flex;
  flex-direction: row;
  align-items: end;
  gap: 2px;
`

const Beskrivelse = styled(Textarea)`
  margin-top: 12px;
  margin-bottom: 12px;
`

const Deler = styled.div`
  margin-bottom: 12px;
`

export default OverstyrBeregningPeriode
