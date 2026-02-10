/*
TODO: Aksel Box migration:
Could not migrate the following:
  - borderColor=border-neutral-subtle
*/

import React, { useMemo, useState } from 'react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { useFieldArray, useForm } from 'react-hook-form'
import { Alert, BodyShort, Box, Button, ErrorSummary, Heading, HStack, VStack } from '@navikt/ds-react'
import styled from 'styled-components'
import { hentLevendeSoeskenFraAvdoedeForSoeker, IPdlPerson } from '~shared/types/Person'
import { addMonths } from 'date-fns'
import { SoeskenMedIBeregning } from '~shared/types/Beregning'
import { Barn } from '~components/behandling/beregningsgrunnlag/soeskenjustering/Barn'
import {
  FEIL_I_PERIODE,
  feilIKomplettePerioderOverIntervall,
  mapListeFraDto,
  PeriodisertBeregningsgrunnlag,
  PeriodisertBeregningsgrunnlagDto,
} from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import SoeskenjusteringPeriode from '~components/behandling/beregningsgrunnlag/soeskenjustering/SoeskenjusteringPeriode'

import { CheckmarkCircleIcon } from '@navikt/aksel-icons'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

type SoeskenKanskjeMedIBeregning = {
  foedselsnummer: string
  skalBrukes?: boolean
}

export type Soeskengrunnlag = PeriodisertBeregningsgrunnlag<SoeskenMedIBeregning[]>[]

export type SoeskengrunnlagUtfylling = PeriodisertBeregningsgrunnlag<SoeskenKanskjeMedIBeregning[]>[]

type SoeskenjusteringProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: Soeskengrunnlag) => void
  setSoeskenJusteringManglerIkke: () => void
}

const nySoeskengrunnlagPeriode = (soesken: IPdlPerson[], fom?: string) => ({
  fom: fom !== undefined ? new Date(fom) : new Date(),
  data: soesken.map((barn) => ({
    foedselsnummer: barn.foedselsnummer,
    skalBrukes: undefined,
  })),
})

function manglendeSoeskenIEksisterendeSoeskenjustering(
  soesken: IPdlPerson[],
  soeskenjusteringDto?: PeriodisertBeregningsgrunnlagDto<SoeskenMedIBeregning[]>[]
): PeriodisertBeregningsgrunnlagDto<SoeskenMedIBeregning[]>[] {
  const perioderMedMangler = (soeskenjusteringDto ?? []).filter((periode) =>
    periode.data.every(
      (soeskenIBeregning) => !soesken.some((soesken) => soeskenIBeregning.foedselsnummer === soesken.foedselsnummer)
    )
  )
  return perioderMedMangler
}

