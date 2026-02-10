import { IBehandlingStatus, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { Alert, Box, Button, Heading, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { useAppDispatch } from '~store/Store'
import React from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterBehandlingsstatus, oppdaterViderefoertOpphoer } from '~store/reducers/BehandlingReducer'
import { lagreViderefoertOpphoer } from '~shared/api/behandling'
import { Vilkaartyper } from '~shared/api/vilkaarsvurdering'
import { addMonths, isBefore, startOfDay } from 'date-fns'
import { JaNei } from '~shared/types/ISvar'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { useForm } from 'react-hook-form'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { ControlledSingleSelectCombobox } from '~shared/components/combobox/ControlledSingleSelectCombobox'
import { concat } from 'lodash'

interface ViderefoertOpphoerVurderingData {
  skalViderefoere?: JaNei
  dato?: string
  vilkaar: string
  begrunnelse?: string
}

const vilkaarIkkeValgt: string = ''

const skjemaDefaultValues: ViderefoertOpphoerVurderingData = {
  skalViderefoere: undefined,
  dato: undefined,
  vilkaar: vilkaarIkkeValgt,
  begrunnelse: undefined,
}

const viderefoertOpphoerVurderingData = (viderefoertOpphoer: ViderefoertOpphoer | null, vilkaarTyper: Vilkaartyper) => {
  return {
    skalViderefoere: viderefoertOpphoer ? viderefoertOpphoer.skalViderefoere : skjemaDefaultValues.skalViderefoere,
    dato: viderefoertOpphoer ? viderefoertOpphoer.dato : skjemaDefaultValues.dato,
    vilkaar: viderefoertOpphoer
      ? (finnTittelFraVilkaartype(vilkaarTyper, viderefoertOpphoer.vilkaar) ?? skjemaDefaultValues.vilkaar)
      : skjemaDefaultValues.vilkaar,
    begrunnelse: viderefoertOpphoer ? viderefoertOpphoer.begrunnelse : skjemaDefaultValues.begrunnelse,
  }
}

function finnTittelFraVilkaartype(vilkaarTyper: Vilkaartyper, vilkaar: string) {
  return vilkaarTyper.typer.find((n) => n.name == vilkaar)?.tittel
}

function finnVilkaartypeFraTittel(vilkaarTyper: Vilkaartyper, tittel: string) {
  return vilkaarTyper.typer.find((p) => p.tittel === tittel)
}

export const ViderefoereOpphoerSkjema = ({
  virkningstidspunkt,
  viderefoertOpphoer,
  setVisSkjema,
  behandlingId,
  vilkaarTyper,
}: {
  viderefoertOpphoer: ViderefoertOpphoer | null
  virkningstidspunkt: Date
  setVisSkjema: (visSkjema: boolean) => void
  behandlingId: string
  vilkaarTyper: Vilkaartyper
}) => {
  const dispatch = useAppDispatch()
  const [lagreViderefoertOpphoerResult, lagreViderefoertOpphoerRequest] = useApiCall(lagreViderefoertOpphoer)
  const { handleSubmit, control, register, watch } = useForm<ViderefoertOpphoerVurderingData>({
    shouldUnregister: true,
    defaultValues: viderefoertOpphoerVurderingData(viderefoertOpphoer, vilkaarTyper),
  })

  const lagreViderefoerOpphoer = (data: ViderefoertOpphoerVurderingData) => {
    lagreViderefoertOpphoerRequest(
      {
        behandlingId,
        skalViderefoere: data.skalViderefoere,
        vilkaarType:
          data.vilkaar != vilkaarIkkeValgt ? finnVilkaartypeFraTittel(vilkaarTyper, data.vilkaar)?.name : undefined,
        begrunnelse: data.begrunnelse != '' ? data.begrunnelse : undefined,
        opphoerstidspunkt: data.dato ? new Date(data.dato) : undefined,
      },
      (viderefoertOpphoer) => {
        dispatch(oppdaterViderefoertOpphoer(viderefoertOpphoer))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        setVisSkjema(false)
      }
    )
  }

  const validerFom = (value: Date): string | undefined => {
    if (isBefore(startOfDay(new Date(value)), startOfDay(virkningstidspunkt)))
      return 'Må være på eller senere enn virkningstidspunktet'
  }

  return (
    <form onSubmit={handleSubmit(lagreViderefoerOpphoer)}>
      <VStack gap="space-4">
        <Box>
          <Heading level="3" size="small">
            Er det nødvendig å fastsette til og med-dato?
          </Heading>
          <ControlledRadioGruppe
            legend=""
            size="small"
            control={control}
            name="skalViderefoere"
            radios={
              <HStack gap="space-4">
                <Radio value={JaNei.JA}>Ja</Radio>
                <Radio value={JaNei.NEI}>Nei</Radio>
              </HStack>
            }
          ></ControlledRadioGruppe>
        </Box>
        {watch('skalViderefoere') === JaNei.JA && (
          <>
            <ControlledMaanedVelger
              fromDate={virkningstidspunkt}
              toDate={addMonths(new Date(), 6)}
              name="dato"
              label="Opphørstidspunkt"
              control={control}
              validate={validerFom}
              required
            />

            <ControlledSingleSelectCombobox
              name="vilkaar"
              control={control}
              label="Velg vilkåret som gjør at saken opphører"
              errorVedTomInput="Må fylles ut"
              options={concat(
                vilkaarTyper.typer.map((i) => i.tittel),
                vilkaarIkkeValgt
              )}
            />
          </>
        )}

        <Textarea label="Begrunnelse" placeholder="Valgfritt" {...register('begrunnelse')} />

        <HStack gap="space-2">
          {isFailure(lagreViderefoertOpphoerResult) && (
            <Alert variant="error">Kunne ikke lagre videreført opphør</Alert>
          )}
          <Button loading={isPending(lagreViderefoertOpphoerResult)} variant="primary" size="small" type="submit">
            Lagre
          </Button>
          <Button
            variant="secondary"
            size="small"
            onClick={() => {
              setVisSkjema(false)
            }}
          >
            Avbryt
          </Button>
        </HStack>
      </VStack>
    </form>
  )
}
