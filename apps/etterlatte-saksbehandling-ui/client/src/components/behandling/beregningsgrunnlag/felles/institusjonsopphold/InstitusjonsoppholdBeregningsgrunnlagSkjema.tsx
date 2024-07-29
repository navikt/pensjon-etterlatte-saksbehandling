import React from 'react'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import {
  BeregningsGrunnlagOMSPostDto,
  BeregningsMetode,
  InstitusjonsoppholdGrunnlagDTO,
  InstitusjonsoppholdIBeregning,
  ReduksjonKey,
  ReduksjonOMS,
} from '~shared/types/Beregning'
import { Box, Button, HelpText, HStack, Select, Textarea, TextField, VStack } from '@navikt/ds-react'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { useForm } from 'react-hook-form'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'
import { validerStringNumber } from '~components/person/journalfoeringsoppgave/nybehandling/validator'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlagOMS } from '~shared/api/beregning'
import { SakType } from '~shared/types/sak'
import { oppdaterBeregingsGrunnlagOMS } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { isPending } from '~shared/api/apiUtils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  initalInstitusjonsoppholdPeriode,
  konverterTilSisteDagIMaaneden,
} from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/utils'

interface Props {
  behandling: IDetaljertBehandling
  sakType: SakType
  beregningsgrunnlag?: BeregningsGrunnlagOMSPostDto | undefined
  eksisterendePeriode?: PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>
  indexTilPeriode?: number
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
  paaAvbryt: () => void
  paaLagre: () => void
}

export const InstitusjonsoppholdBeregningsgrunnlagSkjema = ({
  behandling,
  sakType,
  beregningsgrunnlag,
  indexTilPeriode,
  eksisterendePeriode,
  institusjonsopphold,
  paaAvbryt,
  paaLagre,
}: Props) => {
  const [lagreBeregningsGrunnlagOMSResult, lagreBeregningsGrunnlagOMSRequest] = useApiCall(lagreBeregningsGrunnlagOMS)

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
      : initalInstitusjonsoppholdPeriode(behandling, institusjonsopphold?.[institusjonsopphold?.length - 1]),
  })

  const validerReduksjon = (reduksjon: ReduksjonKey): string | undefined => {
    if (reduksjon === 'VELG_REDUKSJON') return 'Må settes'
    return undefined
  }

  const lagrePeriode = (
    institusjonsoppholdPeriode: PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>
  ) => {
    // TODO: Håndtere for at egenReduksjon kan bli satt til NaN hvis reduksjon !== 'JA_EGEN_PROSENT_AV_G'

    const formatertInstitusjonsoppholdPeriode = {
      ...institusjonsoppholdPeriode,
      tom: institusjonsoppholdPeriode.tom && konverterTilSisteDagIMaaneden(institusjonsoppholdPeriode.tom),
    }

    const grunnlag = {
      beregningsMetode: beregningsgrunnlag?.beregningsMetode ?? { beregningsMetode: BeregningsMetode.NASJONAL },
      institusjonsopphold: !!institusjonsopphold?.length
        ? [...institusjonsopphold, formatertInstitusjonsoppholdPeriode]
        : [formatertInstitusjonsoppholdPeriode],
    }

    if (sakType === SakType.OMSTILLINGSSTOENAD) {
      if (eksisterendePeriode && institusjonsopphold && indexTilPeriode !== undefined) {
        // TODO: lage logikk for å redigere og replace eksistenrende periode
      } else {
        lagreBeregningsGrunnlagOMSRequest(
          {
            behandlingId: behandling.id,
            grunnlag: grunnlag,
          },
          () => {
            dispatch(oppdaterBeregingsGrunnlagOMS(grunnlag))
            paaLagre()
          }
        )
      }
    }
  }

  return (
    <form onSubmit={handleSubmit(lagrePeriode)}>
      <VStack gap="4">
        <HStack gap="4" align="start">
          <ControlledMaanedVelger name="fom" label="Fra og med" control={control} required />
          <ControlledMaanedVelger name="tom" label="Til og med" control={control} />
          <Select
            {...register('data.reduksjon', { validate: validerReduksjon })}
            label="Reduksjon"
            error={errors.data?.reduksjon?.message}
          >
            {Object.entries(ReduksjonOMS).map(([key, value]) => (
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
              })}
              label={
                <HStack gap="2">
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
        <HStack gap="4">
          <Button variant="secondary" type="button" size="small" icon={<XMarkIcon aria-hidden />} onClick={paaAvbryt}>
            Avbryt
          </Button>
          <Button
            size="small"
            icon={<FloppydiskIcon aria-hidden />}
            loading={isPending(lagreBeregningsGrunnlagOMSResult)}
          >
            Lagre
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
