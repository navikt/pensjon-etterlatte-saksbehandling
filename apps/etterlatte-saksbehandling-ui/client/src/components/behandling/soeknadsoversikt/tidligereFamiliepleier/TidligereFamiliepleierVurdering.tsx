import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { Box, Button, Heading, HStack, Radio, Textarea, TextField, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { JaNei } from '~shared/types/ISvar'
import TidligereFamiliepleierVisning from '~components/behandling/soeknadsoversikt/tidligereFamiliepleier/TidligereFamiliepleierVisning'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { fnrErGyldig } from '~utils/fnr'
import { ControlledDatoVelger } from '~shared/components/datoVelger/ControlledDatoVelger'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreTidligereFamiliepleier } from '~shared/api/behandling'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { oppdaterBehandlingsstatus, oppdaterTidligereFamiliepleier } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus, ITidligereFamiliepleier } from '~shared/types/IDetaljertBehandling'

export interface TidligereFamiliepleierValues {
  svar: JaNei | null
  foedselsnummer?: string | null
  startPleieforhold?: Date | null
  opphoertPleieforhold?: Date | null
  begrunnelse: string
}

export const TidligereFamiliepleierValuesDefault: TidligereFamiliepleierValues = {
  svar: null,
  foedselsnummer: null,
  startPleieforhold: undefined,
  opphoertPleieforhold: undefined,
  begrunnelse: '',
}

const tidligereFamiliepleierValuesDefault = (tidligereFamiliepleier: ITidligereFamiliepleier | null) =>
  tidligereFamiliepleier
    ? {
        svar: tidligereFamiliepleier.svar ? JaNei.JA : JaNei.NEI,
        foedselsnummer: tidligereFamiliepleier.foedselsnummer ?? null,
        startPleieforhold: tidligereFamiliepleier.startPleieforhold
          ? new Date(tidligereFamiliepleier.startPleieforhold)
          : undefined,
        opphoertPleieforhold: tidligereFamiliepleier.opphoertPleieforhold
          ? new Date(tidligereFamiliepleier.opphoertPleieforhold)
          : undefined,
        begrunnelse: tidligereFamiliepleier.begrunnelse,
      }
    : TidligereFamiliepleierValuesDefault

export const TidligereFamiliepleierVurdering = ({
  redigerbar,
  setVurdert,
  behandlingId,
}: {
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()
  const tidligereFamiliepleier =
    useAppSelector((state) => state.behandlingReducer.behandling?.tidligereFamiliepleier) ?? null

  const [lagreTidligereFamiliepleierStatus, lagreTidligereFamiliepleierRequest] =
    useApiCall(lagreTidligereFamiliepleier)

  const [rediger, setRediger] = useState<boolean>(tidligereFamiliepleier === null)

  const {
    register,
    handleSubmit,
    formState: { errors },
    control,
    watch,
    reset,
  } = useForm<TidligereFamiliepleierValues>({
    defaultValues: tidligereFamiliepleierValuesDefault(tidligereFamiliepleier),
  })

  const lagre = (data: TidligereFamiliepleierValues) => {
    const erTidligereFamiliepleier = data.svar === JaNei.JA

    lagreTidligereFamiliepleierRequest(
      {
        behandlingId,
        svar: erTidligereFamiliepleier,
        foedselsnummer: erTidligereFamiliepleier ? data.foedselsnummer!! : undefined,
        startPleieforhold: erTidligereFamiliepleier ? data.startPleieforhold!! : undefined,
        opphoertPleieforhold: erTidligereFamiliepleier ? data.opphoertPleieforhold!! : undefined,
        begrunnelse: data.begrunnelse,
      },
      (response) => {
        dispatch(oppdaterTidligereFamiliepleier(response))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        setVurdert(true)
        setRediger(false)
      }
    )
  }

  const avbryt = () => {
    reset(tidligereFamiliepleierValuesDefault(tidligereFamiliepleier))
    setRediger(false)
    if (tidligereFamiliepleier === null) setVurdert(false)
  }

  const svar = watch('svar')
  const startPleieforhold = watch('startPleieforhold')
  const opphoertPleieforhold = watch('opphoertPleieforhold')

  return (
    <VurderingsboksWrapper
      tittel="Er søker tidligere familiepleier?"
      subtittelKomponent={<TidligereFamiliepleierVisning tidligereFamiliepleier={tidligereFamiliepleier} />}
      redigerbar={redigerbar}
      vurdering={
        tidligereFamiliepleier?.kilde
          ? {
              saksbehandler: tidligereFamiliepleier?.kilde.ident,
              tidspunkt: new Date(tidligereFamiliepleier?.kilde.tidspunkt),
            }
          : undefined
      }
      kommentar={tidligereFamiliepleier?.begrunnelse}
      defaultRediger={tidligereFamiliepleier === null}
      overstyrRediger={rediger}
      setOverstyrRediger={setRediger}
    >
      <>
        <Heading level="3" size="small">
          Er søker tidligere familiepleier?
        </Heading>

        <Box width="15rem">
          <VStack gap="space-4">
            <ControlledRadioGruppe
              name="svar"
              legend=""
              size="small"
              control={control}
              errorVedTomInput="Du må velge om gjenlevende er tidligere familiepleier"
              radios={
                <HStack gap="space-4">
                  <Radio size="small" value={JaNei.JA}>
                    Ja
                  </Radio>
                  <Radio size="small" value={JaNei.NEI}>
                    Nei
                  </Radio>
                </HStack>
              }
            />
            {svar === JaNei.JA && (
              <>
                <TextField
                  label="Fødselsnummer for forpleiede"
                  autoComplete="off"
                  {...register('foedselsnummer', {
                    required: { value: true, message: 'Du må fylle inn fødselsnummer' },
                    validate: {
                      fnrErGyldig: (value) => fnrErGyldig(value!!) || 'Ugyldig fødselsnummer',
                    },
                  })}
                  error={errors.foedselsnummer?.message}
                />
                <ControlledDatoVelger
                  name="startPleieforhold"
                  label="Pleieforholdet startet"
                  control={control}
                  fromDate={new Date(1950, 0, 1)}
                  toDate={opphoertPleieforhold ?? new Date()}
                  dropdownCaption
                />
                <ControlledDatoVelger
                  name="opphoertPleieforhold"
                  label="Pleieforholdet opphørte"
                  control={control}
                  fromDate={startPleieforhold ?? new Date(1950, 0, 1)}
                  toDate={new Date()}
                />
              </>
            )}
            <Textarea
              label="Begrunnelse"
              minRows={3}
              autoComplete="off"
              {...register('begrunnelse', {
                required: { value: true, message: 'Du må skrive en begrunnelse' },
              })}
            />
            <HStack gap="space-2">
              <Button variant="primary" type="button" size="small" onClick={handleSubmit(lagre)}>
                Lagre
              </Button>
              <Button variant="secondary" type="button" size="small" onClick={avbryt}>
                Avbryt
              </Button>
            </HStack>
          </VStack>
        </Box>
        {isFailureHandler({
          apiResult: lagreTidligereFamiliepleierStatus,
          errorMessage: 'Kunne ikke lagre tidligere familiepleier',
        })}
      </>
    </VurderingsboksWrapper>
  )
}
