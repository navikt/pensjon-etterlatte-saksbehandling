import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useAppDispatch } from '~store/Store'
import {
  setVurderingBehandling,
  useAktivitetspliktBehandlingState,
} from '~store/reducers/AktivitetspliktBehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { redigerUnntakForBehandling, slettUnntakForBehandling } from '~shared/api/aktivitetsplikt'
import React, { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import {
  AktivitetspliktUnntakType,
  IAktivitetspliktUnntak,
  IAktivitetspliktVurderingNyDto,
} from '~shared/types/Aktivitetsplikt'
import { JaNei } from '~shared/types/ISvar'
import { BodyShort, Box, Button, HStack, Label, Radio, Textarea, VStack } from '@navikt/ds-react'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { isPending } from '~shared/api/apiUtils'
import { VurderAktivitetspliktWrapperBehandling } from '~components/behandling/aktivitetsplikt/VurderAktivitetspliktWrapperBehandling'
import { finnVarigUnntak, vurderingHarInnhold } from '~components/behandling/aktivitetsplikt/AktivitetspliktVurdering'

interface HarBrukerVarigUnntakFormdata {
  harAktivitetsplikt: JaNei
  beskrivelse: string
}
const vurderingHarInnholdUnntattVarigUnntak = (vurdering: IAktivitetspliktVurderingNyDto) => {
  return (
    vurdering.aktivitet.length > 0 ||
    vurdering.unntak.filter(
      (unntak) => unntak.unntak !== AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
    ).length > 0
  )
}

function harAktivitetspliktFraVurdering(
  vurdering: IAktivitetspliktVurderingNyDto,
  varigUnntak?: IAktivitetspliktUnntak
): JaNei | undefined {
  if (!vurderingHarInnhold(vurdering)) {
    return undefined
  }
  if (varigUnntak) {
    return JaNei.NEI
  } else {
    return JaNei.JA
  }
}

const formvaluesFraVurdering = (
  vurdering: IAktivitetspliktVurderingNyDto,
  varigUnntak?: IAktivitetspliktUnntak
): Partial<HarBrukerVarigUnntakFormdata> => ({
  harAktivitetsplikt: harAktivitetspliktFraVurdering(vurdering, varigUnntak),
  beskrivelse: vurdering.unntak.find(
    (u) => u.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
  )?.beskrivelse,
})

export function HarBrukerVarigUnntak(props: {
  behandling: IDetaljertBehandling
  doedsdato: Date
  redigerbar: boolean
}) {
  const { behandling, redigerbar, doedsdato } = props
  const dispatch = useAppDispatch()
  const vurdering = useAktivitetspliktBehandlingState()
  const [lagreUnntakVarigStatus, lagreUnntakVarig] = useApiCall(redigerUnntakForBehandling)
  const [redigerer, setRedigerer] = useState<boolean>(!harAktivitetspliktFraVurdering(vurdering))
  const [slettStatus, slettApi] = useApiCall(slettUnntakForBehandling)

  const eksisterendeVarigUnntak = finnVarigUnntak(vurdering)
  const {
    handleSubmit,
    register,
    watch,
    control,
    reset,
    formState: { errors },
  } = useForm<HarBrukerVarigUnntakFormdata>({
    defaultValues: formvaluesFraVurdering(vurdering, eksisterendeVarigUnntak),
  })

  useEffect(() => {
    setRedigerer(!harAktivitetspliktFraVurdering(vurdering, eksisterendeVarigUnntak))
    reset(formvaluesFraVurdering(vurdering, eksisterendeVarigUnntak))
  }, [vurdering, eksisterendeVarigUnntak])

  function avbrytRedigering(nyVurdering?: IAktivitetspliktVurderingNyDto) {
    reset(formvaluesFraVurdering(nyVurdering ?? vurdering))
    if (nyVurdering) {
      dispatch(setVurderingBehandling(nyVurdering))
    }
    setRedigerer(false)
  }

  function slettVarigUnntakHvisFins() {
    const unntakSomSkalSlettes = eksisterendeVarigUnntak
    if (unntakSomSkalSlettes) {
      // slett det først
      slettApi(
        {
          sakId: behandling.sakId,
          behandlingId: behandling.id,
          unntakId: unntakSomSkalSlettes.id,
        },
        (nyState) => avbrytRedigering(nyState)
      )
    } else {
      // Vi har ikke lagret et varig unntak og vi kan bare oppdatere state
      avbrytRedigering()
    }
  }

  const jaHarAktivitetsplikt = watch('harAktivitetsplikt') === JaNei.JA

  const lagreVarigUnntak = (formdata: HarBrukerVarigUnntakFormdata) => {
    if (formdata.harAktivitetsplikt === JaNei.NEI) {
      lagreUnntakVarig(
        {
          sakId: behandling.sakId,
          behandlingId: behandling.id,
          request: {
            id: eksisterendeVarigUnntak?.id,
            unntak: AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT,
            fom: new Date().toISOString(),
            tom: undefined,
            beskrivelse: formdata.beskrivelse,
          },
        },
        (data) => {
          dispatch(setVurderingBehandling(data))
          avbrytRedigering()
        }
      )
    }
  }

  return redigerbar && redigerer ? (
    <>
      <form onSubmit={handleSubmit(lagreVarigUnntak)}>
        <VStack maxWidth="32.5rem" gap="4">
          <ControlledRadioGruppe
            control={control}
            name="harAktivitetsplikt"
            legend="Har bruker aktivitetsplikt etter overgangsperioden?"
            description={
              <BodyShort>
                Vurder om bruker fra 6 måneder etter dødsfallet må være i aktivitet, eller har varig unntak.
              </BodyShort>
            }
            radios={
              <>
                <Radio size="small" value={JaNei.JA}>
                  Ja, bruker har aktivitetsplikt
                </Radio>
                <Radio size="small" value={JaNei.NEI}>
                  Nei, bruker er født i 1963 eller tidligere og har lav inntekt
                </Radio>
              </>
            }
            errorVedTomInput="Du må velge om bruker har aktivitetsplikt"
          />
          {watch('harAktivitetsplikt') === JaNei.NEI && (
            <>
              <Box maxWidth="60rem" paddingBlock="2 2">
                <Textarea
                  {...register('beskrivelse', {
                    required: {
                      value: true,
                      message: 'Du må beskrive hvorfor bruker er unntatt fra aktivitetsplikten',
                    },
                  })}
                  label="Beskrivelse"
                  description="Beskriv hvorfor bruker er unntatt fra aktivitetsplikten"
                  error={errors.beskrivelse?.message}
                />
              </Box>
              <HStack gap="4">
                <Button loading={isPending(lagreUnntakVarigStatus)} variant="primary" type="submit" size="small">
                  Lagre
                </Button>
                <Button
                  variant="secondary"
                  size="small"
                  disabled={isPending(lagreUnntakVarigStatus)}
                  onClick={() => avbrytRedigering()}
                >
                  Avbryt
                </Button>
              </HStack>
            </>
          )}
        </VStack>
      </form>

      {jaHarAktivitetsplikt &&
        (vurderingHarInnholdUnntattVarigUnntak(vurdering) ? (
          <HStack gap="4">
            <Button size="small" variant="primary" disabled={isPending(slettStatus)} onClick={slettVarigUnntakHvisFins}>
              Lagre
            </Button>
            <Button
              variant="secondary"
              size="small"
              loading={isPending(slettStatus)}
              onClick={() => avbrytRedigering()}
            >
              Avbryt
            </Button>
          </HStack>
        ) : (
          <VurderAktivitetspliktWrapperBehandling
            doedsdato={doedsdato}
            behandling={behandling}
            defaultOpen={true}
            varigUnntak={eksisterendeVarigUnntak}
          />
        ))}
    </>
  ) : (
    <VStack gap="4" maxWidth="32.5rem">
      <div>
        <Label size="medium">Har bruker aktivitetsplikt?</Label>
        {jaHarAktivitetsplikt ? (
          <BodyShort>Ja</BodyShort>
        ) : (
          <BodyShort>Nei, bruker er født i 1963 eller tidligere og har lav inntekt</BodyShort>
        )}
      </div>
      {watch('harAktivitetsplikt') === JaNei.NEI && (
        <div>
          <Label>Begrunnelse</Label>
          <BodyShort>{watch('beskrivelse')}</BodyShort>
        </div>
      )}
      {redigerbar && (
        <div>
          <Button variant="secondary" size="small" onClick={() => setRedigerer(true)}>
            Rediger
          </Button>
        </div>
      )}
    </VStack>
  )
}
