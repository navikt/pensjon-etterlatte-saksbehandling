import { Box, Button, Checkbox, CheckboxGroup, HGrid, HStack, Textarea, VStack } from '@navikt/ds-react'
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
import { ControlledSingleSelectCombobox } from '~shared/components/combobox/ControlledSingleSelectCombobox'

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

  const { register, handleSubmit, control } = useForm<OppdaterTrygdetidGrunnlag>({
    defaultValues: eksisterendeGrunnlag
      ? {
          ...eksisterendeGrunnlag,
          prorata: !eksisterendeGrunnlag.prorata,
          bosted: landListe.find((land) => land.isoLandkode === eksisterendeGrunnlag.bosted)?.beskrivelse.tekst,
        }
      : initialState(trygdetidGrunnlagType),
  })

  const [trygdetidgrunnlagStatus, requestLagreTrygdetidgrunnlag] = useApiCall(lagreTrygdetidgrunnlag)

  const lagLesbarLandliste = (): Array<string> => {
    const indexTilNorge = landListe.findIndex((land) => land.isoLandkode === 'NOR')
    const kopiAvLandListe = [...landListe]
    // Flytt "Norge" til å være første i listen over land
    kopiAvLandListe.unshift(kopiAvLandListe.splice(indexTilNorge, 1)[0])
    return kopiAvLandListe.map((land) => land.beskrivelse.tekst)
  }

  const onSubmit = (data: OppdaterTrygdetidGrunnlag) => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    requestLagreTrygdetidgrunnlag(
      {
        behandlingId,
        trygdetidId,
        // Flippe verdi av prorata for å matche backend
        trygdetidgrunnlag: {
          ...data,
          prorata: !data.prorata,
          // Gjøre om land valgt fra lesbar tekst til land kode
          bosted: landListe.find((land) => land.beskrivelse.tekst === data.bosted)!.isoLandkode,
        },
      },
      (respons) => {
        setTrygdetid(respons)
      }
    )
  }

  return (
    <Box paddingInline="space-0 space-8" paddingBlock="space-8">
      <form onSubmit={handleSubmit((data) => onSubmit(data))}>
        <VStack gap="space-2">
          <HGrid gap="space-4" columns="15rem min-content 12rem">
            <ControlledSingleSelectCombobox
              name="bosted"
              control={control}
              label="Land"
              errorVedTomInput="Obligatorisk"
              options={lagLesbarLandliste()}
            />

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

          <HStack gap="space-4">
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
