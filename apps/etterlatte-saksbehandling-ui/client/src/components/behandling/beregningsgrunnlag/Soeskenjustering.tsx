import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { Soesken } from '~components/behandling/soeknadsoversikt/familieforhold/personer/Soesken'
import { Control, Controller, useFieldArray, useForm, UseFormWatch } from 'react-hook-form'
import { BodyShort, Button, Heading, Label, Radio, RadioGroup } from '@navikt/ds-react'
import styled from 'styled-components'
import { IPdlPerson } from '~shared/types/Person'
import { addMonths, format } from 'date-fns'
import PeriodeAccordion from '~components/behandling/beregningsgrunnlag/PeriodeAccordion'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { SoeskenMedIBeregning } from '~shared/types/Beregning'
import { hentSoeskenjusteringsgrunnlag } from '~shared/api/beregning'
import { Barn } from '~components/behandling/soeknadsoversikt/familieforhold/personer/Barn'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ContentHeader } from '~shared/styled'
import { FamilieforholdWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import {
  FeilIPeriode,
  PeriodisertBeregningsgrunnlag,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'

type SoeskenKanskjeMedIBeregning = {
  foedselsnummer: string
  skalBrukes?: boolean
}

const defaultValues: { soeskengrunnlag: SoeskengrunnlagUtfylling } = {
  soeskengrunnlag: [],
}

export type Soeskengrunnlag = PeriodisertBeregningsgrunnlag<SoeskenMedIBeregning[]>[]

export type SoeskengrunnlagUtfylling = PeriodisertBeregningsgrunnlag<SoeskenKanskjeMedIBeregning[]>[]

type SoeskenjusteringProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: Soeskengrunnlag) => void
}

const nySoeskengrunnlagPeriode = (soesken: IPdlPerson[], fom?: string) => ({
  fom: fom !== undefined ? new Date(fom) : new Date(),
  harEnSlutt: false,
  data: soesken.map((barn) => ({
    foedselsnummer: barn.foedselsnummer,
    skalBrukes: undefined,
  })),
})

