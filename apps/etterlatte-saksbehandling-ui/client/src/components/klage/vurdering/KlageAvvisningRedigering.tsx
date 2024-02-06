import { Alert, BodyLong, Button, Heading, HelpText, Radio, RadioGroup } from '@navikt/ds-react'
import React from 'react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Feilmelding, Innhold, VurderingWrapper } from '~components/klage/styled'
import { useNavigate } from 'react-router-dom'
import { InnstillingTilKabalUtenBrev, Klage, KlageUtfallUtenBrev, Omgjoering, Utfall } from '~shared/types/Klage'
import { Controller, useForm } from 'react-hook-form'
import { FieldOrNull } from '~shared/types/util'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterUtfallForKlage } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { forrigeSteg } from '~components/klage/stegmeny/KlageStegmeny'
import { erSkjemaUtfylt, KlageOmgjoering } from '~components/klage/vurdering/KlageVurderingForms'
import styled from 'styled-components'

type FilledFormDataVurdering = {
  utfall: Utfall
  omgjoering?: Omgjoering
  innstilling?: InnstillingTilKabalUtenBrev
}

type FormdataVurdering = FieldOrNull<FilledFormDataVurdering>

export function KlageAvvisningRedigering(props: { klage: Klage }) {
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
            Avvis klagen
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <form onSubmit={handleSubmit(sendInnVurdering)}>
        <Innhold>
          <BodyLong spacing={true}>
            Siden klagefristen ikke er overholdt må klagen formelt avvises, men du kan likevel bestemme at vedtaket skal
            omgjøres.
          </BodyLong>
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
                    <Radio value={Utfall.AVVIST_MED_VEDTAKSBREV}>Vedtak om avvisning</Radio>
                    <Radio value={Utfall.AVVIST_MED_OMGJOERING}>Avvist med omgjøring</Radio>
                  </RadioGroup>
                  {fieldState.error ? <Feilmelding>Du må velge et utfall for klagen.</Feilmelding> : null}
                </>
              )}
            />
          </VurderingWrapper>

          {valgtUtfall === Utfall.AVVIST_MED_OMGJOERING ? <KlageOmgjoering control={control} /> : null}
          {valgtUtfall === Utfall.AVVIST_MED_VEDTAKSBREV ? (
            <InfoAlert variant="info" inline>
              Det skal fattes et vedtak om avvisning. Gjenny støtter ikke dette ennå, men det kommer snart.
            </InfoAlert>
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
          {/*TODO Enable når lagring er på plass*/}
          <Button disabled={true} loading={isPending(lagreUtfallStatus)} type="submit" variant="primary">
            {kanSeBrev(valgtUtfall) ? 'Gå til brev' : 'Gå til oppsummering'}
          </Button>
          <HelpText>Blir aktivert når lagring av utfall for avvisning er støttet</HelpText>
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

const InfoAlert = styled(Alert).attrs({ variant: 'info' })`
  margin-top: 2rem;
  margin-bottom: 2rem;
`

function mapFraFormdataTilKlageUtfall(skjema: FilledFormDataVurdering): KlageUtfallUtenBrev {
  switch (skjema.utfall) {
    case Utfall.AVVIST_MED_OMGJOERING:
      return { utfall: 'AVVIST_MED_OMGJOERING', omgjoering: skjema.omgjoering!! }
    case Utfall.AVVIST_MED_VEDTAKSBREV:
      return { utfall: 'AVVIST_MED_VEDTAKSBREV' }
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
    case 'AVVIST_MED_VEDTAKSBREV':
      return { utfall: Utfall.AVVIST_MED_VEDTAKSBREV }
    default:
      return { utfall: null }
  }
}
