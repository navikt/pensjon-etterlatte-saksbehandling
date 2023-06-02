import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { IBehandlingReducer, oppdaterBeregingsGrunnlag } from '~store/reducers/BehandlingReducer'
import { Soesken } from '~components/behandling/soeknadsoversikt/familieforhold/personer/Soesken'
import { Control, Controller, useFieldArray, useForm, UseFormWatch } from 'react-hook-form'
import { BodyShort, Button, ErrorSummary, Heading, Label, Radio, RadioGroup } from '@navikt/ds-react'
import styled from 'styled-components'
import { IPdlPerson } from '~shared/types/Person'
import { addMonths, format } from 'date-fns'
import PeriodeAccordion from '~components/behandling/beregningsgrunnlag/PeriodeAccordion'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { SoeskenMedIBeregning } from '~shared/types/Beregning'
import { hentBeregningsGrunnlag } from '~shared/api/beregning'
import { Barn } from '~components/behandling/soeknadsoversikt/familieforhold/personer/Barn'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ContentHeader } from '~shared/styled'
import { FamilieforholdWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import {
  FeilIPeriode,
  mapListeFraDto,
  PeriodisertBeregningsgrunnlag,
  feilIKomplettePerioderOverIntervall,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { useAppDispatch } from '~store/Store'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'

type SoeskenKanskjeMedIBeregning = {
  foedselsnummer: string
  skalBrukes?: boolean
}

export type Soeskengrunnlag = PeriodisertBeregningsgrunnlag<SoeskenMedIBeregning[]>[]

export type SoeskengrunnlagUtfylling = PeriodisertBeregningsgrunnlag<SoeskenKanskjeMedIBeregning[]>[]

type SoeskenjusteringProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: Soeskengrunnlag) => void
}

const nySoeskengrunnlagPeriode = (soesken: IPdlPerson[], fom?: string) => ({
  fom: fom !== undefined ? new Date(fom) : new Date(),
  data: soesken.map((barn) => ({
    foedselsnummer: barn.foedselsnummer,
    skalBrukes: undefined,
  })),
})

const Soeskenjustering = (props: SoeskenjusteringProps) => {
  const { behandling, onSubmit } = props
  const [visFeil, setVisFeil] = useState(false)
  const { handleSubmit, reset, control, watch } = useForm<{
    soeskenMedIBeregning: PeriodisertBeregningsgrunnlag<SoeskenKanskjeMedIBeregning[]>[]
  }>({
    defaultValues: { soeskenMedIBeregning: mapListeFraDto(behandling.beregningsGrunnlag?.soeskenMedIBeregning ?? []) },
  })
  const { fields, append, remove } = useFieldArray({
    name: 'soeskenMedIBeregning',
    control,
  })

  const soeskenjustering = behandling.beregningsGrunnlag?.soeskenMedIBeregning
  const [soeskenjusteringGrunnlag, fetchSoeskengjusteringGrunnlag] = useApiCall(hentBeregningsGrunnlag)
  const soeskenjusteringErDefinertIRedux = soeskenjustering !== undefined
  const behandles = hentBehandlesFraStatus(behandling?.status)
  const sisteTom = watch(`soeskenMedIBeregning.${fields.length - 1}.tom`)
  const sisteFom = watch(`soeskenMedIBeregning.${fields.length - 1}.fom`)
  const dispatch = useAppDispatch()

  useEffect(() => {
    if (!soeskenjusteringErDefinertIRedux) {
      fetchSoeskengjusteringGrunnlag(behandling.id, (result) => {
        if (result === null) {
          reset({ soeskenMedIBeregning: [nySoeskengrunnlagPeriode(soesken, behandling.virkningstidspunkt?.dato)] })
        } else {
          reset({ soeskenMedIBeregning: mapListeFraDto(result.soeskenMedIBeregning) })
          dispatch(oppdaterBeregingsGrunnlag(result))
        }
      })
    }
  }, [])

  if (!behandling.familieforhold) {
    return null
  }

  const allePerioder = watch('soeskenMedIBeregning')
  const feil: [number, FeilISoeskenPeriode][] = [
    ...feilIKomplettePerioderOverIntervall(allePerioder, new Date(behandling.virkningstidspunkt!.dato)),
    ...allePerioder.flatMap((periode, indeks) =>
      feilISoeskenjusteringsperiode(periode).map((feil) => [indeks, feil] as [number, FeilISoeskenPeriode])
    ),
  ]

  const soesken: IPdlPerson[] =
    behandling.familieforhold.avdoede.opplysning.avdoedesBarn?.filter(
      (barn) => barn.foedselsnummer !== behandling.søker?.foedselsnummer
    ) ?? []

  const fnrTilSoesken: Record<string, IPdlPerson> = soesken.reduce(
    (acc, next) => ({
      ...acc,
      [next.foedselsnummer]: next,
    }),
    {} as Record<string, IPdlPerson>
  )

  const ferdigstilleForm = (data: { soeskenMedIBeregning: SoeskengrunnlagUtfylling }) => {
    if (validerSoeskenjustering(data.soeskenMedIBeregning) && feil.length === 0) {
      setVisFeil(false)
      onSubmit(data.soeskenMedIBeregning)
    } else {
      setVisFeil(true)
    }
  }

  const doedsdato = behandling.familieforhold.avdoede.opplysning.doedsdato

  return (
    <>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="2" size="medium">
            Søskenjustering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <FamilieforholdWrapper>
        {behandling.søker && <Barn person={behandling.søker} doedsdato={doedsdato} />}
        <Border />
        <Spinner visible={isPending(soeskenjusteringGrunnlag)} label={'Henter beregningsgrunnlag for søsken'} />
        {isFailure(soeskenjusteringGrunnlag) && <ApiErrorAlert>Søskenjustering kan ikke hentes</ApiErrorAlert>}
      </FamilieforholdWrapper>
      {visFeil && feil.length > 0 && behandles ? <FeilIPerioder feil={feil} /> : null}
      <form id="formsoeskenjustering" name="formsoeskenjustering">
        {isSuccess(soeskenjusteringGrunnlag) || soeskenjusteringErDefinertIRedux ? (
          <>
            <UstiletListe>
              {fields.map((item, index) => (
                <SoeskenjusteringPeriode
                  key={item.id}
                  behandling={behandling}
                  control={control}
                  index={index}
                  remove={() => remove(index)}
                  canRemove={fields.length > 1}
                  fnrTilSoesken={fnrTilSoesken}
                  watch={watch}
                  visFeil={visFeil}
                  feil={feil}
                />
              ))}
            </UstiletListe>
            {behandles ? (
              <NyPeriodeButton
                onClick={() => append(nySoeskengrunnlagPeriode(soesken, addMonths(sisteTom || sisteFom, 1).toString()))}
              >
                Legg til periode
              </NyPeriodeButton>
            ) : null}
            <Button onClick={handleSubmit(ferdigstilleForm)}>Lagre søskenjustering</Button>
          </>
        ) : null}
      </form>
    </>
  )
}

