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

export interface PeriodisertBeregningsgrunnlag<G> {
  fom: Date
  tom?: Date
  data: G
}

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

const nyttSoeskengrunnlagMedPerioderFraFamilieforhold = (soesken: IPdlPerson[], fom?: string) => [
  {
    fom: fom !== undefined ? new Date(fom) : new Date(),
    harEnSlutt: false,
    data: soesken.map((barn) => ({
      foedselsnummer: barn.foedselsnummer,
      skalBrukes: undefined,
    })),
  },
]

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
        reset({
          soeskengrunnlag:
            result?.soeskenMedIBeregning ??
            nyttSoeskengrunnlagMedPerioderFraFamilieforhold(soesken, behandling.virkningstidspunkt?.dato),
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
      <form name="form" onSubmit={handleSubmit(submitForm)}>
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
              onClick={() =>
                append(
                  nyttSoeskengrunnlagMedPerioderFraFamilieforhold(
                    soesken,
                    addMonths(sisteTom || new Date(), 1).toString()
                  )[0]
                )
              }
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

const SoeskenjusteringPeriode = (props: SoeskenjusteringPeriodeProps) => {
  const { control, index, remove, fnrTilSoesken, canRemove, behandling, watch } = props
  const { fields } = useFieldArray({
    name: `soeskengrunnlag.${index}.data`,
    control,
  })

  const soeskenMedForPeriode = watch(`soeskengrunnlag.${index}.data`)
  const antallSoeskenMed = soeskenMedForPeriode.filter((soesken) => soesken.skalBrukes === true).length
  const antallSoeskenIkkeMed = soeskenMedForPeriode.filter((soesken) => soesken.skalBrukes === false).length
  const antallSoeskenIkkeValgt = soeskenMedForPeriode.filter(
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
                  <div style={{ display: 'flex', justifyContent: 'flex-start', flexDirection: 'row', gap: '1em' }}>
                    <MaanedVelger
                      onChange={(val) => field.field.onChange(val)}
                      label="Til og med"
                      placeholder="Ingen slutt"
                      isNullable
                      value={field.field.value}
                    />
                    {field.field.value !== null && field.field.value !== undefined ? (
                      <Button
                        style={{ height: 'fit-content', width: 'fit-content', margin: 'auto 0' }}
                        size="xsmall"
                        onClick={() => field.field.onChange(null)}
                        variant="secondary"
                      >
                        Fjern sluttdato
                      </Button>
                    ) : null}
                  </div>
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
          <BodyShort style={{ margin: 'auto 0' }}>
            {antallSoeskenMed} i beregning, {antallSoeskenIkkeMed} ikke i beregning{' '}
            {antallSoeskenIkkeValgt ? <span>({antallSoeskenIkkeValgt} ikke valgt)</span> : null}
          </BodyShort>
          {canRemove ? (
            <Button
              style={{ height: 'fit-content', margin: 'auto 0' }}
              size="xsmall"
              variant="secondary"
              onClick={remove}
            >
              Slett
            </Button>
          ) : null}
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
