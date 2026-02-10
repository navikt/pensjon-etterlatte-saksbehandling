import { Alert, BodyLong, Box, Button, Heading, HStack, Label, Radio, VStack } from '@navikt/ds-react'
import React from 'react'
import { useNavigate } from 'react-router-dom'
import { InnstillingTilKabalUtenBrev, Klage, KlageUtfallUtenBrev, Omgjoering, Utfall } from '~shared/types/Klage'
import { useForm } from 'react-hook-form'
import { FieldOrNull } from '~shared/types/util'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterUtfallForKlage } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { forrigeSteg, kanSeBrev } from '~components/klage/stegmeny/KlageStegmeny'
import { erSkjemaUtfylt, KlageOmgjoering } from '~components/klage/vurdering/KlageVurderingForms'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { FeatureToggle, useFeaturetoggle } from '~useUnleash'
import { JaNei } from '~shared/types/ISvar'

type FilledFormDataVurdering = {
  utfall: Utfall
  omgjoering?: Omgjoering
  innstilling?: InnstillingTilKabalUtenBrev
}

type FormdataVurdering = FieldOrNull<FilledFormDataVurdering>

export function KlageAvvisning(props: { klage: Klage }) {
  const navigate = useNavigate()
  const { klage } = props
  const [lagreUtfallStatus, lagreUtfall] = useApiCall(oppdaterUtfallForKlage)
  const dispatch = useAppDispatch()

  const {
    control,
    handleSubmit,
    watch,
    formState: { isDirty },
  } = useForm<FormdataVurdering>({
    defaultValues: mapKlageTilFormdata(klage),
  })

  const valgtUtfall: Utfall | null = watch('utfall')

  function sendInnVurdering(skjema: FormdataVurdering) {
    if (!klage) {
      return
    }
    if (!erSkjemaUtfylt(skjema)) {
      // Gjør noe bedre håndtering her
      return
    }
    if (!isDirty) {
      // Skjema er fylt ut men med samme innhold som starten => skip lagring og gå videre
      navigate(nesteSteg(skjema.utfall, klage.id))
    }

    const utfall = mapFraFormdataTilKlageUtfall(skjema)
    lagreUtfall({ klageId: klage.id, utfall }, (oppdatertKlage) => {
      dispatch(addKlage(oppdatertKlage))
      navigate(nesteSteg(valgtUtfall, klage.id))
    })
  }

  const vedtakOmAvvistErAktivert = useFeaturetoggle(FeatureToggle.pensjon_etterlatte_kan_opprette_vedtak_avvist_klage)

  const lagreUtfallAktivert = valgtUtfall !== Utfall.AVVIST || vedtakOmAvvistErAktivert

  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading level="1" size="large">
          Avvis klagen
        </Heading>
      </Box>
      <form onSubmit={handleSubmit(sendInnVurdering)}>
        <Box paddingBlock="space-8" paddingInline="space-16 space-8">
          <VStack gap="space-4">
            {klage.formkrav?.formkrav?.erKlagenFramsattInnenFrist === JaNei.NEI && (
              <BodyLong>
                Siden klagefristen ikke er overholdt må klagen formelt avvises, men du kan likevel bestemme at vedtaket
                skal omgjøres.
              </BodyLong>
            )}
            {!!klage.formkrav?.klagerHarIkkeSvartVurdering?.begrunnelse && (
              <>
                <BodyLong>
                  Klager har ikke svart på vår henvendelse om å oppfylle formkravene. Klagen skal avvises.
                </BodyLong>
                <Label>Begrunnelse</Label>
                <BodyLong>{klage.formkrav.klagerHarIkkeSvartVurdering.begrunnelse}</BodyLong>
              </>
            )}

            <ControlledRadioGruppe
              name="utfall"
              control={control}
              legend="Velg utfall"
              errorVedTomInput="Du må velge et utfall for klagen"
              radios={
                <>
                  <Radio value={Utfall.AVVIST}>Vedtak om avvisning</Radio>
                  <Radio value={Utfall.AVVIST_MED_OMGJOERING}>Avvist med omgjøring</Radio>
                </>
              }
            />

            {valgtUtfall === Utfall.AVVIST_MED_OMGJOERING ? <KlageOmgjoering control={control} /> : null}
            {!lagreUtfallAktivert && (
              <Alert variant="info" inline>
                Det skal fattes et vedtak om avvisning. Gjenny støtter ikke dette ennå, men det kommer snart.
              </Alert>
            )}
          </VStack>
        </Box>

        {isFailureHandler({
          apiResult: lagreUtfallStatus,
          errorMessage: 'Kunne ikke lagre utfallet av klagen. Prøv igjen senere, og meld sak hvis problemet vedvarer.',
        })}

        <HStack gap="space-4" justify="center">
          <Button type="button" variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'vurdering'))}>
            Gå tilbake
          </Button>
          {lagreUtfallAktivert && (
            <Button loading={isPending(lagreUtfallStatus)} type="submit" variant="primary">
              {kanSeBrev(valgtUtfall) ? 'Gå til brev' : 'Gå til oppsummering'}
            </Button>
          )}
        </HStack>
      </form>
    </>
  )
}

function nesteSteg(valgtUtfall: Utfall | null, klageId: string) {
  return kanSeBrev(valgtUtfall) ? `/klage/${klageId}/brev` : `/klage/${klageId}/oppsummering`
}

function mapFraFormdataTilKlageUtfall(skjema: FilledFormDataVurdering): KlageUtfallUtenBrev {
  switch (skjema.utfall) {
    case Utfall.AVVIST_MED_OMGJOERING:
      return { utfall: 'AVVIST_MED_OMGJOERING', omgjoering: skjema.omgjoering!! }
    case Utfall.AVVIST:
      return { utfall: 'AVVIST' }
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
    case 'AVVIST_MED_OMGJOERING':
      return { utfall: Utfall.AVVIST_MED_OMGJOERING, omgjoering: utfall.omgjoering }
    case 'AVVIST':
      return { utfall: Utfall.AVVIST }
    default:
      return { utfall: null }
  }
}