const NyPeriodeButton = styled(Button).attrs({ size: 'small' })`
  margin: 1em 6em 1em 6em;
`

const validerSoeskenjustering = (grunnlag: SoeskengrunnlagUtfylling): grunnlag is Soeskengrunnlag => {
  return grunnlag.every(
    (value) =>
      value.data.every((barn) => barn.skalBrukes !== undefined && barn.skalBrukes !== null) &&
      feilISoeskenjusteringsperiode(value).length === 0
  )
}

const formaterMaanedDato = (fallback: string, dato: Date | null | undefined) => {
  if (dato) {
    return format(dato, 'MMMM yyyy')
  }
  return fallback
}

type FeilISoeskenPeriode = FeilIPeriode | 'IKKE_ALLE_VALGT'

export function feilISoeskenjusteringsperiode(
  grunnlag: PeriodisertBeregningsgrunnlag<SoeskenKanskjeMedIBeregning[]>
): FeilISoeskenPeriode[] {
  const feil: FeilISoeskenPeriode[] = []
  const alleErValgt = grunnlag.data.every((person) => person.skalBrukes !== undefined && person.skalBrukes !== null)
  if (!alleErValgt) {
    feil.push('IKKE_ALLE_VALGT')
  }
  if (grunnlag.tom !== undefined && grunnlag.tom < grunnlag.fom) {
    feil.push('TOM_FOER_FOM')
  }
  return feil
}

type SoeskenjusteringPeriodeProps = {
  control: Control<{ soeskenMedIBeregning: SoeskengrunnlagUtfylling }>
  index: number
  remove: () => void
  canRemove: boolean
  behandling: IBehandlingReducer
  fnrTilSoesken: Record<string, IPdlPerson>
  feil: [number, FeilISoeskenPeriode][]
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
                      placeholder="Ingen slutt"
                      value={tom.field.value}
                    />
                    {tom.field.value !== null && tom.field.value !== undefined ? (
                      <FjernKnapp onClick={() => tom.field.onChange(undefined)}>Fjern sluttdato</FjernKnapp>
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

const FeilForPeriode = (props: { feil: FeilISoeskenPeriode[] }) => {
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

const FeilIPerioder = (props: { feil: [number, FeilISoeskenPeriode][] }) => {
  return (
    <FeilIPerioderOppsummering heading="Du må fikse feil i periodiseringen før du kan beregne">
      {props.feil.map(([index, feil]) => (
        <ErrorSummary.Item key={`${index}${feil}`} href={`#soeskenjustering.${index}`}>
          {teksterFeilIPeriode[feil]}
        </ErrorSummary.Item>
      ))}
    </FeilIPerioderOppsummering>
  )
}

const teksterFeilIPeriode: Record<FeilISoeskenPeriode, string> = {
  INGEN_PERIODER: 'Minst en søskenjusteringsperiode må finnes',
  DEKKER_IKKE_SLUTT_AV_INTERVALL: 'Periodene må være komplette tilbake til virk',
  DEKKER_IKKE_START_AV_INTERVALL: 'Periodene må vare ut ytelsen',
  HULL_ETTER_PERIODE: 'Det er et hull i periodene etter denne perioden',
  PERIODE_OVERLAPPER_MED_NESTE: 'Perioden overlapper med neste periode',
  TOM_FOER_FOM: 'Til og med kan ikke være før fra og med',
  IKKE_ALLE_VALGT: 'Alle søsken må fylles ut',
} as const

const FeilIPerioderOppsummering = styled(ErrorSummary)`
  margin: 2em auto;
  width: 30em;
`

const VertikalMidtstiltBodyShort = styled(BodyShort)`
  margin: auto 0;
`

const FjernKnapp = styled(Button).attrs({ size: 'xsmall', variant: 'secondary' })`
  height: fit-content;
  width: fit-content;
  margin: auto 0;
`

const UstiletListe = styled.ul`
  list-style-type: none;
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

export default Soeskenjustering