const Soeskenjustering = (props: SoeskenjusteringProps) => {
  const { behandling, onSubmit, setSoeskenJusteringManglerIkke } = props
  const personopplysninger = usePersonopplysninger()
  const [visFeil, setVisFeil] = useState(false)
  const [skjulManglendeSoesken, setSkjulManglendeSoesken] = useState(false)
  if (!personopplysninger) {
    return null
  }

  const soesken = hentLevendeSoeskenFraAvdoedeForSoeker(
    personopplysninger.avdoede,
    personopplysninger.soeker?.opplysning.foedselsnummer
  )

  const manglendeSoesken = useMemo(() => {
    return manglendeSoeskenIEksisterendeSoeskenjustering(soesken, behandling.beregningsGrunnlag?.soeskenMedIBeregning)
  }, [behandling.beregningsGrunnlag?.soeskenMedIBeregning, personopplysninger.avdoede])

  const { handleSubmit, control, watch, reset } = useForm<{
    soeskenMedIBeregning: PeriodisertBeregningsgrunnlag<SoeskenKanskjeMedIBeregning[]>[]
  }>({
    defaultValues: {
      soeskenMedIBeregning: !!behandling.beregningsGrunnlag?.soeskenMedIBeregning.length
        ? mapListeFraDto(behandling.beregningsGrunnlag?.soeskenMedIBeregning)
        : [nySoeskengrunnlagPeriode(soesken, behandling.virkningstidspunkt?.dato)],
    },
  })

  const { fields, append, remove } = useFieldArray({
    name: 'soeskenMedIBeregning',
    control,
  })

  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const sisteTom = watch(`soeskenMedIBeregning.${fields.length - 1}.tom`)
  const sisteFom = watch(`soeskenMedIBeregning.${fields.length - 1}.fom`)
  const [visOkLagret, setVisOkLagret] = useState(false)
  const allePerioder = watch('soeskenMedIBeregning')
  const feil: [number, FeilIPeriodeGrunnlagAlle][] = [
    ...feilIKomplettePerioderOverIntervall(allePerioder, new Date(behandling.virkningstidspunkt!.dato)),
    ...allePerioder.flatMap((periode, indeks) =>
      feilISoeskenjusteringsperiode(periode).map((feil) => [indeks, feil] as [number, FeilIPeriodeGrunnlagAlle])
    ),
  ]

  function oppdaterSoeskenTilJustering() {
    if (manglendeSoesken.length === 0) {
      return
    }
    setSkjulManglendeSoesken(true)
    const allePerioder = mapListeFraDto(behandling.beregningsGrunnlag!!.soeskenMedIBeregning)
    const oppdatertePerioder = allePerioder.map((periode) => {
      const manglendeSoeskenForPeriode = soesken.filter((soesken) =>
        periode.data.every((sib) => sib.foedselsnummer !== soesken.foedselsnummer)
      )
      return {
        ...periode,
        data: [
          ...periode.data,
          ...manglendeSoeskenForPeriode.map(
            (soesken): SoeskenMedIBeregning => ({
              foedselsnummer: soesken.foedselsnummer,
              skalBrukes: false,
            })
          ),
        ],
      }
    })
    reset({ soeskenMedIBeregning: oppdatertePerioder })
  }

  const fnrTilSoesken: Record<string, IPdlPerson> = soesken.reduce(
    (acc, next) => ({
      ...acc,
      [next.foedselsnummer]: next,
    }),
    {} as Record<string, IPdlPerson>
  )

  const ferdigstillForm = (data: { soeskenMedIBeregning: SoeskengrunnlagUtfylling }) => {
    setSoeskenJusteringManglerIkke()
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

  return (
    <VStack gap="space-4">
      <Heading level="3" size="small">
        Søskenjustering
      </Heading>
      {manglendeSoesken.length > 0 && !skjulManglendeSoesken && (
        <div style={{ maxWidth: '40rem' }}>
          <Alert variant="warning">
            <VStack gap="space-4">
              <BodyShort>
                Den lagrede søskenjusteringen har ikke med alle avdødes barn. For å få riktig søskenjustering må de
                manglende søskene legges til periodene.
              </BodyShort>
              <BodyShort>
                <Button onClick={oppdaterSoeskenTilJustering}>Oppdater søsken</Button>
              </BodyShort>
            </VStack>
          </Alert>
        </div>
      )}
      <FamilieforholdWrapper>
        {personopplysninger.soeker && (
          <Barn
            person={personopplysninger.soeker?.opplysning}
            doedsdato={personopplysninger.soeker?.opplysning.doedsdato}
          />
        )}
      </FamilieforholdWrapper>
      <Box borderWidth="1 0 0 0" borderColor="border-neutral-subtle">
        {visFeil && feil.length > 0 && redigerbar ? <FeilIPerioder feil={feil} /> : null}
        <form id="formsoeskenjustering">
          <UstiletListe>
            {fields.map((item, index) => (
              <SoeskenjusteringPeriode
                key={item.id}
                behandling={behandling}
                familieforhold={{
                  avdoede: personopplysninger.avdoede,
                  gjenlevende: personopplysninger.gjenlevende,
                  soeker: personopplysninger.soeker,
                }}
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
          {redigerbar && (
            <HStack gap="space-4" align="center">
              <Button
                size="small"
                variant="secondary"
                type="button"
                onClick={() => append(nySoeskengrunnlagPeriode(soesken, addMonths(sisteTom || sisteFom, 1).toString()))}
              >
                Legg til periode
              </Button>

              <Button type="submit" onClick={handleSubmit(ferdigstillForm)} size="small">
                Lagre søskenjustering
              </Button>
              {visOkLagret && <CheckmarkCircleIcon color="var(--ax-success-600)" />}
            </HStack>
          )}
        </form>
      </Box>
    </VStack>
  )
}

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
        <ErrorSummary.Item key={`${index}${feil}`} href={`#soeskenjustering.${index}`}>
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

const FamilieforholdWrapper = styled.div`
  padding: 1em 4em;
  display: grid;
  gap: 4rem;
  margin-bottom: 4rem;
`

export default Soeskenjustering
