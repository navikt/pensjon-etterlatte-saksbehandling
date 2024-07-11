import {
  Beregning,
  OverstyrBeregingsperiodeGrunnlagData,
  OverstyrBeregning,
  OverstyrBeregningsperiode,
  OverstyrtAarsak,
} from '~shared/types/Beregning'
import { Alert, BodyLong, Box, Button, ErrorSummary, HStack, List, Modal, Table, VStack } from '@navikt/ds-react'
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
import React, { Dispatch, SetStateAction, useEffect, useRef, useState } from 'react'
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
  deaktiverOverstyrtBeregning,
} from '~shared/api/beregning'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { NesteOgTilbake } from '../handlinger/NesteOgTilbake'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

import { isPending, mapApiResult } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { validateFnrObligatorisk } from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { OverstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'

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
  setOverstyrt: Dispatch<SetStateAction<OverstyrBeregning | undefined>>
}) => {
  const { behandling, setOverstyrt } = props
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
  const [deaktiverOverstyrtBeregningResultat, deaktiverOverstyrtBereging] = useApiCall(deaktiverOverstyrtBeregning)

  const [overstyrBeregningGrunnlag, fetchOverstyrBeregningGrunnlag] = useApiCall(hentOverstyrBeregningGrunnlag)
  const [persistOverstyrBeregningGrunnlag, saveOverstyrBeregningGrunnlag] = useApiCall(lagreOverstyrBeregningGrunnlag)
  const [endreBeregning, postOpprettEllerEndreBeregning] = useApiCall(opprettEllerEndreBeregning)
  const { next } = useBehandlingRoutes()

  const dispatch = useAppDispatch()
  const modalRef = useRef<HTMLDialogElement>(null)

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

  const leggTilBeregningsperiode = () => {
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
  }

  return (
    <>
      <Box paddingInline="16" paddingBlock="0 4" maxWidth="42.5em">
        <Alert variant="warning">
          <VStack gap="4">
            Denne saken har overstyrt beregning. Sjekk om du kan skru av overstyrt beregning. Husk at saken da må
            revurderes fra første virkningstidspunkt / konverteringstidspunkt.
            <List as="ul" size="small" title="Saker som fortsatt trenger overstyrt beregning er:">
              {Object.entries(OverstyrtBeregningKategori).map(([key, value]) => (
                <List.Item key={key}>{value}</List.Item>
              ))}
            </List>
          </VStack>
        </Alert>
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
                <VStack gap="4" align="start">
                  <Button
                    type="button"
                    icon={<PlusCircleIcon title="legg til" />}
                    iconPosition="left"
                    variant="tertiary"
                    onClick={() => {
                      leggTilBeregningsperiode()
                    }}
                  >
                    Legg til beregningsperiode
                  </Button>
                  <HStack gap="4" align="center">
                    <Button size="small" variant="tertiary" onClick={() => modalRef.current?.showModal()}>
                      Skru av overstyrt beregning
                    </Button>
                    <Modal
                      ref={modalRef}
                      header={{ heading: 'Er du sikker på at du vil skru av overstyrt beregning?' }}
                    >
                      <Modal.Body>
                        <BodyLong>
                          Beregningsperioder vil bli permanent slettet. Virkningstidspunkt for revurdering MÅ settes
                          tilbake til sakens første virkningstidspunkt.
                        </BodyLong>
                      </Modal.Body>
                      <Modal.Footer>
                        <Button
                          type="button"
                          variant="danger"
                          loading={isPending(deaktiverOverstyrtBeregningResultat)}
                          onClick={() => deaktiverOverstyrtBereging(behandling.id, () => setOverstyrt(undefined))}
                        >
                          Skru av overstyrt beregning
                        </Button>
                        <Button type="button" variant="secondary" onClick={() => modalRef.current?.close()}>
                          {' '}
                          Avbryt{' '}
                        </Button>
                      </Modal.Footer>
                    </Modal>

                    <Button
                      type="submit"
                      variant="secondary"
                      size="small"
                      onClick={handleSubmit(ferdigstillForm)}
                      loading={isPending(persistOverstyrBeregningGrunnlag)}
                    >
                      Lagre
                    </Button>
                  </HStack>
                </VStack>
              )}
              {visOkLagret && <CheckmarkCircleIcon color={AGreen500} />}
            </FormWrapper>

            {isFailureHandler({
              errorMessage: 'En feil har oppstått',
              apiResult: deaktiverOverstyrtBeregningResultat,
            })}
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

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
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
      </Box>
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
    <Box paddingInline="16" paddingBlock="0 4" maxWidth="42.5em">
      <ErrorSummary heading="Du må fikse feil i periodiseringen før du kan beregne">
        {props.feil.map(([index, feil]) => (
          <ErrorSummary.Item key={`${index}${feil}`} href={`#overstyrBeregningForm.${index}`}>
            {teksterFeilIPeriode[feil]}
          </ErrorSummary.Item>
        ))}
      </ErrorSummary>
    </Box>
  )
}

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
