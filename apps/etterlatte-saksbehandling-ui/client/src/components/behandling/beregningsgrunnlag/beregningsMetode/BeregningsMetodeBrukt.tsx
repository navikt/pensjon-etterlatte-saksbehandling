import React, { useState } from 'react'
import {
  BeregningsGrunnlagDto,
  BeregningsMetode,
  BeregningsMetodeBeregningsgrunnlag,
  BeregningsMetodeBeregningsgrunnlagForm,
} from '~shared/types/Beregning'
import { BodyShort, Box, Button, Heading, HStack, Label, Radio, Textarea, VStack } from '@navikt/ds-react'
import { FloppydiskIcon, PencilIcon, PlusIcon, TagIcon, TrashIcon, XMarkIcon } from '@navikt/aksel-icons'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { isPending, Result } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'

const defaultBeregningMetode: BeregningsMetodeBeregningsgrunnlag = {
  beregningsMetode: null,
  begrunnelse: '',
}

interface Props {
  redigerbar: boolean
  oppdaterBeregningsgrunnlag: (beregningsmetodeForm: BeregningsMetodeBeregningsgrunnlagForm) => void
  eksisterendeMetode?: BeregningsMetodeBeregningsgrunnlag
  lagreBeregningsGrunnlagResult: Result<BeregningsGrunnlagDto>
  kunEnJuridiskForelder?: Boolean
  datoTilKunEnJuridiskForelder?: Date | undefined
}

export const BeregningsMetodeBrukt = ({
  redigerbar,
  oppdaterBeregningsgrunnlag,
  eksisterendeMetode,
  lagreBeregningsGrunnlagResult,
  kunEnJuridiskForelder = false,
  datoTilKunEnJuridiskForelder = undefined,
}: Props) => {
  const [redigerTrydgetidMetodeBrukt, setRedigerTrygdetidMetodeBrukt] = useState<boolean>(false)

  function toForm(
    datoTilKun: Date | undefined,
    beregningsMetodeBeregningsgrunnlag: BeregningsMetodeBeregningsgrunnlag
  ): BeregningsMetodeBeregningsgrunnlagForm {
    return {
      beregningsMetode: beregningsMetodeBeregningsgrunnlag.beregningsMetode,
      begrunnelse: beregningsMetodeBeregningsgrunnlag.begrunnelse,
      datoTilKunEnJuridiskForelder: datoTilKun,
    }
  }

  const { register, getValues, control, reset, handleSubmit } = useForm<BeregningsMetodeBeregningsgrunnlagForm>({
    defaultValues: toForm(
      datoTilKunEnJuridiskForelder,
      eksisterendeMetode ? eksisterendeMetode : defaultBeregningMetode
    ),
  })

  const slettBeregningsMetode = () => {
    oppdaterBeregningsgrunnlag({
      beregningsMetode: BeregningsMetode.NASJONAL,
      begrunnelse: '',
      datoTilKunEnJuridiskForelder: undefined,
    })
    reset(defaultBeregningMetode)
    setRedigerTrygdetidMetodeBrukt(false)
  }

  const lagreBeregningsMetode = (beregningsmetodeForm: BeregningsMetodeBeregningsgrunnlagForm) => {
    oppdaterBeregningsgrunnlag(beregningsmetodeForm)
    setRedigerTrygdetidMetodeBrukt(false)
  }

  return (
    <form onSubmit={handleSubmit(lagreBeregningsMetode)}>
      <VStack gap="4">
        <HStack gap="2" align="center">
          <TagIcon aria-hidden fontSize="1.5rem" />
          <Heading size="small" level="3">
            Trygdetid i beregning
          </Heading>
        </HStack>
        {!redigerTrydgetidMetodeBrukt && (
          <>
            <VStack gap="2">
              <Label>
                {getValues().beregningsMetode !== null
                  ? `Metode: ${formaterEnumTilLesbarString(getValues().beregningsMetode!)}`
                  : 'Metode ikke satt'}
              </Label>
              {getValues().begrunnelse && <BodyShort>{getValues().begrunnelse}</BodyShort>}
            </VStack>

            {redigerbar && (
              <HStack gap="4">
                <Button
                  type="button"
                  variant="secondary"
                  size="small"
                  icon={getValues().beregningsMetode ? <PencilIcon aria-hidden /> : <PlusIcon aria-hidden />}
                  onClick={() => setRedigerTrygdetidMetodeBrukt(true)}
                >
                  {getValues().beregningsMetode ? 'Rediger' : 'Legg til'}
                </Button>
                {getValues().beregningsMetode && (
                  <Button
                    type="button"
                    variant="secondary"
                    size="small"
                    icon={<TrashIcon aria-hidden />}
                    loading={isPending(lagreBeregningsGrunnlagResult)}
                    onClick={slettBeregningsMetode}
                  >
                    Slett
                  </Button>
                )}
              </HStack>
            )}
          </>
        )}
        {redigerbar && redigerTrydgetidMetodeBrukt && (
          <>
            {kunEnJuridiskForelder && (
              <ControlledMaanedVelger
                name="datoTilKunEnJuridiskForelder"
                label="Til og med dato for kun én juridisk forelder (Valgfritt)"
                description="Siste måneden med kun én juridisk forelder"
                control={control}
              />
            )}
            <ControlledRadioGruppe
              name="beregningsMetode"
              control={control}
              legend="Hvilken metode ble brukt?"
              errorVedTomInput="Du må velge en metode"
              radios={
                <>
                  <Radio value={BeregningsMetode.NASJONAL}>Nasjonal beregning</Radio>
                  <Radio value={BeregningsMetode.PRORATA}>Prorata</Radio>
                  <Radio value={BeregningsMetode.BEST}>Høyest verdi av nasjonal/prorata</Radio>
                </>
              }
            />

            <Box width="15rem">
              <Textarea {...register('begrunnelse')} label="Begrunnelse (valgfritt)" />
            </Box>

            {isFailureHandler({
              apiResult: lagreBeregningsGrunnlagResult,
              errorMessage: 'Feil i lagring av metode',
            })}

            <HStack gap="4">
              <Button
                size="small"
                icon={<FloppydiskIcon aria-hidden />}
                loading={isPending(lagreBeregningsGrunnlagResult)}
              >
                Lagre
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="small"
                icon={<XMarkIcon aria-hidden />}
                onClick={() => {
                  setRedigerTrygdetidMetodeBrukt(false)
                  reset()
                }}
              >
                Avbryt
              </Button>
            </HStack>
          </>
        )}
      </VStack>
    </form>
  )
}
