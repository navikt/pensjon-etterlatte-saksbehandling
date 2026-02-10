import React from 'react'
import {
  BodyShort,
  Box,
  Button,
  Checkbox,
  CheckboxGroup,
  HGrid,
  HStack,
  Label,
  Select,
  Textarea,
  TextField,
  VStack,
} from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { OverstyrBeregningsperiode, OverstyrtAarsak } from '~shared/types/Beregning'
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
import { oppdaterBehandlingsstatus, oppdaterOverstyrBeregningsGrunnlag } from '~store/reducers/BehandlingReducer'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { IBehandlingStatus, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  initialOverstyrBeregningsgrunnlagPeriode,
  konverterTilSisteDagIMaaneden,
  replacePeriodePaaIndex,
  stripWhitespace,
  validerAarsak,
} from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/utils'
import { JaNei } from '~shared/types/ISvar'
import { formaterTilISOString } from '~utils/formatering/dato'
import { SakType } from '~shared/types/sak'

interface Props {
  behandling: IDetaljertBehandling
  eksisterendePeriode?: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>
  indexTilEksisterendePeriode?: number
  paaAvbryt: () => void
  paaLagre: () => void
}

export const OverstyrBeregningsgrunnlagPeriodeSkjema = ({
  behandling,
  eksisterendePeriode,
  indexTilEksisterendePeriode,
  paaAvbryt,
  paaLagre,
}: Props) => {
  const overstyrBeregningGrunnlagPerioder = useAppSelector(
    (state) => state.behandlingReducer.behandling?.overstyrBeregning?.perioder
  )

  const dispatch = useAppDispatch()

  const [lagreOverstyrBeregningGrunnlagResult, lagreOverstyrBeregningGrunnlagRequest] =
    useApiCall(lagreOverstyrBeregningGrunnlag)

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>>({
    defaultValues: eksisterendePeriode
      ? eksisterendePeriode
      : initialOverstyrBeregningsgrunnlagPeriode(
          behandling,
          overstyrBeregningGrunnlagPerioder?.[overstyrBeregningGrunnlagPerioder.length - 1]
        ),
  })
  const lagrePeriode = (
    overtyrBeregningsgrunnlagPeriode: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode>
  ) => {
    const periodeMedBeloepUtenWhitespace: PeriodisertBeregningsgrunnlagDto<OverstyrBeregningsperiode> = {
      ...overtyrBeregningsgrunnlagPeriode,
      tom: overtyrBeregningsgrunnlagPeriode.tom && konverterTilSisteDagIMaaneden(overtyrBeregningsgrunnlagPeriode.tom),
      data: {
        ...overtyrBeregningsgrunnlagPeriode.data,
        utbetaltBeloep: stripWhitespace(overtyrBeregningsgrunnlagPeriode.data.utbetaltBeloep),
      },
    }

    if (eksisterendePeriode && overstyrBeregningGrunnlagPerioder && indexTilEksisterendePeriode !== undefined) {
      lagreOverstyrBeregningGrunnlagRequest(
        {
          behandlingId: behandling.id,
          grunnlag: {
            perioder: replacePeriodePaaIndex(
              periodeMedBeloepUtenWhitespace,
              overstyrBeregningGrunnlagPerioder,
              indexTilEksisterendePeriode
            ),
          },
        },
        (result) => {
          dispatch(oppdaterOverstyrBeregningsGrunnlag(result))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
          paaLagre()
        }
      )
    } else {
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
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
          paaLagre()
        }
      )
    }
  }

  return (
    <form onSubmit={handleSubmit(lagrePeriode)}>
      <VStack gap="space-4">
        <HGrid gap="space-4" columns="min-content min-content max-content" align="start">
          <ControlledMaanedVelger name="fom" label="Fra og med" control={control} required />
          <ControlledMaanedVelger
            name="tom"
            label="Til og med"
            control={control}
            validate={(maaned) => {
              if (behandling.viderefoertOpphoer?.skalViderefoere == JaNei.JA) {
                if (
                  !maaned ||
                  formaterTilISOString(maaned) >= formaterTilISOString(behandling.viderefoertOpphoer?.dato)
                ) {
                  return 'Til og med-dato må være før opphør fra og med'
                }
              }
              return undefined
            }}
          />
          <Box width="fit-content">
            <HStack gap="space-2" align="center">
              <TextField
                {...register('data.utbetaltBeloep', {
                  valueAsNumber: true,
                  required: { value: true, message: 'Må settes' },
                  validate: validerStringNumber,
                })}
                label="Utbetalt beløp"
                error={errors.data?.utbetaltBeloep?.message}
              />
              {behandling.sakType == SakType.BARNEPENSJON && (
                <Box width="fit-content">
                  <CheckboxGroup legend="Foreldreløssats">
                    <Checkbox {...register('data.foreldreloessats')} value="">
                      Har foreldreløssats
                    </Checkbox>
                  </CheckboxGroup>
                </Box>
              )}
            </HStack>
          </Box>

          <TextField
            {...register('data.trygdetid', {
              valueAsNumber: true,
              required: { value: true, message: 'Må settes' },
              min: { value: 0, message: 'Kan ikke være negativ' },
              max: { value: 40, message: 'Kan ikke være mer enn 40 år' },
              validate: validerStringNumber,
            })}
            label="Anvendt trygdetid (år)"
            error={errors.data?.trygdetid?.message}
          />
          <TextField
            {...register('data.trygdetidForIdent', { validate: validateFnrObligatorisk })}
            label="Tilhørende fnr"
            error={errors.data?.trygdetidForIdent?.message}
          />
          <div>
            <Label>Prorata brøk (valgfritt)</Label>
            <HStack gap="space-2" align="center">
              <TextField
                {...register('data.prorataBroekTeller')}
                label=""
                error={errors.data?.prorataBroekTeller?.message}
              />
              <BodyShort>/</BodyShort>
              <TextField
                {...register('data.prorataBroekNevner')}
                label=""
                error={errors.data?.prorataBroekNevner?.message}
              />
            </HStack>
          </div>
        </HGrid>
        <VStack gap="space-4" maxWidth="17rem">
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

        <HStack gap="space-4">
          <Button
            size="small"
            icon={<FloppydiskIcon aria-hidden />}
            loading={isPending(lagreOverstyrBeregningGrunnlagResult)}
          >
            Lagre
          </Button>
          <Button variant="secondary" type="button" size="small" icon={<XMarkIcon aria-hidden />} onClick={paaAvbryt}>
            Avbryt
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
