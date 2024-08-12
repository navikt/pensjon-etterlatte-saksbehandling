import { IDetaljertBehandling, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { Button, HStack, Radio, Select, Textarea, VStack } from '@navikt/ds-react'
import { useAppDispatch } from '~store/Store'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreViderefoertOpphoer } from '~shared/api/behandling'
import { hentVilkaartyper, Vilkaartype, Vilkaartyper } from '~shared/api/vilkaarsvurdering'
import { JaNei } from '~shared/types/ISvar'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { SammendragAvViderefoereOpphoerVurdering } from '~components/behandling/soeknadsoversikt/viderefoereOpphoer/SammendragAvViderefoereOpphoerVurdering'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { FloppydiskIcon, XMarkIcon } from '@navikt/aksel-icons'

export const ViderefoereOpphoerVurdering = ({
  redigerbar,
  setVurdert,
  behandling,
}: {
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandling: IDetaljertBehandling
}) => {
  const dispatch = useAppDispatch()

  const [kravdato] = useState<string | undefined>()

  const [lagreViderefoertOpphoerResult, lagreViderefoertOpphoerRequest] = useApiCall(lagreViderefoertOpphoer)
  const [hentVilkaartyperResult, hentVilkaartyperRequest] = useApiCall(hentVilkaartyper)

  const {
    register,
    control,
    watch,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ViderefoertOpphoer>({
    defaultValues: behandling.viderefoertOpphoer !== null ? behandling.viderefoertOpphoer : undefined,
  })

  const finnVilkaartypeFraTittel = (vilkaar: Vilkaartyper, tittel: string) => {
    return vilkaar.typer.find((p) => p.tittel === tittel)
  }

  const lagre = (onSuccess?: () => void) => {
    handleSubmit(() => {
      console.log('Blir denen i det hele tatt trigget?')
      //TODO gjør alt her
      onSuccess?.()
    })
    //
    // if (skalViderefoere !== undefined && !vilkaarError && isSuccess(vilkaartyperResult)) {
    //   const vilkaartype = vilkaar ? finnVilkaartypeFraTittel(vilkaartyperResult.data, vilkaar)?.name || '' : undefined
    //   return setViderefoertOpphoer(
    //     { skalViderefoere, behandlingId, begrunnelse, vilkaar: vilkaartype, kravdato, opphoerstidspunkt },
    //     (viderefoertOpphoer) => {
    //       dispatch(oppdaterViderefoertOpphoer(viderefoertOpphoer))
    //       dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
    //       onSuccess?.()
    //     }
    //   )
    // }
  }

  const avbryt = (onSuccess?: () => void) => {
    reset()
    setVurdert(behandling.viderefoertOpphoer !== null)
    onSuccess?.()
  }

  useEffect(() => {
    hentVilkaartyperRequest(behandling.id)
  }, [])

  return mapResult(hentVilkaartyperResult, {
    pending: <Spinner visible label="Henter vilkårstyper..." />,
    error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente vilkårstyper'}</ApiErrorAlert>,
    success: (vilkaartyper) => (
      <VurderingsboksWrapper
        tittel="Skal opphøret umiddelbart videreføres?"
        subtittelKomponent={
          <SammendragAvViderefoereOpphoerVurdering
            viderefoereOpphoer={behandling.viderefoertOpphoer}
            vilkaartyper={vilkaartyper.typer}
          />
        }
        redigerbar={redigerbar}
        vurdering={
          behandling.viderefoertOpphoer?.kilde
            ? {
                saksbehandler: behandling.viderefoertOpphoer?.kilde.ident,
                tidspunkt: new Date(behandling.viderefoertOpphoer?.kilde.tidspunkt),
              }
            : undefined
        }
        lagreklikk={lagre}
        avbrytklikk={avbryt}
        kommentar={behandling.viderefoertOpphoer?.begrunnelse}
        defaultRediger={behandling.viderefoertOpphoer === null}
      >
        <form>
          <VStack gap="4" paddingBlock="0 3">
            <ControlledRadioGruppe
              name="skalViderefoere"
              control={control}
              legend="Skal viderefoeres?"
              radios={
                <HStack gap="4">
                  <Radio value={JaNei.JA}>Ja</Radio>
                  <Radio value={JaNei.NEI}>Nei</Radio>
                </HStack>
              }
              errorVedTomInput="Må settes"
            />
            {watch().skalViderefoere === JaNei.JA && (
              <>
                <ControlledMaanedVelger name="dato" label="Opphørstidspunkt" control={control} required />
                <Select
                  {...register('vilkaar', {
                    required: {
                      value: true,
                      message: 'Du må velge et vilkår',
                    },
                  })}
                  label="Velg vilkåret som gjør at saken opphører"
                  error={errors.vilkaar?.message}
                >
                  <option value="">Velg et vilkår</option>
                  {vilkaartyper.typer.map((type: Vilkaartype) => (
                    <option key={type.name} value={type.name}>
                      {type.tittel}
                    </option>
                  ))}
                </Select>
              </>
            )}
            <Textarea {...register('begrunnelse')} label="Begrunnelse" placeholder="Valgfritt" />
            {/*<HStack gap="4">*/}
            {/*  <Button size="small" icon={<FloppydiskIcon aria-hidden />}>*/}
            {/*    Lagre*/}
            {/*  </Button>*/}
            {/*  <Button type="button" variant="secondary" size="small" icon={<XMarkIcon aria-hidden />}>*/}
            {/*    Avbryt*/}
            {/*  </Button>*/}
            {/*</HStack>*/}
          </VStack>
        </form>
      </VurderingsboksWrapper>
    ),
  })
}
