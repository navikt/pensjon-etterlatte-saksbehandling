import { Control, Controller, useFieldArray, UseFormWatch } from 'react-hook-form'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import PeriodeAccordion from '~components/behandling/beregningsgrunnlag/PeriodeAccordion'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { BodyShort, Button, Label, Radio, RadioGroup } from '@navikt/ds-react'
import { format } from 'date-fns'
import { Soesken } from '~components/behandling/soeknadsoversikt/familieforhold/personer/Soesken'
import React from 'react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { IPdlPerson } from '~shared/types/Person'
import {
  FeilIPeriodeGrunnlagAlle,
  SoeskengrunnlagUtfylling,
  teksterFeilIPeriode,
  UstiletListe,
} from '~components/behandling/beregningsgrunnlag/soeskenjustering/Soeskenjustering'
import styled from 'styled-components'

type SoeskenjusteringPeriodeProps = {
  control: Control<{ soeskenMedIBeregning: SoeskengrunnlagUtfylling }>
  index: number
  remove: () => void
  canRemove: boolean
  behandling: IBehandlingReducer
  fnrTilSoesken: Record<string, IPdlPerson>
  feil: [number, FeilIPeriodeGrunnlagAlle][]
  watch: UseFormWatch<{ soeskenMedIBeregning: SoeskengrunnlagUtfylling }>
  visFeil: boolean
}

const SoeskenjusteringPeriode = (props: SoeskenjusteringPeriodeProps) => {
  const { control, index, remove, fnrTilSoesken, canRemove, behandling, watch, visFeil, feil } = props
  const { fields } = useFieldArray({
    name: `soeskenMedIBeregning.${index}.data`,
    control,
  })
  const behandles = hentBehandlesFraStatus(behandling?.status)

  const grunnlag = watch(`soeskenMedIBeregning.${index}`)
  const mineFeil = [...feil.filter(([feilIndex]) => feilIndex === index).flatMap((a) => a[1])]

  const soeskenIPeriode = grunnlag.data
  const antallSoeskenMed = soeskenIPeriode.filter((soesken) => soesken.skalBrukes === true).length
  const antallSoeskenIkkeMed = soeskenIPeriode.filter((soesken) => soesken.skalBrukes === false).length
  const antallSoeskenIkkeValgt = soeskenIPeriode.filter(
    (soesken) => soesken.skalBrukes === undefined || soesken.skalBrukes === null
  ).length

  return (
    <PeriodeAccordion
      id={`soeskenjustering.${index}`}
      title={`Periode ${index + 1}`}
      titleHeadingLevel="3"
      feilBorder={visFeil && mineFeil.length > 0}
      topSummary={(expanded) => (
        <PeriodeInfo>
          <div>
            <Controller
              render={(fom) =>
                expanded && behandles ? (
                  <MaanedVelger
                    label="Fra og med"
                    value={fom.field.value}
                    onChange={(date: Date | null) => fom.field.onChange(date)}
                  />
                ) : (
                  <OppdrasSammenLes>
                    <Label>Fra og med</Label>
                    <BodyShort>{format(fom.field.value, 'MMMM yyyy')}</BodyShort>
                  </OppdrasSammenLes>
                )
              }
              name={`soeskenMedIBeregning.${index}.fom`}
              control={control}
            />
          </div>
          <div>
            <Controller
              render={(tom) =>
                expanded && behandles ? (
                  <MaanedvelgerMedUtnulling>
                    <MaanedVelger
                      onChange={(val) => tom.field.onChange(val)}
                      label="Til og med"
                      value={tom.field.value}
                    />
                    {tom.field.value !== null && tom.field.value !== undefined ? (
                      <FjernKnapp type="button" onClick={() => tom.field.onChange(undefined)}>
                        Fjern sluttdato
                      </FjernKnapp>
                    ) : null}
                  </MaanedvelgerMedUtnulling>
                ) : (
                  <OppdrasSammenLes>
                    <Label>Til og med</Label>
                    <BodyShort>{formaterMaanedDato('Ingen slutt', tom.field.value)}</BodyShort>
                  </OppdrasSammenLes>
                )
              }
              name={`soeskenMedIBeregning.${index}.tom`}
              control={control}
            />
          </div>
          <VertikalMidtstiltBodyShort>
            {antallSoeskenMed} i beregning, {antallSoeskenIkkeMed} ikke i beregning{' '}
            {antallSoeskenIkkeValgt ? <span>({antallSoeskenIkkeValgt} ikke valgt)</span> : null}
            {mineFeil.length > 0 && visFeil ? <FeilForPeriode feil={mineFeil} /> : null}
          </VertikalMidtstiltBodyShort>
          {canRemove && behandles ? <FjernKnapp onClick={remove}>Slett</FjernKnapp> : null}
        </PeriodeInfo>
      )}
    >
      <UstiletListe>
        {fields.map((item, k) => {
          return (
            <li key={item.id}>
              <SoeskenContainer>
                <Soesken person={fnrTilSoesken[item.foedselsnummer]} familieforhold={behandling.familieforhold!} />
                <Controller
                  name={`soeskenMedIBeregning.${index}.data.${k}`}
                  control={control}
                  render={(soesken) =>
                    behandles ? (
                      <RadioGroupRow
                        legend="Oppdras sammen"
                        value={soesken.field.value?.skalBrukes ?? null}
                        onChange={(value) => {
                          soesken.field.onChange({
                            foedselsnummer: item.foedselsnummer,
                            skalBrukes: value,
                          })
                        }}
                      >
                        <Radio value={true}>Ja</Radio>
                        <Radio value={false}>Nei</Radio>
                      </RadioGroupRow>
                    ) : (
                      <OppdrasSammenLes>
                        <strong>Oppdras sammen</strong>
                        <label>{soesken.field.value?.skalBrukes ? 'Ja' : 'Nei'}</label>
                      </OppdrasSammenLes>
                    )
                  }
                />
              </SoeskenContainer>
            </li>
          )
        })}
      </UstiletListe>
    </PeriodeAccordion>
  )
}

const formaterMaanedDato = (fallback: string, dato: Date | null | undefined) => {
  if (dato) {
    return format(dato, 'MMMM yyyy')
  }
  return fallback
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

const VertikalMidtstiltBodyShort = styled(BodyShort)`
  margin: auto 0;
`

const FjernKnapp = styled(Button).attrs({ size: 'xsmall', variant: 'secondary' })`
  height: fit-content;
  width: fit-content;
  margin: auto 0;
`

const PeriodeInfo = styled.div`
  max-width: 80em;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 5em;
  grid-gap: 1em;
`

const OppdrasSammenLes = styled.div`
  display: flex;
  flex-direction: column;
`

const SoeskenContainer = styled.div`
  display: flex;
  align-items: center;
`

const MaanedvelgerMedUtnulling = styled.div`
  display: flex;
  justify-content: flex-start;
  flex-direction: row;
  gap: 1em;
`

const RadioGroupRow = styled(RadioGroup)`
  margin-top: 1.2em;

  .navds-radio-buttons {
    display: flex;
    flex-direction: row;
    gap: 12px;
  }

  legend {
    padding-top: 9px;
  }
`

export default SoeskenjusteringPeriode
