import { Button, Heading, Radio, RadioGroup } from '@navikt/ds-react'
import React from 'react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Feilmelding, Innhold, VurderingWrapper } from '~components/klage/styled'
import { useNavigate } from 'react-router-dom'
import { Klage, KlageUtfallUtenBrev, Utfall } from '~shared/types/Klage'
import { Controller, useForm } from 'react-hook-form'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterUtfallForKlage } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { forrigeSteg } from '~components/klage/stegmeny/KlageStegmeny'
import {
  erSkjemaUtfylt,
  FilledFormDataVurdering,
  FormdataVurdering,
  KlageInnstilling,
  KlageOmgjoering,
} from '~components/klage/vurdering/KlageVurderingForms'

export function KlageVurderingRedigering(props: { klage: Klage }) {
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

  const valgtUtfall = watch('utfall')

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

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Ta stilling til klagen
          </Heading>
        </HeadingWrapper>
      </ContentHeader>

      <form onSubmit={handleSubmit(sendInnVurdering)}>
        <Innhold>
          <VurderingWrapper>
            <Controller
              rules={{
                required: true,
              }}
              name="utfall"
              control={control}
              render={({ field, fieldState }) => (
                <>
                  <RadioGroup legend="Velg utfall" {...field}>
                    <Radio value={Utfall.OMGJOERING}>Omgjøring av vedtak</Radio>
                    <Radio value={Utfall.DELVIS_OMGJOERING}>Delvis omgjøring av vedtak</Radio>
                    <Radio value={Utfall.STADFESTE_VEDTAK}>Stadfeste vedtaket</Radio>
                  </RadioGroup>
                  {fieldState.error ? <Feilmelding>Du må velge et utfall for klagen.</Feilmelding> : null}
                </>
              )}
            />
          </VurderingWrapper>

          {valgtUtfall === Utfall.STADFESTE_VEDTAK || valgtUtfall === Utfall.DELVIS_OMGJOERING ? (
            <KlageInnstilling control={control} />
          ) : null}

          {valgtUtfall === Utfall.OMGJOERING || valgtUtfall === Utfall.DELVIS_OMGJOERING ? (
            <KlageOmgjoering control={control} />
          ) : null}
        </Innhold>

        {isFailureHandler({
          apiResult: lagreUtfallStatus,
          errorMessage: 'Kunne ikke lagre utfallet av klagen. Prøv igjen senere, og meld sak hvis problemet vedvarer.',
        })}

        <FlexRow justify="center">
          <Button type="button" variant="secondary" onClick={() => navigate(forrigeSteg(klage, 'vurdering'))}>
            Gå tilbake
          </Button>
          <Button loading={isPending(lagreUtfallStatus)} type="submit" variant="primary">
            {kanSeBrev(valgtUtfall) ? 'Gå til brev' : 'Gå til oppsummering'}
          </Button>
        </FlexRow>
      </form>
    </Content>
  )
}

function kanSeBrev(valgtUtfall: Utfall | null) {
  switch (valgtUtfall) {
    case 'DELVIS_OMGJOERING':
    case 'STADFESTE_VEDTAK':
      return true
  }
  return false
}

function nesteSteg(valgtUtfall: Utfall | null, klageId: string) {
  return kanSeBrev(valgtUtfall) ? `/klage/${klageId}/brev` : `/klage/${klageId}/oppsummering`
}

function mapFraFormdataTilKlageUtfall(skjema: FilledFormDataVurdering): KlageUtfallUtenBrev {
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
