import React, { useState } from 'react'
import {
  BeregningsGrunnlagOMSDto,
  BeregningsMetode,
  BeregningsMetodeBeregningsgrunnlag,
  InstitusjonsoppholdGrunnlagData,
} from '~shared/types/Beregning'
import { BodyShort, Box, Button, Heading, HStack, Label, Radio, Textarea, VStack } from '@navikt/ds-react'
import { FloppydiskIcon, PencilIcon, PlusIcon, TagIcon, TrashIcon, XMarkIcon } from '@navikt/aksel-icons'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBeregningsGrunnlagOMS } from '~shared/api/beregning'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { mapListeTilDto } from '~components/behandling/beregningsgrunnlag/PeriodisertBeregningsgrunnlag'

const defaultBeregningMetode: BeregningsMetodeBeregningsgrunnlag = {
  beregningsMetode: null,
  begrunnelse: '',
}

interface Props {
  redigerbar: boolean
  behandling: IBehandlingReducer
  beregningsgrunnlag: BeregningsGrunnlagOMSDto | null
  institusjonsoppholdsGrunnlagData: InstitusjonsoppholdGrunnlagData | null
}

export const TrygdetidMetodeBruktOMS = ({
  redigerbar,
  behandling,
  beregningsgrunnlag,
  institusjonsoppholdsGrunnlagData,
}: Props) => {
  const [redigerTrydgetidMetodeBrukt, setRedigerTrygdetidMetodeBrukt] = useState<boolean>(false)

  const [lagreBeregningsGrunnlagOMSResult, lagreBeregningsGrunnlagOMSRequest] = useApiCall(lagreBeregningsGrunnlagOMS)

  const { register, getValues, control, reset, handleSubmit } = useForm<BeregningsMetodeBeregningsgrunnlag>({
    defaultValues:
      beregningsgrunnlag && beregningsgrunnlag.beregningsMetode
        ? beregningsgrunnlag.beregningsMetode
        : defaultBeregningMetode,
  })

  const slettBeregningsMetode = () => {
    lagreBeregningsGrunnlagOMSRequest(
      {
        behandlingId: behandling.id,
        grunnlag: {
          ...beregningsgrunnlag,
          // Hvis man skal "slette" metode, så defaulter man til beregningsmetode NASJONAL
          beregningsMetode: {
            beregningsMetode: BeregningsMetode.NASJONAL,
            begrunnelse: '',
          },
          institusjonsopphold:
            beregningsgrunnlag && institusjonsoppholdsGrunnlagData
              ? mapListeTilDto(institusjonsoppholdsGrunnlagData)
              : behandling.beregningsGrunnlag?.institusjonsopphold ?? [],
        },
      },
      () => {
        reset(defaultBeregningMetode)
        setRedigerTrygdetidMetodeBrukt(false)
      }
    )
  }

  const lagreBeregningsMetode = (data: BeregningsMetodeBeregningsgrunnlag) => {
    lagreBeregningsGrunnlagOMSRequest(
      {
        behandlingId: behandling.id,
        grunnlag: {
          ...beregningsgrunnlag,
          beregningsMetode: data,
          institusjonsopphold:
            beregningsgrunnlag && institusjonsoppholdsGrunnlagData
              ? mapListeTilDto(institusjonsoppholdsGrunnlagData)
              : behandling.beregningsGrunnlag?.institusjonsopphold ?? [],
        },
      },
      () => {
        setRedigerTrygdetidMetodeBrukt(false)
      }
    )
  }

  return (
    <form onSubmit={handleSubmit(lagreBeregningsMetode)}>
      <VStack gap="4">
        <HStack gap="2" align="center">
          <TagIcon aria-hidden fontSize="1.5rem" />
          <Heading size="small" level="3">
            Trygdetid brukt i beregningen
          </Heading>
        </HStack>
        {!redigerTrydgetidMetodeBrukt && (
          <>
            <VStack gap="2">
              <Label>
                {getValues().beregningsMetode !== null
                  ? `Metode brukt: ${formaterEnumTilLesbarString(getValues().beregningsMetode!)}`
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
                    loading={isPending(lagreBeregningsGrunnlagOMSResult)}
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
              apiResult: lagreBeregningsGrunnlagOMSResult,
              errorMessage: 'Feil i lagring av metode',
            })}

            <HStack gap="4">
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
              <Button
                size="small"
                icon={<FloppydiskIcon aria-hidden />}
                loading={isPending(lagreBeregningsGrunnlagOMSResult)}
              >
                Lagre
              </Button>
            </HStack>
          </>
        )}
      </VStack>
    </form>
  )
}
