import React from 'react'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import {
  InstitusjonsoppholdGrunnlagDTO,
  InstitusjonsoppholdIBeregning,
  LagreBeregningsGrunnlagDto,
  ReduksjonBP,
  ReduksjonKey,
  ReduksjonOMS,
  toLagreBeregningsGrunnlagDto,
} from '~shared/types/Beregning'
import { Box, Button, HelpText, HStack, Select, Textarea, TextField, VStack } from '@navikt/ds-react'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { useForm } from 'react-hook-form'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'
import { validerStringNumber } from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlag } from '~shared/api/beregning'
import { SakType } from '~shared/types/sak'
import { oppdaterBehandlingsstatus, oppdaterBeregningsGrunnlag } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { isPending } from '~shared/api/apiUtils'
import {
  initalInstitusjonsoppholdPeriode,
  konverterTilSisteDagIMaaneden,
  replacePeriodePaaIndex,
} from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/utils'
import { useBehandling } from '~components/behandling/useBehandling'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

interface Props {
  sakType: SakType
  eksisterendePeriode?: PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>
  indexTilEksisterendePeriode?: number
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
  paaAvbryt: () => void
  paaLagre: () => void
}

export const InstitusjonsoppholdBeregningsgrunnlagSkjema = ({
  sakType,
  indexTilEksisterendePeriode,
  eksisterendePeriode,
  institusjonsopphold,
  paaAvbryt,
  paaLagre,
}: Props) => {
  const behandling = useBehandling()
  if (!behandling) return <ApiErrorAlert>Fant ikke behandling</ApiErrorAlert>

  const beregningsgrunnlag = behandling?.beregningsGrunnlag
  const [lagreBeregningsGrunnlagResult, lagreBeregningsGrunnlagRequest] = useApiCall(lagreBeregningsGrunnlag)

  const dispatch = useAppDispatch()

  const {
    register,
    control,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm<PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>>({
    defaultValues: eksisterendePeriode
      ? eksisterendePeriode
      : initalInstitusjonsoppholdPeriode(behandling!!, institusjonsopphold?.[institusjonsopphold?.length - 1]),
  })

  const validerReduksjon = (reduksjon: ReduksjonKey): string | undefined => {
    if (reduksjon === 'VELG_REDUKSJON') return 'Må settes'
    return undefined
  }

  const lagrePeriode = (
    institusjonsoppholdPeriode: PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>
  ) => {
    const formatertInstitusjonsoppholdPeriode = {
      ...institusjonsoppholdPeriode,
      tom: institusjonsoppholdPeriode.tom && konverterTilSisteDagIMaaneden(institusjonsoppholdPeriode.tom),
    }

    const grunnlag: LagreBeregningsGrunnlagDto =
      eksisterendePeriode && institusjonsopphold && indexTilEksisterendePeriode !== undefined
        ? {
            ...toLagreBeregningsGrunnlagDto(beregningsgrunnlag),
            institusjonsopphold: replacePeriodePaaIndex(
              formatertInstitusjonsoppholdPeriode,
              institusjonsopphold,
              indexTilEksisterendePeriode
            ),
          }
        : {
            ...toLagreBeregningsGrunnlagDto(beregningsgrunnlag),
            institusjonsopphold: !!institusjonsopphold?.length
              ? [...institusjonsopphold, formatertInstitusjonsoppholdPeriode]
              : [formatertInstitusjonsoppholdPeriode],
          }

    lagreBeregningsGrunnlagRequest(
      {
        behandlingId: behandling.id,
        grunnlag: grunnlag,
      },
      (result) => {
        dispatch(oppdaterBeregningsGrunnlag(result))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.TRYGDETID_OPPDATERT))
        paaLagre()
      }
    )
  }

  return (
    <form onSubmit={handleSubmit(lagrePeriode)}>
      <VStack gap="space-4">
        <HStack gap="space-4" align="start">
          <ControlledMaanedVelger name="fom" label="Fra og med" control={control} required />
          <ControlledMaanedVelger name="tom" label="Til og med" control={control} />
          <Select
            {...register('data.reduksjon', { validate: validerReduksjon })}
            label="Reduksjon"
            error={errors.data?.reduksjon?.message}
          >
            {Object.entries(sakType === SakType.OMSTILLINGSSTOENAD ? ReduksjonOMS : ReduksjonBP).map(([key, value]) => (
              <option key={key} value={key}>
                {value}
              </option>
            ))}
          </Select>
          {watch().data.reduksjon === 'JA_EGEN_PROSENT_AV_G' && (
            <TextField
              {...register('data.egenReduksjon', {
                valueAsNumber: true,
                required: { value: true, message: 'Må settes' },
                validate: validerStringNumber,
                shouldUnregister: true,
              })}
              label={
                <HStack gap="space-2">
                  Reduksjonsbeløp
                  <HelpText>
                    Oppgi den prosentsatsen av G som ytelsen skal reduseres med for å få riktig beløp i beregningen.
                  </HelpText>
                </HStack>
              }
              error={errors.data?.egenReduksjon?.message}
            />
          )}
        </HStack>
        <Box maxWidth="15rem">
          <Textarea {...register('data.begrunnelse')} label="Begrunnelse (valgfritt)" />
        </Box>
        <HStack gap="space-4">
          <Button size="small" icon={<FloppydiskIcon aria-hidden />} loading={isPending(lagreBeregningsGrunnlagResult)}>
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
