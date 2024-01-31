import { BodyShort, Button, Heading, Radio, RadioGroup, Select, Textarea } from '@navikt/ds-react'
import React from 'react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Feilmelding, Innhold, VurderingWrapper } from '~components/klage/styled'
import { useNavigate } from 'react-router-dom'
import { useKlage } from '~components/klage/useKlage'
import {
  AARSAKER_OMGJOERING,
  InnstillingTilKabalUtenBrev,
  Klage,
  KlageUtfallUtenBrev,
  LOVHJEMLER_BP,
  LOVHJEMLER_OMS,
  Omgjoering,
  TEKSTER_AARSAK_OMGJOERING,
  TEKSTER_LOVHJEMLER,
  Utfall,
} from '~shared/types/Klage'
import { Control, Controller, useForm } from 'react-hook-form'
import { FieldOrNull } from '~shared/types/util'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterUtfallForKlage } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { SakType } from '~shared/types/sak'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { forrigeSteg } from '~components/klage/stegmeny/KlageStegmeny'
import Spinner from '~shared/Spinner'

type FilledFormDataVurdering = {
  utfall: Utfall
  omgjoering?: Omgjoering
  innstilling?: InnstillingTilKabalUtenBrev
}

type FormdataVurdering = FieldOrNull<FilledFormDataVurdering>

function erSkjemaUtfylt(skjema: FormdataVurdering): skjema is FilledFormDataVurdering {
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
    if (!innstilling || !innstilling.lovhjemmel || !innstilling.tekst) {
      return false
    }
  }
  return true
}

function mapFraFormdataTilKlageUtfall(skjema: FilledFormDataVurdering): KlageUtfallUtenBrev {
  switch (skjema.utfall) {
    case Utfall.DELVIS_OMGJOERING:
      return { utfall: 'DELVIS_OMGJOERING', omgjoering: skjema.omgjoering!!, innstilling: skjema.innstilling!! }
    case Utfall.OMGJOERING:
      return { utfall: 'OMGJOERING', omgjoering: skjema.omgjoering!! }
    case Utfall.STADFESTE_VEDTAK:
      return { utfall: 'STADFESTE_VEDTAK', innstilling: skjema.innstilling!! }
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
  }
}

export function KlageVurderingRedigering() {
  const navigate = useNavigate()
  const klage = useKlage()
  const [lagreUtfallStatus, lagreUtfall] = useApiCall(oppdaterUtfallForKlage)
  const dispatch = useAppDispatch()

  if (!klage) {
    return <Spinner visible label="Henter klage" />
  }

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

function KlageOmgjoering(props: { control: Control<FormdataVurdering> }) {
  const { control } = props

  return (
    <>
      <Heading level="2" size="medium">
        Omgjøring
      </Heading>

      <VurderingWrapper>
        <Controller
          rules={{
            required: true,
            minLength: 1,
          }}
          name="omgjoering.grunnForOmgjoering"
          control={control}
          render={({ field, fieldState }) => {
            const { value, ...rest } = field
            return (
              <>
                <Select label="Hvorfor skal saken omgjøres?" value={value ?? ''} {...rest}>
                  <option value="">Velg grunn</option>
                  {AARSAKER_OMGJOERING.map((aarsak) => (
                    <option key={aarsak} value={aarsak}>
                      {TEKSTER_AARSAK_OMGJOERING[aarsak]}
                    </option>
                  ))}
                </Select>
                {fieldState.error ? <Feilmelding>Du må velge en årsak for omgjøringen.</Feilmelding> : null}
              </>
            )
          }}
        />
      </VurderingWrapper>

      <VurderingWrapper>
        <Controller
          rules={{
            required: true,
            minLength: 1,
          }}
          name="omgjoering.begrunnelse"
          control={control}
          render={({ field, fieldState }) => {
            const { value, ...rest } = field
            return (
              <>
                <Textarea label="Begrunnelse" value={value ?? ''} {...rest} />

                {fieldState.error ? <Feilmelding>Du må gi en begrunnelse for omgjøringen.</Feilmelding> : null}
              </>
            )
          }}
        />
      </VurderingWrapper>
    </>
  )
}

function KlageInnstilling(props: { control: Control<FormdataVurdering> }) {
  const { control } = props

  const klage = useKlage()
  const aktuelleHjemler = klage?.sak.sakType === SakType.BARNEPENSJON ? LOVHJEMLER_BP : LOVHJEMLER_OMS

  return (
    <>
      <Heading level="2" size="medium">
        Innstilling til KA
      </Heading>

      <BodyShort spacing>Angi hvilken hjemmel klagen hovedsakelig knytter seg til</BodyShort>
      <VurderingWrapper>
        <Controller
          rules={{
            required: true,
            minLength: 1,
          }}
          name="innstilling.lovhjemmel"
          control={control}
          render={({ field, fieldState }) => {
            const { value, ...rest } = field
            return (
              <>
                <Select label="Hjemmel" value={value ?? ''} {...rest}>
                  <option value="">Velg hjemmel</option>
                  {aktuelleHjemler.map((hjemmel) => (
                    <option key={hjemmel} value={hjemmel}>
                      {TEKSTER_LOVHJEMLER[hjemmel]}
                    </option>
                  ))}
                </Select>
                {fieldState.error ? (
                  <Feilmelding>Du må angi hjemmelen klagen hovedsakelig knytter seg til.</Feilmelding>
                ) : null}
              </>
            )
          }}
        />
      </VurderingWrapper>

      <BodyShort spacing>Skriv innstilling til klagen, som blir med i brev til KA og bruker</BodyShort>
      <VurderingWrapper>
        <Controller
          rules={{
            required: true,
            minLength: 1,
          }}
          name="innstilling.tekst"
          control={control}
          render={({ field, fieldState }) => {
            const { value, ...rest } = field
            return (
              <>
                <Textarea label="Innstilling til KA" value={value ?? ''} {...rest} />
                {fieldState.error ? <Feilmelding>Du må skrive en innstillingstekst.</Feilmelding> : null}
              </>
            )
          }}
        />
      </VurderingWrapper>
    </>
  )
}
