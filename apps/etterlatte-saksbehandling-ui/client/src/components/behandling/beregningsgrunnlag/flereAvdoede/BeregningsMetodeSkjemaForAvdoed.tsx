import React from 'react'
import { BodyShort, Box, Button, Heading, HStack, Radio, ReadMore, Textarea, VStack } from '@navikt/ds-react'
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
  eksisterendeMetode?: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed> | undefined
  paaAvbryt: () => void
  oppdaterBeregningsMetodeForAvdoed: (nyMetode: PeriodisertBeregningsgrunnlag<BeregningsmetodeForAvdoed>) => void
  lagreBeregningsgrunnlagResult: Result<void>
}

export const BeregningsMetodeSkjemaForAvdoed = ({
  ident,
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
          legend="Trygdetid i beregning"
          errorVedTomInput="Du må velge en metode"
          radios={
            <>
              <Radio value={BeregningsMetode.NASJONAL}>Nasjonal beregning (folketrygdberegning)</Radio>
              <Radio value={BeregningsMetode.PRORATA}>
                Prorata (EØS/avtaleland, der rettighet er oppfylt ved sammenlegging)
              </Radio>
              <Radio value={BeregningsMetode.BEST}>
                Den som gir høyest verdi av nasjonal/prorata (EØS/avtale-land, der rettighet er oppfylt etter nasjonale
                regler)
              </Radio>
            </>
          }
        />
        <Heading size="xsmall" level="4">
          Gyldig for beregning
        </Heading>
        <BodyShort>
          Disse datoene brukes til å regne ut satsen for barnepensjon ut ifra om det er en eller to forelder død.
        </BodyShort>
        <HStack gap="4">
          <ControlledMaanedVelger
            name="fom"
            label="Fra og med"
            description="Måned etter dødsfall"
            control={control}
            required
            validate={validerFom}
          />
          <VStack>
            <ControlledMaanedVelger
              name="tom"
              label="Til og med (valgfritt)"
              description="Siste måneden med foreldrerett"
              control={control}
            />
            <ReadMore header="Når skal du oppgi til og med dato">
              <Box maxWidth="30rem">
                Beregningen gjelder for perioden der den avdøde regnes som forelder for barnet. Hvis barnet har blitt
                adoptert skal datoen “til og med” oppgis. Velg forelderen med dårligst trygdetid hvis det kun er én
                adoptivforelder.
              </Box>
            </ReadMore>
          </VStack>
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
