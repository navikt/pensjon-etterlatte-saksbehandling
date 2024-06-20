import {
  Beregning,
  OverstyrBeregingsperiodeGrunnlagData,
  OverstyrBeregning,
  OverstyrBeregningsperiode,
  OverstyrtAarsak,
} from '~shared/types/Beregning'
import { Alert, BodyShort, Box, Button, ErrorSummary, HStack, Table, VStack } from '@navikt/ds-react'
import styled from 'styled-components'
import { behandlingErRedigerbar } from '../felles/utils'
import { useFieldArray, useForm } from 'react-hook-form'
import {
  FEIL_I_PERIODE,
  feilIKomplettePerioderOverIntervall,
  mapListeFraDto,
  mapListeTilDto,
  PeriodisertBeregningsgrunnlag,
} from './PeriodisertBeregningsgrunnlag'
import OverstyrBeregningTableWrapper from './OverstyrBeregningTableWrapper'
import { Dispatch, SetStateAction, useEffect, useState } from 'react'
import { CheckmarkCircleIcon, PlusCircleIcon } from '@navikt/aksel-icons'
import {
  IBehandlingReducer,
  oppdaterBehandlingsstatus,
  oppdaterBeregning,
  oppdaterOverstyrBeregningsGrunnlag,
} from '~store/reducers/BehandlingReducer'
import { addMonths } from 'date-fns'
import { AGreen500 } from '@navikt/ds-tokens/dist/tokens'
import {
  hentOverstyrBeregningGrunnlag,
  lagreOverstyrBeregningGrunnlag,
  opprettEllerEndreBeregning,
  slettOverstyrtBeregning,
} from '~shared/api/beregning'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'

