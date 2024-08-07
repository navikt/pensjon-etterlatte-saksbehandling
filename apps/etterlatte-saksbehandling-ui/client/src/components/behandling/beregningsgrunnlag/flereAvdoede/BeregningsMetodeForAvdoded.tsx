import React from 'react'
import { Box, Button, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { PeriodisertBeregningsgrunnlag } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'
import { BeregningsMetode, BeregningsmetodeForAvdoed } from '~shared/types/Beregning'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { startOfDay } from 'date-fns'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'
import { isPending, Result } from '~shared/api/apiUtils'

const setDefaultBeregningsMetodeForAvdoed = (
  ident: string
): PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> => {
  return {
    fom: new Date(),
    tom: undefined,
    data: {
      beregningsMetode: {
        beregningsMetode: null,
        begrunnelse: null,
      },
      avdoed: ident,
    },
  }
}

interface Props {
  ident: string
  navn: string
  eksisterendeMetode?: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined
  paaAvbryt: () => void
  oppdaterBeregningsMetodeForAvdoed: (nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>) => void
  lagreBeregningsgrunnlagResult: Result<void>
}

export const BeregningsMetodeForAvdoded = ({
  ident,
  navn,
  eksisterendeMetode,
  paaAvbryt,
  oppdaterBeregningsMetodeForAvdoed,
  lagreBeregningsgrunnlagResult,
}: Props) => {
  const { register, control, getValues, handleSubmit, reset } = useForm<
    PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>
  >({
    defaultValues: eksisterendeMetode
      ? {
          ...eksisterendeMetode,
          data: {
            ...eksisterendeMetode.data,
            avdoed: ident,
          },
        }
      : setDefaultBeregningsMetodeForAvdoed(ident),
  })

  const validerFom = (value: Date): string | undefined => {
    const fom = startOfDay(new Date(value))
    const tom = getValues().tom ? startOfDay(new Date(getValues().tom!)) : undefined

    if (tom && fom > tom) {
      return 'Fra-måned kan ikke være etter til-måned.'
    }

    return undefined
  }

  return (
    <form onSubmit={handleSubmit(oppdaterBeregningsMetodeForAvdoed)}>
      <VStack gap="4">
        <ControlledRadioGruppe
          name="data.beregningsMetode.beregningsMetode"
          control={control}
          legend={`Hvilken metode ble brukt for ${navn}?`}
          errorVedTomInput="Du må velge en metode"
          radios={
            <>
              <Radio value={BeregningsMetode.NASJONAL}>Nasjonal beregning</Radio>
              <Radio value={BeregningsMetode.PRORATA}>Prorata</Radio>
              <Radio value={BeregningsMetode.BEST}>Høyest verdi av nasjonal/prorata</Radio>
            </>
          }
        />

        <HStack gap="4">
          <ControlledMaanedVelger name="fom" label="Fra og med" control={control} required validate={validerFom} />
          <ControlledMaanedVelger name="tom" label="Til og med" control={control} />
        </HStack>

        <Box width="15rem">
          <Textarea {...register('data.beregningsMetode.begrunnelse')} label="Begrunnelse (valgfritt)" />
        </Box>

        <HStack gap="4">
          <Button size="small" icon={<FloppydiskIcon aria-hidden />} loading={isPending(lagreBeregningsgrunnlagResult)}>
            Lagre
          </Button>
          <Button
            type="button"
            variant="secondary"
            size="small"
            icon={<XMarkIcon aria-hidden />}
            onClick={() => {
              reset()
              paaAvbryt()
            }}
          >
            Avbryt
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
