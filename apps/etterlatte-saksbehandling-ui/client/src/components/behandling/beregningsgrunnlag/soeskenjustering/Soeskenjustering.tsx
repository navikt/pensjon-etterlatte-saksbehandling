import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useState } from 'react'
import { IBehandlingReducer, oppdaterBeregingsGrunnlag } from '~store/reducers/BehandlingReducer'
import { useFieldArray, useForm } from 'react-hook-form'
import { Button, ErrorSummary, Heading } from '@navikt/ds-react'
import styled from 'styled-components'
import { IPdlPerson } from '~shared/types/Person'
import { addMonths } from 'date-fns'
import { SoeskenMedIBeregning } from '~shared/types/Beregning'
import { hentBeregningsGrunnlag } from '~shared/api/beregning'
import { Barn } from '~components/behandling/soeknadsoversikt/familieforhold/personer/Barn'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ContentHeader } from '~shared/styled'
import { FamilieforholdWrapper } from '~components/behandling/soeknadsoversikt/familieforhold/barnepensjon/FamilieforholdBarnepensjon'
import {
  mapListeFraDto,
  PeriodisertBeregningsgrunnlag,
  feilIKomplettePerioderOverIntervall,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { useAppDispatch } from '~store/Store'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { SuccessColored } from '@navikt/ds-icons'
import SoeskenjusteringPeriode from '~components/behandling/beregningsgrunnlag/soeskenjustering/SoeskenjusteringPeriode'

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
  const [visOkLagret, setVisOkLagret] = useState(false)

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
  const feil: [number, FeilIPeriodeGrunnlagAlle][] = [
    ...feilIKomplettePerioderOverIntervall(allePerioder, new Date(behandling.virkningstidspunkt!.dato)),
    ...allePerioder.flatMap((periode, indeks) =>
      feilISoeskenjusteringsperiode(periode).map((feil) => [indeks, feil] as [number, FeilIPeriodeGrunnlagAlle])
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

  const ferdigstillForm = (data: { soeskenMedIBeregning: SoeskengrunnlagUtfylling }) => {
    if (validerSoeskenjustering(data.soeskenMedIBeregning) && feil.length === 0) {
      setVisFeil(false)
      onSubmit(data.soeskenMedIBeregning)
      setVisOkLagret(true)
      setTimeout(() => {
        setVisOkLagret(false)
      }, 1000)
    } else {
      setVisFeil(true)
      setVisOkLagret(false)
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
      <form id="formsoeskenjustering">
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
                type="button"
                onClick={() => append(nySoeskengrunnlagPeriode(soesken, addMonths(sisteTom || sisteFom, 1).toString()))}
              >
                Legg til periode
              </NyPeriodeButton>
            ) : null}
            {behandles && (
              <Button type="submit" onClick={handleSubmit(ferdigstillForm)}>
                Lagre søskenjustering
              </Button>
            )}
            {visOkLagret && <SuccessColored fontSize={20} />}
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

export function feilISoeskenjusteringsperiode(
  grunnlag: PeriodisertBeregningsgrunnlag<SoeskenKanskjeMedIBeregning[]>
): FeilIPeriodeGrunnlagAlle[] {
  const feil: FeilIPeriodeGrunnlagAlle[] = []
  const alleErValgt = grunnlag.data.every((person) => person.skalBrukes !== undefined && person.skalBrukes !== null)
  if (!alleErValgt) {
    feil.push('IKKE_ALLE_VALGT')
  }
  if (grunnlag.tom !== undefined && grunnlag.tom < grunnlag.fom) {
    feil.push('TOM_FOER_FOM')
  }
  return feil
}

const FeilIPerioder = (props: { feil: [number, FeilIPeriodeGrunnlagAlle][] }) => {
  return (
    <FeilIPerioderOppsummering heading="Du må fikse feil i periodiseringen før du kan beregne">
      {props.feil.map(([index, feil]) => (
        <ErrorSummary.Item key={`${index}${feil}`} href={`#soeskenjustering̋.${index}`}>
          {teksterFeilIPeriode[feil]}
        </ErrorSummary.Item>
      ))}
    </FeilIPerioderOppsummering>
  )
}

const FeilIPerioderOppsummering = styled(ErrorSummary)`
  margin: 2em auto;
  width: 30em;
`

export const FEIL_I_PERIODE = [
  'TOM_FOER_FOM',
  'PERIODE_OVERLAPPER_MED_NESTE',
  'HULL_ETTER_PERIODE',
  'INGEN_PERIODER',
  'DEKKER_IKKE_START_AV_INTERVALL',
  'DEKKER_IKKE_SLUTT_AV_INTERVALL',
] as const
export type FeilIPeriodeSoeskenjustering = (typeof FEIL_I_PERIODE)[number]
export type FeilIPeriodeGrunnlagAlle = FeilIPeriodeSoeskenjustering | 'IKKE_ALLE_VALGT'

export const teksterFeilIPeriode: Record<FeilIPeriodeGrunnlagAlle, string> = {
  INGEN_PERIODER: 'Minst en søskenjusteringsperiode må finnes',
  DEKKER_IKKE_SLUTT_AV_INTERVALL: 'Periodene må være komplette tilbake til virk',
  DEKKER_IKKE_START_AV_INTERVALL: 'Periodene må vare ut ytelsen',
  HULL_ETTER_PERIODE: 'Det er et hull i periodene etter denne perioden',
  PERIODE_OVERLAPPER_MED_NESTE: 'Perioden overlapper med neste periode',
  TOM_FOER_FOM: 'Til og med kan ikke være før fra og med',
  IKKE_ALLE_VALGT: 'Alle søsken må fylles ut',
} as const

export const UstiletListe = styled.ul`
  list-style-type: none;
`

export default Soeskenjustering
