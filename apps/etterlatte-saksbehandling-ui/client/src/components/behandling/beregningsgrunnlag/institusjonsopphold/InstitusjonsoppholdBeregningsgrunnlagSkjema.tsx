import React from 'react'
import { PeriodisertBeregningsgrunnlagDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import {
  BeregningsGrunnlagDto,
  BeregningsGrunnlagOMSDto,
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
import { lagreBeregningsGrunnlag, lagreBeregningsGrunnlagOMS } from '~shared/api/beregning'
import { SakType } from '~shared/types/sak'
import { oppdaterBeregingsGrunnlag, oppdaterBeregingsGrunnlagOMS } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { isPending } from '~shared/api/apiUtils'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  initalInstitusjonsoppholdPeriode,
  konverterTilSisteDagIMaaneden,
  replacePeriodePaaIndex,
} from '~components/behandling/beregningsgrunnlag/overstyrGrunnlagsBeregning/utils'

interface Props {
  behandling: IDetaljertBehandling
  sakType: SakType
  beregningsgrunnlag?: BeregningsGrunnlagDto | BeregningsGrunnlagOMSDto
  eksisterendePeriode?: PeriodisertBeregningsgrunnlagDto<InstitusjonsoppholdIBeregning>
  indexTilEksisterendePeriode?: number
  institusjonsopphold: InstitusjonsoppholdGrunnlagDTO | undefined
  paaAvbryt: () => void
  paaLagre: () => void
}

export const InstitusjonsoppholdBeregningsgrunnlagSkjema = ({
  behandling,
  sakType,
  beregningsgrunnlag,
  indexTilEksisterendePeriode,
  eksisterendePeriode,
  institusjonsopphold,
  paaAvbryt,
  paaLagre,
}: Props) => {
  const [lagreBeregningsGrunnlagBPResult, lagreBeregningsGrunnlagBPRequest] = useApiCall(lagreBeregningsGrunnlag)
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
    const formatertInstitusjonsoppholdPeriode = {
      ...institusjonsoppholdPeriode,
      tom: institusjonsoppholdPeriode.tom && konverterTilSisteDagIMaaneden(institusjonsoppholdPeriode.tom),
    }

    if (eksisterendePeriode && institusjonsopphold && indexTilEksisterendePeriode !== undefined) {
      const grunnlag = {
        beregningsMetode: beregningsgrunnlag?.beregningsMetode ?? { beregningsMetode: BeregningsMetode.NASJONAL },
        institusjonsopphold: replacePeriodePaaIndex(
          formatertInstitusjonsoppholdPeriode,
          institusjonsopphold,
          indexTilEksisterendePeriode
        ),
      }
      if (sakType === SakType.OMSTILLINGSSTOENAD) {
        lagreBeregningsGrunnlagOMSRequest(
          {
            behandlingId: behandling.id,
            grunnlag,
          },
          () => {
            dispatch(oppdaterBeregingsGrunnlagOMS(grunnlag))
            paaLagre()
          }
        )
      } else if (sakType === SakType.BARNEPENSJON) {
        const beregningsgrunnlagBP = beregningsgrunnlag as BeregningsGrunnlagDto
        lagreBeregningsGrunnlagBPRequest(
          {
            behandlingId: behandling.id,
            grunnlag: {
              ...beregningsgrunnlagBP,
              institusjonsopphold: grunnlag.institusjonsopphold,
            },
          },
          () => {
            dispatch(
              oppdaterBeregingsGrunnlag({ ...beregningsgrunnlagBP, institusjonsopphold: grunnlag.institusjonsopphold })
            )
            paaLagre()
          }
        )
      }
    } else {
      const grunnlag = {
        beregningsMetode: beregningsgrunnlag?.beregningsMetode ?? { beregningsMetode: BeregningsMetode.NASJONAL },
        institusjonsopphold: !!institusjonsopphold?.length
          ? [...institusjonsopphold, formatertInstitusjonsoppholdPeriode]
          : [formatertInstitusjonsoppholdPeriode],
      }
      if (sakType === SakType.OMSTILLINGSSTOENAD) {
        lagreBeregningsGrunnlagOMSRequest(
          {
            behandlingId: behandling.id,
            grunnlag,
          },
          () => {
            dispatch(oppdaterBeregingsGrunnlagOMS(grunnlag))
            paaLagre()
          }
        )
      } else if (sakType === SakType.BARNEPENSJON) {
        const beregningsgrunnlagBP = beregningsgrunnlag as BeregningsGrunnlagDto
        lagreBeregningsGrunnlagBPRequest(
          {
            behandlingId: behandling.id,
            grunnlag: {
              ...beregningsgrunnlagBP,
              beregningsMetode: grunnlag.beregningsMetode,
              institusjonsopphold: grunnlag.institusjonsopphold,
            },
          },
          () => {
            dispatch(
              oppdaterBeregingsGrunnlag({
                ...beregningsgrunnlagBP,
                beregningsMetode: grunnlag.beregningsMetode,
                institusjonsopphold: grunnlag.institusjonsopphold,
              })
            )
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
          <Button
            size="small"
            icon={<FloppydiskIcon aria-hidden />}
            loading={isPending(lagreBeregningsGrunnlagOMSResult) || isPending(lagreBeregningsGrunnlagBPResult)}
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
