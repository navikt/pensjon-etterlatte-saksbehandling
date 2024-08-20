import { Box, Button, Checkbox, CheckboxGroup, HGrid, HStack, Select, Textarea, VStack } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  ITrygdetid,
  ITrygdetidGrunnlag,
  ITrygdetidGrunnlagType,
  lagreTrygdetidgrunnlag,
  OppdaterTrygdetidGrunnlag,
} from '~shared/api/trygdetid'
import React from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useParams } from 'react-router-dom'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { useForm } from 'react-hook-form'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'
import { ILand } from '~utils/kodeverk'

type Props = {
  eksisterendeGrunnlag: ITrygdetidGrunnlag | undefined
  trygdetidId: string
  setTrygdetid: (trygdetid: ITrygdetid) => void
  avbryt: () => void
  trygdetidGrunnlagType: ITrygdetidGrunnlagType
  landListe: ILand[]
}

const initialState = (type: ITrygdetidGrunnlagType) => {
  return { type: type, bosted: '', poengInnAar: false, poengUtAar: false, prorata: false }
}

export const TrygdetidGrunnlag = ({
  trygdetidId,
  eksisterendeGrunnlag,
  setTrygdetid,
  avbryt,
  trygdetidGrunnlagType,
  landListe,
}: Props) => {
  const { behandlingId } = useParams()

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
    getValues,
  } = useForm<OppdaterTrygdetidGrunnlag>({
    defaultValues: eksisterendeGrunnlag
      ? { ...eksisterendeGrunnlag, prorata: !eksisterendeGrunnlag.prorata }
      : initialState(trygdetidGrunnlagType),
  })

  const [trygdetidgrunnlagStatus, requestLagreTrygdetidgrunnlag] = useApiCall(lagreTrygdetidgrunnlag)

  const onSubmit = (data: OppdaterTrygdetidGrunnlag) => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    requestLagreTrygdetidgrunnlag(
      {
        behandlingId,
        trygdetidId,
        // Flippe verdi av prorata for å matche backend
        trygdetidgrunnlag: { ...data, prorata: !data.prorata },
      },
      (respons) => {
        setTrygdetid(respons)
      }
    )
  }

  return (
    <Box paddingInline="0 8" paddingBlock="8">
      <form onSubmit={handleSubmit((data) => onSubmit(data))}>
        <VStack gap="2">
          <HGrid gap="4" columns="15rem min-content 12rem">
            <Select
              {...register('bosted', {
                required: {
                  value: true,
                  message: 'Obligatorisk',
                },
              })}
              label="Land"
              key={`${getValues().bosted}-${trygdetidGrunnlagType}`}
              autoComplete="off"
              error={errors.bosted?.message}
            >
              <option value="">Velg land</option>
              {landListe.map((land) => (
                <option key={`${land.isoLandkode}-${trygdetidGrunnlagType}`} value={land.isoLandkode}>
                  {land.beskrivelse.tekst}
                </option>
              ))}
            </Select>

            <ControlledDatoVelger
              name="periodeFra"
              label="Fra dato"
              control={control}
              errorVedTomInput="Obligatorisk"
            />
            <ControlledDatoVelger
              name="periodeTil"
              label="Til dato"
              control={control}
              errorVedTomInput="Obligatorisk"
            />

            <Textarea
              {...register('begrunnelse')}
              key={`begrunnelse-${trygdetidGrunnlagType}`}
              label="Begrunnelse"
              placeholder="Valgfritt"
              minRows={3}
              autoComplete="off"
            />
            {trygdetidGrunnlagType === ITrygdetidGrunnlagType.FAKTISK && (
              <>
                <CheckboxGroup legend="Poeng i inn/ut år">
                  {/* Stoppe aksel å klage på at checkbox ikke har value, mens RHF styrer den */}
                  {/* Hvis man setter verdien fra RHF i Aksel Checkbox vil den overridet til string */}
                  <Checkbox {...register('poengInnAar')} value="">
                    Poeng i inn år
                  </Checkbox>
                  <Checkbox {...register('poengUtAar')} value="">
                    Poeng i ut år
                  </Checkbox>
                </CheckboxGroup>

                <CheckboxGroup legend="Prorata">
                  <Checkbox {...register('prorata')} value="">
                    Ikke med i prorata
                  </Checkbox>
                </CheckboxGroup>
              </>
            )}
          </HGrid>

          <HStack gap="4">
            <Button
              size="small"
              loading={isPending(trygdetidgrunnlagStatus)}
              type="submit"
              icon={<FloppydiskIcon aria-hidden />}
            >
              Lagre
            </Button>
            <Button size="small" onClick={avbryt} variant="secondary" icon={<XMarkIcon aria-hidden />}>
              Avbryt
            </Button>
          </HStack>
        </VStack>
      </form>
      {mapFailure(trygdetidgrunnlagStatus, (error) =>
        error.status === 409 ? (
          <ApiErrorAlert>Trygdetidsperioder kan ikke være overlappende</ApiErrorAlert>
        ) : (
          <ApiErrorAlert>{error.detail}</ApiErrorAlert>
        )
      )}
    </Box>
  )
}
