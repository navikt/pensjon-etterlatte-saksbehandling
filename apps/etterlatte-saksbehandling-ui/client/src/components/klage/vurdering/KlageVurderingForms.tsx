import {
  AARSAKER_OMGJOERING,
  InnstillingTilKabalUtenBrev,
  LOVHJEMLER_BP,
  LOVHJEMLER_OMS,
  Omgjoering,
  TEKSTER_AARSAK_OMGJOERING,
  TEKSTER_LOVHJEMLER,
  Utfall,
} from '~shared/types/Klage'
import React from 'react'
import { Heading, Select, Textarea } from '@navikt/ds-react'
import { FieldOrNull } from '~shared/types/util'
import { Control, Controller } from 'react-hook-form'
import { Feilmelding, VurderingWrapper } from '~components/klage/styled'
import { useKlage } from '~components/klage/useKlage'
import { SakType } from '~shared/types/sak'

export type FilledFormDataVurdering = {
  utfall: Utfall
  omgjoering?: Omgjoering
  innstilling?: InnstillingTilKabalUtenBrev
}

export type FormdataVurdering = FieldOrNull<FilledFormDataVurdering>

export function erSkjemaUtfylt(skjema: FormdataVurdering): skjema is FilledFormDataVurdering {
  if (skjema.utfall === null) {
    return false
  }
  if (
    skjema.utfall === Utfall.OMGJOERING ||
    skjema.utfall === Utfall.DELVIS_OMGJOERING ||
    skjema.utfall === Utfall.AVVIST_MED_OMGJOERING
  ) {
    const { omgjoering } = skjema
    if (!omgjoering || !omgjoering.begrunnelse || !omgjoering.grunnForOmgjoering) {
      return false
    }
  }
  if (skjema.utfall === Utfall.STADFESTE_VEDTAK || skjema.utfall === Utfall.DELVIS_OMGJOERING) {
    const { innstilling } = skjema
    if (!innstilling || !innstilling.lovhjemmel) {
      return false
    }
  }
  return true
}

export function KlageOmgjoering(props: { control: Control<FormdataVurdering> }) {
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

export function KlageInnstilling(props: { control: Control<FormdataVurdering> }) {
  const { control } = props

  const klage = useKlage()
  const aktuelleHjemler = klage?.sak.sakType === SakType.BARNEPENSJON ? LOVHJEMLER_BP : LOVHJEMLER_OMS

  return (
    <>
      <Heading level="2" size="medium" spacing>
        Innstilling til KA
      </Heading>

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
                <Select
                  label="Hjemmel"
                  value={value ?? ''}
                  {...rest}
                  description="Angi hvilken hjemmel klagen hovedsakelig knytter seg til"
                >
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

      <VurderingWrapper>
        <Controller
          name="innstilling.internKommentar"
          control={control}
          render={({ field }) => {
            const { value, ...rest } = field
            return (
              <>
                <Textarea label="Intern kommentar til KA" value={value ?? ''} {...rest} />
              </>
            )
          }}
        />
      </VurderingWrapper>
    </>
  )
}
