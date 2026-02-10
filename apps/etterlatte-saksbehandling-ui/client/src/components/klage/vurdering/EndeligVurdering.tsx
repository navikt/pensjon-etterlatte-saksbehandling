import { Alert, Box, Button, Heading, HStack, Radio, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  InnstillingTilKabalUtenBrev,
  Klage,
  KlageUtfallUtenBrev,
  Omgjoering,
  teksterKlageutfall,
  Utfall,
} from '~shared/types/Klage'
import { useForm } from 'react-hook-form'
import { FieldOrNull } from '~shared/types/util'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterUtfallForKlage } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { forrigeSteg, kanSeBrev } from '~components/klage/stegmeny/KlageStegmeny'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { KlageOmgjoering } from '~components/klage/vurdering/components/KlageOmgjoering'
import { KlageInnstilling } from '~components/klage/vurdering/components/KlageInnstilling'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'

type FilledFormDataVurdering = {
  utfall: Utfall
  omgjoering?: Omgjoering
  innstilling?: InnstillingTilKabalUtenBrev
}

export type FormdataVurdering = FieldOrNull<FilledFormDataVurdering>

function erSkjemaUtfylt(skjema: FormdataVurdering): boolean {
  if (skjema.utfall === null) {
    return false
  }
  if (skjema.utfall === Utfall.OMGJOERING || skjema.utfall === Utfall.DELVIS_OMGJOERING) {
    const { omgjoering } = skjema
    if (!omgjoering || !omgjoering.begrunnelse || !omgjoering.grunnForOmgjoering) {
      return false
    }
  }
  if (skjema.utfall === Utfall.STADFESTE_VEDTAK || skjema.utfall === Utfall.DELVIS_OMGJOERING) {
    const { innstilling } = skjema
    if (!innstilling || !innstilling.lovhjemmel || !innstilling.innstillingTekst) {
      return false
    }
  }
  return true
}

function mapFraFormdataTilKlageUtfall(skjema: FieldOrNull<FilledFormDataVurdering>): KlageUtfallUtenBrev {
  switch (skjema.utfall) {
    case Utfall.DELVIS_OMGJOERING:
      return { utfall: 'DELVIS_OMGJOERING', omgjoering: skjema.omgjoering!!, innstilling: skjema.innstilling!! }
    case Utfall.OMGJOERING:
      return { utfall: 'OMGJOERING', omgjoering: skjema.omgjoering!! }
    case Utfall.STADFESTE_VEDTAK:
      return { utfall: 'STADFESTE_VEDTAK', innstilling: skjema.innstilling!! }
    default:
      throw new Error('Valgt utfall er ikke gyldig')
  }
}

function mapKlageTilFormdata(klage: Klage | null): FormdataVurdering {
  if (!klage || !klage.utfall) {
    return { utfall: null, omgjoering: null, innstilling: null }
  }
  const utfall = klage.utfall
  switch (utfall.utfall) {
    case 'DELVIS_OMGJOERING':
      return { utfall: Utfall.DELVIS_OMGJOERING, omgjoering: utfall.omgjoering, innstilling: utfall.innstilling }
    case 'OMGJOERING':
      return { utfall: Utfall.OMGJOERING, omgjoering: utfall.omgjoering }
    case 'STADFESTE_VEDTAK':
      return { utfall: Utfall.STADFESTE_VEDTAK, innstilling: utfall.innstilling }
    default:
      return { utfall: null }
  }
}

function nesteSteg(valgtUtfall: Utfall | null, klageId: string) {
  return kanSeBrev(valgtUtfall) ? `/klage/${klageId}/brev` : `/klage/${klageId}/oppsummering`
}