import { isPending, mapApiResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { validateFnrObligatorisk } from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { getValueOfKey } from '~shared/types/OverstyrtBeregning'

const stripWhitespace = (s: string | number): string => {
  if (typeof s === 'string') return s.replace(/\s+/g, '')
  else return s.toString().replace(/\s+/g, '')
}

function fjernWhitespaceFraUtbetaltBeloep(
  data: OverstyrBeregingsperiodeGrunnlagData
): OverstyrBeregingsperiodeGrunnlagData {
  return data.map(({ fom, tom, data }) => ({
    fom,
    tom,
    data: {
      ...data,
      utbetaltBeloep: stripWhitespace(data.utbetaltBeloep),
    },
  }))
}

const OverstyrBeregningGrunnlag = (props: {
  behandling: IBehandlingReducer
  overstyrBeregning: OverstyrBeregning
  setOverstyrt: Dispatch<SetStateAction<OverstyrBeregning | undefined>>
}) => {
  const { behandling, overstyrBeregning, setOverstyrt } = props
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const behandles = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const [visFeil, setVisFeil] = useState(false)
  const [visOkLagret, setVisOkLagret] = useState(false)
  const perioder = useAppSelector((state) => state.behandlingReducer.behandling?.overstyrBeregning?.perioder)
  const { control, register, watch, handleSubmit, setValue } = useForm<{
    overstyrBeregningForm: OverstyrBeregingsperiodeGrunnlagData
  }>({
    defaultValues: {
      overstyrBeregningForm: mapListeFraDto(perioder ?? []),
    },
  })
  const [slettResultat, slettOverstyrtBereging] = useApiCall(slettOverstyrtBeregning)

  const [overstyrBeregningGrunnlag, fetchOverstyrBeregningGrunnlag] = useApiCall(hentOverstyrBeregningGrunnlag)
  const [persistOverstyrBeregningGrunnlag, saveOverstyrBeregningGrunnlag] = useApiCall(lagreOverstyrBeregningGrunnlag)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const { next } = useBehandlingRoutes()

  const dispatch = useAppDispatch()

  useEffect(() => {
    fetchOverstyrBeregningGrunnlag(behandling.id, (result) => {
      dispatch(oppdaterOverstyrBeregningsGrunnlag(result))

      setValue('overstyrBeregningForm', mapListeFraDto(result.perioder ?? []))
    })
  }, [])

  const { fields, append, remove } = useFieldArray({
    name: 'overstyrBeregningForm',
    control,
  })

  const sisteTom = watch(`overstyrBeregningForm.${fields.length - 1}.tom`)
  const sisteFom = watch(`overstyrBeregningForm.${fields.length - 1}.fom`)
  const allePerioder = watch('overstyrBeregningForm')

  const feil: [number, FeilIPeriodeGrunnlagAlle][] = [
    ...feilIKomplettePerioderOverIntervall(allePerioder, new Date(behandling.virkningstidspunkt!.dato)),
    ...allePerioder.flatMap((periode, indeks) =>
      feilIOverstyrBeregningperiode(periode).map((feil) => [indeks, feil] as [number, FeilIPeriodeGrunnlagAlle])
    ),
  ]

  const validerOverstyrBeregning = (grunnlag: OverstyrBeregingsperiodeGrunnlagData) => {
    return grunnlag.every((value) => feilIOverstyrBeregningperiode(value).length === 0)
  }

  const validerOgLagre = (overstyrtBeregningForm: OverstyrBeregingsperiodeGrunnlagData) => {
    const fiksetBeloep = fjernWhitespaceFraUtbetaltBeloep(overstyrtBeregningForm)
    if (validerOverstyrBeregning(fiksetBeloep) && feil.length === 0) {
      setVisFeil(false)
      saveOverstyrBeregningGrunnlag(
        {
          behandlingId: behandling.id,
          grunnlag: {
            perioder: mapListeTilDto(fiksetBeloep),
          },
        },
        (result) => {
          dispatch(oppdaterOverstyrBeregningsGrunnlag(result))
        }
      )
      setVisOkLagret(true)
      setTimeout(() => {
        setVisOkLagret(false)
      }, 1000)

      return true
    } else {
      setVisFeil(true)
      setVisOkLagret(false)
      return false
    }
  }

  const onSubmit = () => {
    if (validerOgLagre(allePerioder)) {
      postOpprettEllerEndreBeregning(behandling.id, (beregning: Beregning) => {
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.BEREGNET))
        dispatch(oppdaterBeregning(beregning))
        next()
      })
    }
  }

  const ferdigstillForm = (data: { overstyrBeregningForm: OverstyrBeregingsperiodeGrunnlagData }) => {
    validerOgLagre(data.overstyrBeregningForm)
  }

  const nesteFomDato = (
    fom: Date | undefined = new Date(behandling.virkningstidspunkt!.dato),
    tom: Date | undefined
  ): Date => {
    return tom ? addMonths(tom, 1) : fom
  }

  return (
    <>
      <Box paddingInline="16" paddingBlock="0 4">
        <VStack gap="5">
          <Alert variant="warning">Denne beregningen er manuelt overstyrt</Alert>
          <BodyShort textColor="default">
            <b>Årsak:</b> {getValueOfKey(overstyrBeregning.kategori)}
          </BodyShort>
          <BodyShort textColor="default">
            <b>Beskrivelse: </b> {overstyrBeregning.beskrivelse}
          </BodyShort>
        </VStack>
      </Box>

      {mapApiResult(
        overstyrBeregningGrunnlag,
        <Spinner visible={true} label="Henter grunnlag" />,
        () => (
          <ApiErrorAlert>En feil har oppstått ved henting av grunnlag</ApiErrorAlert>
        ),
        () => (
          <>
            {visFeil && feil.length > 0 && behandles ? <FeilIPerioder feil={feil} /> : null}
            <FormWrapper>
              {fields.length ? (
                <Table>
                  <Table.Header>
                    <Table.Row>
                      <Table.HeaderCell />
                      <Table.HeaderCell scope="col">Periode</Table.HeaderCell>
                      <Table.HeaderCell scope="col">Utbetalt beløp</Table.HeaderCell>
                      <Table.HeaderCell scope="col">Trygdetid</Table.HeaderCell>
                      <Table.HeaderCell scope="col">Tilhører FNR</Table.HeaderCell>
                      <Table.HeaderCell scope="col">Prorata</Table.HeaderCell>
                      <Table.HeaderCell scope="col">Årsak</Table.HeaderCell>
                      <Table.HeaderCell scope="col">Beskrivelse</Table.HeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body id="formoverstyrberegning">
                    {fields.map((item, index) => (
                      <OverstyrBeregningTableWrapper
                        key={item.id}
                        item={item}
                        index={index}
                        control={control}
                        register={register}
                        remove={remove}
                        watch={watch}
                        visFeil={visFeil}
                        feil={feil}
                        behandles={behandles}
                        aarsaker={OverstyrtAarsak}
                      />
                    ))}
                  </Table.Body>
                </Table>
              ) : null}
              {behandles && (
                <Button
                  type="button"
                  icon={<PlusCircleIcon title="legg til" />}
                  iconPosition="left"
                  variant="tertiary"
                  onClick={() => {
                    setVisFeil(false)
                    append([
                      {
                        fom: nesteFomDato(sisteFom, sisteTom),
                        tom: undefined,
                        data: {
                          utbetaltBeloep: '0',
                          trygdetid: '0',
                          trygdetidForIdent: '',
                          prorataBroekNevner: '',
                          prorataBroekTeller: '',
                          beskrivelse: '',
                          aarsak: 'VELG_AARSAK',
                        },
                      },
                    ])
                  }}
                >
                  Legg til beregningsperiode
                </Button>
              )}
              {behandles && (
                <HStack gap="4" align="center">
                  <Button
                    type="submit"
                    size="small"
                    onClick={handleSubmit(ferdigstillForm)}
                    loading={isPending(persistOverstyrBeregningGrunnlag)}
                  >
                    Lagre
                  </Button>
                  {behandling.behandlingType == IBehandlingsType.FØRSTEGANGSBEHANDLING && (
                    <Button
                      variant="tertiary"
                      loading={isPending(slettResultat)}
                      onClick={() => {
                        slettOverstyrtBereging(behandling.id, () => setOverstyrt(undefined))
                      }}
                    >
                      Skru av overstyrt beregning
                    </Button>
                  )}
                </HStack>
              )}
              {visOkLagret && <CheckmarkCircleIcon color={AGreen500} />}
            </FormWrapper>
          </>
        )
      )}
      {isPending(persistOverstyrBeregningGrunnlag) && <Spinner visible={true} label="Lagre grunnlag" />}
      {isFailureHandler({
        errorMessage: 'En feil har oppstått ved lagring av grunnlag',
        apiResult: persistOverstyrBeregningGrunnlag,
      })}
      {isFailureHandler({
        errorMessage: 'Kunne ikke opprette ny beregning',
        apiResult: endreBeregning,
      })}
      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button
            variant="primary"
            onClick={onSubmit}
            loading={isPending(persistOverstyrBeregningGrunnlag || isPending(endreBeregning))}
          >
            Beregn
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </>
  )
}

