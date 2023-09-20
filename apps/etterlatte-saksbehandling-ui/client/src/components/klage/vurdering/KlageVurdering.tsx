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
  LOVHJEMLER_KLAGE,
  Omgjoering,
  TEKSTER_AARSAK_OMGJOERING,
  Utfall,
} from '~shared/types/Klage'
import { Control, Controller, useForm } from 'react-hook-form'
import { FieldOrNull } from '~shared/types/util'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterUtfallForKlage } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { ApiErrorAlert } from '~ErrorBoundary'

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

export function KlageVurdering() {
  const navigate = useNavigate()
  const klage = useKlage()
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
      navigate(`/klage/${klage.id}/oppsummering`)
    }

    const utfall = mapFraFormdataTilKlageUtfall(skjema)
    lagreUtfall({ klageId: klage.id, utfall }, (oppdatertKlage) => {
      dispatch(addKlage(oppdatertKlage))
      navigate(`/klage/${klage.id}/oppsummering`)
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

        {isFailure(lagreUtfallStatus) ? (
          <ApiErrorAlert>
            Kunne ikke lagre utfallet av klagen. Prøv igjen senere, og meld sak hvis problemet vedvarer.
          </ApiErrorAlert>
        ) : null}

        <FlexRow justify={'center'}>
          <Button type="button" variant="secondary" onClick={() => navigate(`/klage/${klage?.id}/formkrav`)}>
            Gå tilbake
          </Button>
          <Button loading={isPending(lagreUtfallStatus)} type="submit" variant="primary">
            Send inn vurdering av klagen
          </Button>
        </FlexRow>
      </form>
    </Content>
  )
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
                  {LOVHJEMLER_KLAGE.map((hjemmel) => (
                    <option key={hjemmel} value={hjemmel}>
                      {hjemmel}
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