export function EndeligVurdering(props: { klage: Klage }) {
  const klage = props.klage

  const navigate = useNavigate()
  const [lagreUtfallStatus, lagreUtfall] = useApiCall(oppdaterUtfallForKlage)
  const dispatch = useAppDispatch()
  const stoetterDelvisOmgjoering = useFeaturetoggle(FeatureToggle.pensjon_etterlatte_klage_delvis_omgjoering)

  const [skjemaErFyltUtFeilmelding, setSkjemaErFyltUtFeilmelding] = useState<string>('')

  const {
    control,
    handleSubmit,
    register,
    watch,
    formState: { isDirty, errors },
  } = useForm<FormdataVurdering>({
    defaultValues: mapKlageTilFormdata(klage),
  })

  const valgtUtfall = watch('utfall')

  function haandterLagringVurdering(skjema: FormdataVurdering, naviger: boolean) {
    setSkjemaErFyltUtFeilmelding('')

    if (!klage) {
      return
    }
    if (!erSkjemaUtfylt(skjema)) {
      setSkjemaErFyltUtFeilmelding('Skjema er ikke fylt ut riktig, vennligst se igjennom og prøv på nytt')
      return
    }
    if (!isDirty) {
      // Skjema er fylt ut men med samme innhold som starten => skip lagring og gå videre
      if (naviger) {
        navigate(nesteSteg(skjema.utfall, klage.id))
      }
      return
    }

    const utfall = mapFraFormdataTilKlageUtfall(skjema)
    lagreUtfall({ klageId: klage.id, utfall }, (oppdatertKlage) => {
      dispatch(addKlage(oppdatertKlage))
      if (naviger) {
        navigate(nesteSteg(valgtUtfall, klage.id))
      }
    })
  }
  const skjemaLagring = (skjema: FormdataVurdering) => haandterLagringVurdering(skjema, true)
  const mellomLagring = () => haandterLagringVurdering(watch(), false)

  return (
    <>
      <Heading level="2" size="medium" spacing>
        Endelig vurdering
      </Heading>

      <form onSubmit={handleSubmit(skjemaLagring)}>
        <VStack gap="space-4">
          <ControlledRadioGruppe
            name="utfall"
            control={control}
            legend="Velg utfall"
            errorVedTomInput="Du må velge utfall for klagen"
            radios={
              <>
                <Radio value={Utfall.OMGJOERING}> {teksterKlageutfall[Utfall.OMGJOERING]}</Radio>
                {stoetterDelvisOmgjoering && (
                  <Radio value={Utfall.DELVIS_OMGJOERING}>{teksterKlageutfall[Utfall.DELVIS_OMGJOERING]}</Radio>
                )}
                <Radio value={Utfall.STADFESTE_VEDTAK}> {teksterKlageutfall[Utfall.STADFESTE_VEDTAK]}</Radio>
              </>
            }
          />

          {valgtUtfall === Utfall.STADFESTE_VEDTAK || valgtUtfall === Utfall.DELVIS_OMGJOERING ? (
            <KlageInnstilling register={register} errors={errors} />
          ) : null}

          {valgtUtfall === Utfall.OMGJOERING || valgtUtfall === Utfall.DELVIS_OMGJOERING ? (
            <KlageOmgjoering register={register} errors={errors} />
          ) : null}

          {isFailureHandler({
            apiResult: lagreUtfallStatus,
            errorMessage:
              'Kunne ikke lagre utfallet av klagen. Prøv igjen senere, og meld sak hvis problemet vedvarer.',
          })}

          {!!skjemaErFyltUtFeilmelding && <Alert variant="error">{skjemaErFyltUtFeilmelding}</Alert>}

          {!!valgtUtfall && (
            <>
              <Box>
                <Button size="small" onClick={mellomLagring} loading={isPending(lagreUtfallStatus)}>
                  {teksterLagring[valgtUtfall].toLowerCase()}
                </Button>
              </Box>
              {isSuccess(lagreUtfallStatus) && (
                <Box maxWidth="fit-content">
                  <Alert size="small" variant="success">
                    Utfall er lagret!
                  </Alert>
                </Box>
              )}
            </>
          )}
          <HStack gap="space-4" justify="center">
            <Button type="button" variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'vurdering'))}>
              Gå tilbake
            </Button>
            <Button loading={isPending(lagreUtfallStatus)} type="submit" variant="primary">
              {kanSeBrev(valgtUtfall) ? 'Gå til brev' : 'Gå til oppsummering'}
            </Button>
          </HStack>
        </VStack>
      </form>
    </>
  )
}

const teksterLagring: Record<Utfall, string> = {
  AVVIST: 'Lagre avvist klage',
  AVVIST_MED_OMGJOERING: 'Lagre avvisning med omgjøring',
  DELVIS_OMGJOERING: 'Lagre delvis omgjøring',
  OMGJOERING: 'Lagre omgjøring',
  STADFESTE_VEDTAK: 'Lagre innstilling',
}