function feilIOverstyrBeregningperiode(
  grunnlag: PeriodisertBeregningsgrunnlag<OverstyrBeregningsperiode>
): FeilIPeriodeGrunnlagAlle[] {
  const feil: FeilIPeriodeGrunnlagAlle[] = []

  const beloep = parseInt(grunnlag.data.utbetaltBeloep)
  if (isNaN(beloep) || beloep < 0) {
    feil.push('BELOEP_MANGLER')
  }

  const trygdetid = parseInt(grunnlag.data.trygdetid)
  if (isNaN(trygdetid) || trygdetid <= 0) {
    feil.push('TRYGDETID_MANGLER')
  }

  if (validateFnrObligatorisk(grunnlag.data.trygdetidForIdent)) {
    feil.push('TRYGDETID_MANGLER_FNR')
  }

  const prorataBroekNevner = parseInt(grunnlag.data.prorataBroekNevner ?? '')
  const prorataBroekTeller = parseInt(grunnlag.data.prorataBroekTeller ?? '')

  if (!isNaN(prorataBroekNevner) || !isNaN(prorataBroekTeller)) {
    if (isNaN(prorataBroekNevner) || isNaN(prorataBroekTeller) || prorataBroekNevner <= 0 || prorataBroekTeller <= 0) {
      feil.push('PRORATA_MANGLER')
    }
  }

  if (grunnlag.tom !== undefined && grunnlag.tom < grunnlag.fom) {
    feil.push('TOM_FOER_FOM')
  }

  if (!grunnlag.data.beskrivelse) {
    feil.push('BESKRIVELSE_MANGLER')
  }

  return feil
}

const FeilIPerioder = (props: { feil: [number, FeilIPeriodeGrunnlagAlle][] }) => {
  return (
    <FeilIPerioderOppsummering heading="Du må fikse feil i periodiseringen før du kan beregne">
      {props.feil.map(([index, feil]) => (
        <ErrorSummary.Item key={`${index}${feil}`} href={`#overstyrBeregningForm.${index}`}>
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

type FeilIPeriodeOverstyrBeregning = (typeof FEIL_I_PERIODE)[number]
export type FeilIPeriodeGrunnlagAlle =
  | FeilIPeriodeOverstyrBeregning
  | 'BELOEP_MANGLER'
  | 'TRYGDETID_MANGLER'
  | 'TRYGDETID_MANGLER_FNR'
  | 'BESKRIVELSE_MANGLER'
  | 'PRORATA_MANGLER'

export const teksterFeilIPeriode: Record<FeilIPeriodeGrunnlagAlle, string> = {
  INGEN_PERIODER: 'Minst en periode må finnes',
  DEKKER_IKKE_SLUTT_AV_INTERVALL: 'Periodene må være komplette tilbake til virk',
  DEKKER_IKKE_START_AV_INTERVALL: 'Periodene må vare ut ytelsen',
  HULL_ETTER_PERIODE: 'Det er et hull i periodene etter denne perioden',
  PERIODE_OVERLAPPER_MED_NESTE: 'Perioden overlapper med neste periode',
  TOM_FOER_FOM: 'Til og med kan ikke være før fra og med',
  BELOEP_MANGLER: 'Utbetalt beløp er påkrevd',
  TRYGDETID_MANGLER: 'Trygdetid er påkrevd',
  TRYGDETID_MANGLER_FNR: 'Trygdetid tilhører FNR er påkrevd',
  BESKRIVELSE_MANGLER: 'Beskrivelse er påkrevd',
  PRORATA_MANGLER: 'Prorata brøk må ha begge felter fyllt ut hvis det er i bruk',
} as const

export default OverstyrBeregningGrunnlag

const FormWrapper = styled.div`
  padding: 1em 4em;
  max-width: 70em;
  margin-bottom: 1rem;
`
