import { Button, Heading, Radio, RadioGroup, Select, Textarea } from '@navikt/ds-react'
import React from 'react'
import { FlexRow } from '~shared/styled'
import { BredVurderingWrapper, Feilmelding, VurderingWrapper } from '~components/klage/styled'
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
  teksterKlageutfall,
  Utfall,
} from '~shared/types/Klage'
import { Control, Controller, useForm, UseFormRegister } from 'react-hook-form'
import { FieldOrNull } from '~shared/types/util'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterUtfallForKlage } from '~shared/api/klage'
import { useAppDispatch } from '~store/Store'
import { addKlage } from '~store/reducers/KlageReducer'
import { SakType } from '~shared/types/sak'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { forrigeSteg, kanSeBrev } from '~components/klage/stegmeny/KlageStegmeny'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

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
    if (!innstilling || !innstilling.lovhjemmel) {
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

export function EndeligVurdering(props: { klage: Klage }) {
  const klage = props.klage

  const navigate = useNavigate()
  const [lagreUtfallStatus, lagreUtfall] = useApiCall(oppdaterUtfallForKlage)
  const dispatch = useAppDispatch()
  const stoetterDelvisOmgjoering = useFeatureEnabledMedDefault('pensjon-etterlatte.klage-delvis-omgjoering', false)

  const {
    control,
    handleSubmit,
    register,
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
    <>
      <Heading level="2" size="medium" spacing>
        Endelig vurdering
      </Heading>

      <form onSubmit={handleSubmit(sendInnVurdering)}>
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
                  <Radio value={Utfall.OMGJOERING}> {teksterKlageutfall[Utfall.OMGJOERING]}</Radio>
                  {stoetterDelvisOmgjoering && (
                    <Radio value={Utfall.DELVIS_OMGJOERING}>{teksterKlageutfall[Utfall.DELVIS_OMGJOERING]}</Radio>
                  )}
                  <Radio value={Utfall.STADFESTE_VEDTAK}> {teksterKlageutfall[Utfall.STADFESTE_VEDTAK]}</Radio>
                </RadioGroup>
                {fieldState.error ? <Feilmelding>Du må velge et utfall for klagen.</Feilmelding> : null}
              </>
            )}
          />
        </VurderingWrapper>

        {valgtUtfall === Utfall.STADFESTE_VEDTAK || valgtUtfall === Utfall.DELVIS_OMGJOERING ? (
          <KlageInnstilling control={control} register={register} />
        ) : null}

        {valgtUtfall === Utfall.OMGJOERING || valgtUtfall === Utfall.DELVIS_OMGJOERING ? (
          <KlageOmgjoering control={control} register={register} />
        ) : null}

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
    </>
  )
}

function nesteSteg(valgtUtfall: Utfall | null, klageId: string) {
  return kanSeBrev(valgtUtfall) ? `/klage/${klageId}/brev` : `/klage/${klageId}/oppsummering`
}

function KlageOmgjoering(props: { control: Control<FormdataVurdering>; register: UseFormRegister<FormdataVurdering> }) {
  const { control, register } = props

  return (
    <>
      <Heading level="3" size="medium" spacing>
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

      <BredVurderingWrapper>
        <Textarea
          {...register('omgjoering.begrunnelse', {
            required: {
              value: true,
              message: 'Du må gi en begrunnelse for omgjøringen',
            },
          })}
          label="Begrunnelse"
        />
      </BredVurderingWrapper>
    </>
  )
}

function KlageInnstilling(props: {
  control: Control<FormdataVurdering>
  register: UseFormRegister<FormdataVurdering>
}) {
  const { control, register } = props

  const klage = useKlage()
  const aktuelleHjemler = klage?.sak.sakType === SakType.BARNEPENSJON ? LOVHJEMLER_BP : LOVHJEMLER_OMS

  return (
    <>
      <Heading level="3" size="medium" spacing>
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
                  description="Velg hvilken hjemmel klagen knytter seg til"
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

      <BredVurderingWrapper>
        <Textarea
          {...register('innstilling.innstillingTekst', {
            required: {
              value: true,
              message: 'Du må skrive en innstillingstekst som begrunner hvorfor klagen står seg.',
            },
          })}
          label="Innstilling"
          description="Innstillingen blir med i brev til klager og til KA"
        />
      </BredVurderingWrapper>

      <BredVurderingWrapper>
        <Textarea
          {...register('innstilling.internKommentar')}
          label="Intern kommentar til KA"
          description="Kommentaren blir ikke synlig for bruker"
        />
      </BredVurderingWrapper>
    </>
  )
}