const Soeskenjustering = (props: SoeskenjusteringProps) => {
  const { behandling, onSubmit } = props
  const { handleSubmit, reset, control, watch } = useForm({ defaultValues })
  const { fields, append, remove } = useFieldArray({
    name: 'soeskengrunnlag',
    control,
  })
  const soeskenjustering = behandling.beregningsGrunnlag?.soeskenMedIBeregning
  const [soeskenjusteringGrunnlag, fetchSoeskengjusteringGrunnlag] = useApiCall(hentSoeskenjusteringsgrunnlag)
  const soeskenjusteringErDefinertIRedux = soeskenjustering !== undefined

  const sisteTom = watch(`soeskengrunnlag.${fields.length - 1}.tom`)
  const sisteFom = watch(`soeskengrunnlag.${fields.length - 1}.fom`)
  if (!behandling || !behandling.familieforhold) {
    return null
  }
  const soesken: IPdlPerson[] =
    behandling.familieforhold.avdoede.opplysning.avdoedesBarn?.filter(
      (barn) => barn.foedselsnummer !== behandling.søker?.foedselsnummer
    ) ?? []

  useEffect(() => {
    if (!soeskenjusteringErDefinertIRedux) {
      fetchSoeskengjusteringGrunnlag(behandling.id, (result) => {
        const nyttGrunnlag = result?.soeskenMedIBeregning ?? [
          nySoeskengrunnlagPeriode(soesken, behandling.virkningstidspunkt?.dato),
        ]
        reset({
          soeskengrunnlag: nyttGrunnlag,
        })
      })
    }
  }, [])

  const fnrTilSoesken: Record<string, IPdlPerson> = soesken.reduce(
    (acc, next) => ({
      ...acc,
      [next.foedselsnummer]: next,
    }),
    {} as Record<string, IPdlPerson>
  )

  const submitForm = (data: { soeskengrunnlag: SoeskengrunnlagUtfylling }) => {
    if (validerSoeskenjustering(data.soeskengrunnlag)) {
      onSubmit(data.soeskengrunnlag)
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
      <form id="form" name="form" onSubmit={handleSubmit(submitForm)}>
        {isSuccess(soeskenjusteringGrunnlag) ? (
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
                />
              ))}
            </UstiletListe>
            <NyPeriodeButton
              onClick={() => append(nySoeskengrunnlagPeriode(soesken, addMonths(sisteTom || sisteFom, 1).toString()))}
            >
              Legg til periode
            </NyPeriodeButton>
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
  return grunnlag.every((value) =>
    value.data.every((barn) => barn.skalBrukes !== undefined && barn.skalBrukes !== null)
  )
}

const formaterMaanedDato = (fallback: string, dato: Date | null | undefined) => {
  if (dato) {
    return format(dato, 'MMMM yyyy')
  }
  return fallback
}

type SoeskenjusteringPeriodeProps = {
  control: Control<{ soeskengrunnlag: SoeskengrunnlagUtfylling }>
  index: number
  remove: () => void
  canRemove: boolean
  behandling: IBehandlingReducer
  fnrTilSoesken: Record<string, IPdlPerson>
  watch: UseFormWatch<{ soeskengrunnlag: SoeskengrunnlagUtfylling }>
}

type FeilISoeskenPeriode = FeilIPeriode | 'IKKE_ALLE_VALGT'

export function validerSoeskenjusteringsperiode(
  grunnlag: PeriodisertBeregningsgrunnlag<SoeskenKanskjeMedIBeregning[]>
): FeilISoeskenPeriode[] {
  const feil: FeilISoeskenPeriode[] = []
  const alleErValgt = grunnlag.data.every((person) => person.skalBrukes !== undefined && person.skalBrukes !== null)
  if (!alleErValgt) {
    feil.push('IKKE_ALLE_VALGT')
  }
  if (grunnlag.tom !== undefined && grunnlag.tom > grunnlag.fom) {
    feil.push('TOM_FOER_FOM')
  }
  return feil
}

const SoeskenjusteringPeriode = (props: SoeskenjusteringPeriodeProps) => {
  const { control, index, remove, fnrTilSoesken, canRemove, behandling, watch } = props
  const { fields } = useFieldArray({
    name: `soeskengrunnlag.${index}.data`,
    control,
  })

  const grunnlag = watch(`soeskengrunnlag.${index}`)
  const soeskenIPeriode = grunnlag.data
  const antallSoeskenMed = soeskenIPeriode.filter((soesken) => soesken.skalBrukes === true).length
  const antallSoeskenIkkeMed = soeskenIPeriode.filter((soesken) => soesken.skalBrukes === false).length
  const antallSoeskenIkkeValgt = soeskenIPeriode.filter(
    (soesken) => soesken.skalBrukes === undefined || soesken.skalBrukes === null
  ).length

  return (
    <PeriodeAccordion
      title={`Periode ${index + 1}`}
      titleHeadingLevel="3"
      topSummary={(expanded) => (
        <PeriodeInfo>
          <div>
            <Controller
              render={(field) =>
                expanded ? (
                  <MaanedVelger
                    label="Fra og med"
                    value={field.field.value}
                    onChange={(date: Date | null) => field.field.onChange(date)}
                  />
                ) : (
                  <OppdrasSammenLes>
                    <Label>Fra og med</Label>
                    <BodyShort>{format(field.field.value, 'MMMM yyyy')}</BodyShort>
                  </OppdrasSammenLes>
                )
              }
              name={`soeskengrunnlag.${index}.fom`}
              control={control}
            />
          </div>
          <div>
            <Controller
              render={(field) =>
                expanded ? (
                  <MaanedvelgerMedUtnulling>
                    <MaanedVelger
                      onChange={(val) => field.field.onChange(val)}
                      label="Til og med"
                      placeholder="Ingen slutt"
                      value={field.field.value}
                    />
                    {field.field.value !== null && field.field.value !== undefined ? (
                      <FjernKnapp onClick={() => field.field.onChange(null)}>Fjern sluttdato</FjernKnapp>
                    ) : null}
                  </MaanedvelgerMedUtnulling>
                ) : (
                  <OppdrasSammenLes>
                    <Label>Til og med</Label>
                    <BodyShort>{formaterMaanedDato('Ingen slutt', field.field.value)}</BodyShort>
                  </OppdrasSammenLes>
                )
              }
              name={`soeskengrunnlag.${index}.tom`}
              control={control}
            />
          </div>
          <VertikalMidtstiltBodyShort>
            {antallSoeskenMed} i beregning, {antallSoeskenIkkeMed} ikke i beregning{' '}
            {antallSoeskenIkkeValgt ? <span>({antallSoeskenIkkeValgt} ikke valgt)</span> : null}
          </VertikalMidtstiltBodyShort>
          {canRemove ? <FjernKnapp onClick={remove}>Slett</FjernKnapp> : null}
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
                  name={`soeskengrunnlag.${index}.data.${k}`}
                  control={control}
                  render={(field) => (
                    <RadioGroupRow
                      legend="Oppdras sammen"
                      value={field.field.value?.skalBrukes ?? null}
                      onChange={(value) => {
                        field.field.onChange({
                          foedselsnummer: item.foedselsnummer,
                          skalBrukes: value,
                        })
                      }}
                    >
                      <Radio value={true}>Ja</Radio>
                      <Radio value={false}>Nei</Radio>
                    </RadioGroupRow>
                  )}
                />
              </SoeskenContainer>
            </li>
          )
        })}
      </UstiletListe>
    </PeriodeAccordion>
  )
}

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
