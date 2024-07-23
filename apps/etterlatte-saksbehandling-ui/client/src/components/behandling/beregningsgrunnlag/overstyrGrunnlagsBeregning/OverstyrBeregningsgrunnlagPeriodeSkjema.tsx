import React, { Dispatch, SetStateAction } from 'react'
import { BodyShort, Box, Button, HGrid, HStack, Label, Select, Textarea, TextField, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { OverstyrBeregningsperiode, OverstyrtAarsak, OverstyrtAarsakKey } from '~shared/types/Beregning'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreOverstyrBeregningGrunnlag } from '~shared/api/beregning'
import { useAppDispatch, useAppSelector } from '~store/Store'
import {
  validateFnrObligatorisk,
  validerStringNumber,
} from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { isPending } from '~shared/api/apiUtils'
import { oppdaterOverstyrBeregningsGrunnlag } from '~store/reducers/BehandlingReducer'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { addMonths } from 'date-fns'
import { formaterTilISOString } from '~utils/formatering/dato'

const stripWhitespace = (s: string | number): string => {
  if (typeof s === 'string') return s.replace(/\s+/g, '')
  else return s.toString().replace(/\s+/g, '')
}

const initialPeriode = (
  behandling: IDetaljertBehandling,
  sistePeriode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode> | undefined
): PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode> => {
  const nesteFomDato = (
    fom: Date | undefined = new Date(behandling.virkningstidspunkt!.dato),
    tom: Date | undefined
  ): Date | string => {
    return tom ? addMonths(tom, 1) : fom
  }

  return {
    fom: formaterTilISOString(
      nesteFomDato(
        sistePeriode ? new Date(sistePeriode.fom) : new Date(behandling.virkningstidspunkt!.dato),
        sistePeriode && sistePeriode.fom ? new Date(sistePeriode.fom) : undefined
      )
    ),
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
  }
}

interface Props {
  behandling: IDetaljertBehandling
  setVisOverstyrBeregningPeriodeSkjema: Dispatch<SetStateAction<boolean>>
  eksisterendePeriode?: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>
}

export const OverstyrBeregningsgrunnlagPeriodeSkjema = ({
  behandling,
  setVisOverstyrBeregningPeriodeSkjema,
  eksisterendePeriode,
}: Props) => {
  const overstyrBeregningGrunnlagPerioder = useAppSelector(
    (state) => state.behandlingReducer.behandling?.overstyrBeregning?.perioder
  )

  const dispatch = useAppDispatch()

  const [lagreOverstyrBeregningGrunnlagResult, lagreOverstyrBeregningGrunnlagRequest] =
    useApiCall(lagreOverstyrBeregningGrunnlag)

  const validerAarsak = (aarsak: OverstyrtAarsakKey | undefined): string | undefined => {
    if (!aarsak || aarsak === 'VELG_AARSAK') return 'Må settes'
    return undefined
  }

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>>({
    defaultValues: eksisterendePeriode
      ? eksisterendePeriode
      : initialPeriode(behandling, overstyrBeregningGrunnlagPerioder?.[overstyrBeregningGrunnlagPerioder.length - 1]),
  })

  const avbryt = () => {
    reset()
    setVisOverstyrBeregningPeriodeSkjema(false)
  }

  const lagrePeriode = (
    overtyrBeregningsgrunnlagPeriode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>
  ) => {
    const periodeMedBeloepUtenWhitespace: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode> = {
      ...overtyrBeregningsgrunnlagPeriode,
      fom: formaterTilISOString(overtyrBeregningsgrunnlagPeriode.fom),
      tom: overtyrBeregningsgrunnlagPeriode.tom
        ? formaterTilISOString(overtyrBeregningsgrunnlagPeriode.tom)
        : undefined,
      data: {
        ...overtyrBeregningsgrunnlagPeriode.data,
        utbetaltBeloep: stripWhitespace(overtyrBeregningsgrunnlagPeriode.data.utbetaltBeloep),
      },
    }

    lagreOverstyrBeregningGrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag: {
          perioder: !!overstyrBeregningGrunnlagPerioder?.length
            ? [...overstyrBeregningGrunnlagPerioder, periodeMedBeloepUtenWhitespace]
            : [periodeMedBeloepUtenWhitespace],
        },
      },
      (result) => {
        dispatch(oppdaterOverstyrBeregningsGrunnlag(result))
        setVisOverstyrBeregningPeriodeSkjema(false)
      }
    )
  }

  return (
    <form onSubmit={handleSubmit(lagrePeriode)}>
      <VStack gap="4">
        <HGrid gap="4" columns="min-content min-content max-content" align="start">
          <ControlledMaanedVelger name="fom" label="Fra og med" control={control} required />
          <ControlledMaanedVelger name="tom" label="Til og med" control={control} />
          <Box width="fit-content">
            <TextField
              {...register('data.utbetaltBeloep', {
                valueAsNumber: true,
                required: { value: true, message: 'Må settes' },
                min: { value: 1, message: 'Må være større en 0' },
                validate: validerStringNumber,
              })}
              label="Utbetalt beløp"
              error={errors.data?.utbetaltBeloep?.message}
            />
          </Box>
          <TextField
            {...register('data.trygdetid', {
              valueAsNumber: true,
              required: { value: true, message: 'Må settes' },
              min: { value: 0, message: 'Kan ikke være negativ' },
              validate: validerStringNumber,
            })}
            label="Anvendt trygdetid"
            error={errors.data?.trygdetid?.message}
          />
          <TextField
            {...register('data.trygdetidForIdent', { validate: validateFnrObligatorisk })}
            label="Tilhørende fnr"
            error={errors.data?.trygdetidForIdent?.message}
          />
          <div>
            <Label>Prorata brøk (valgfritt)</Label>
            <HStack gap="3" align="center">
              <TextField
                {...register('data.prorataBroekTeller', {
                  valueAsNumber: true,
                  required: { value: true, message: 'Må settes' },
                  min: { value: 0, message: 'Kan ikke være negativ' },
                  validate: validerStringNumber,
                })}
                label=""
                error={errors.data?.prorataBroekTeller?.message}
              />
              <BodyShort>/</BodyShort>
              <TextField
                {...register('data.prorataBroekNevner', {
                  valueAsNumber: true,
                  required: { value: true, message: 'Må settes' },
                  min: { value: 0, message: 'Kan ikke være negativ' },
                  validate: validerStringNumber,
                })}
                label=""
                error={errors.data?.prorataBroekNevner?.message}
              />
            </HStack>
          </div>
        </HGrid>
        <VStack gap="4" maxWidth="17rem">
          <Select
            {...register('data.aarsak', {
              validate: validerAarsak,
            })}
            label="Årsak"
            error={errors.data?.aarsak?.message}
          >
            {Object.entries(OverstyrtAarsak).map(([key, value]) => (
              <option key={key} value={key}>
                {value}
              </option>
            ))}
          </Select>
          <Textarea {...register('data.beskrivelse')} label="Beskrivelse (valgfritt)" minRows={3} autoComplete="off" />
        </VStack>

        {isFailureHandler({
          apiResult: lagreOverstyrBeregningGrunnlagResult,
          errorMessage: 'Feil i lagringen av periode',
        })}

        <HStack gap="4">
          <Button variant="secondary" type="button" size="small" icon={<XMarkIcon aria-hidden />} onClick={avbryt}>
            Avbryt
          </Button>
          <Button
            size="small"
            icon={<FloppydiskIcon aria-hidden />}
            loading={isPending(lagreOverstyrBeregningGrunnlagResult)}
          >
            Lagre
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
