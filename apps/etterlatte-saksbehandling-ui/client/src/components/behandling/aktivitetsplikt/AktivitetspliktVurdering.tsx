import { BodyShort, Box, Button, Heading, HStack, Label, Radio, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktUnntakType,
  IAktivitetspliktVurderingNyDto,
} from '~shared/types/Aktivitetsplikt'
import { hentAktivitspliktVurderingForBehandling, redigerUnntakForBehandling } from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useDispatch } from 'react-redux'
import { isPending } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { AktivitetsgradOgUnntakTabellBehandling } from '~components/behandling/aktivitetsplikt/aktivitetsgrad/AktivitetsgradOgUnntakTabellBehandling'
import {
  setVurderingBehandling,
  useAktivitetspliktBehandlingState,
} from '~store/reducers/AktivitetspliktBehandlingReducer'
import { VurderAktivitetspliktWrapperBehandling } from '~components/behandling/aktivitetsplikt/VurderAktivitetspliktWrapperBehandling'
import { isBefore, subMonths } from 'date-fns'
import { JaNei } from '~shared/types/ISvar'
import { HjemmelLenke } from '~components/behandling/felles/HjemmelLenke'
import { useAppDispatch } from '~store/Store'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'

const vurderingHarInnhold = (vurdering: IAktivitetspliktVurderingNyDto): boolean => {
  return !!vurdering.unntak.length || !!vurdering.aktivitet.length
}

const harVarigUnntak = (vurdering: IAktivitetspliktVurderingNyDto): boolean => {
  return (
    !!vurdering.unntak.length &&
    !!vurdering.unntak.find(
      (unntak) => unntak.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
    )
  )
}

const formvaluesFraVurdering = (vurdering: IAktivitetspliktVurderingNyDto): Partial<HarBrukerVarigUnntakFormdata> => ({
  harAktivitetsplikt: harAktivitetspliktFraVurdering(vurdering),
  beskrivelse: vurdering.unntak.find(
    (u) => u.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
  )?.beskrivelse,
})

function harAktivitetspliktFraVurdering(vurdering: IAktivitetspliktVurderingNyDto): JaNei | undefined {
  if (!vurderingHarInnhold(vurdering)) {
    return undefined
  }
  if (harVarigUnntak(vurdering)) {
    return JaNei.NEI
  } else {
    return JaNei.JA
  }
}

interface HarBrukerVarigUnntakFormdata {
  harAktivitetsplikt: JaNei
  beskrivelse: string
}

function HarBrukerVarigUnntak(props: { behandling: IDetaljertBehandling; doedsdato: Date; redigerbar: boolean }) {
  const { behandling, redigerbar, doedsdato } = props
  const dispatch = useAppDispatch()
  const vurdering = useAktivitetspliktBehandlingState()
  const [lagreUnntakVarigStatus, lagreUnntakVarig] = useApiCall(redigerUnntakForBehandling)
  const [redigerer, setRedigerer] = useState<boolean>(!harAktivitetspliktFraVurdering(vurdering))

  const {
    handleSubmit,
    register,
    watch,
    control,
    reset,
    formState: { errors },
  } = useForm<HarBrukerVarigUnntakFormdata>({
    defaultValues: formvaluesFraVurdering(vurdering),
  })

  useEffect(() => {
    setRedigerer(!harAktivitetspliktFraVurdering(vurdering))
    reset(formvaluesFraVurdering(vurdering))
  }, [vurdering])

  function avbrytRedigering() {
    reset(formvaluesFraVurdering(vurdering))
    setRedigerer(false)
  }

  const jaHarAktivitetsplikt = watch('harAktivitetsplikt') === JaNei.JA

  const lagreVarigUnntak = (formdata: HarBrukerVarigUnntakFormdata) => {
    if (formdata.harAktivitetsplikt === JaNei.NEI) {
      lagreUnntakVarig(
        {
          sakId: behandling.sakId,
          behandlingId: behandling.id,
          request: {
            id: undefined,
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
                <Button
                  variant="secondary"
                  size="small"
                  disabled={isPending(lagreUnntakVarigStatus)}
                  onClick={avbrytRedigering}
                >
                  Avbryt
                </Button>
                <Button loading={isPending(lagreUnntakVarigStatus)} variant="primary" type="submit" size="small">
                  Lagre
                </Button>
              </HStack>
            </>
          )}
        </VStack>
      </form>

      {jaHarAktivitetsplikt &&
        (vurderingHarInnhold(vurdering) ? (
          <HStack gap="4">
            <Button variant="secondary" size="small" onClick={avbrytRedigering}>
              Avbryt
            </Button>
            <Button size="small" variant="primary" onClick={avbrytRedigering}>
              Lagre
            </Button>
          </HStack>
        ) : (
          <VurderAktivitetspliktWrapperBehandling
            doedsdato={doedsdato}
            behandling={behandling}
            defaultOpen={true}
            varigeUnntak={vurdering.unntak.filter(
              (unntak) => unntak.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
            )}
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

export const AktivitetspliktVurdering = ({
  behandling,
  setManglerAktivitetspliktVurdering,
  doedsdato,
}: {
  behandling: IDetaljertBehandling
  setManglerAktivitetspliktVurdering: (manglerVurdering: boolean) => void
  doedsdato: Date
}) => {
  const [hentetVurdering, hentVurdering] = useApiCall(hentAktivitspliktVurderingForBehandling)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const dispatch = useDispatch()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const vurdering = useAktivitetspliktBehandlingState()
  useEffect(() => {
    hentVurdering({ sakId: behandling.sakId, behandlingId: behandling.id }, (result) => {
      dispatch(setVurderingBehandling(result))
    })
  }, [behandling.id])
  useEffect(() => {
    setManglerAktivitetspliktVurdering(!vurderingHarInnhold(vurdering))
  }, [vurdering])

  if (isPending(hentetVurdering)) {
    return <Spinner label="Henter aktivitetspliktsvurdering" />
  }

  const typeVurdering6eller12MndVurdering = typeVurderingFraDoedsdato(doedsdato)
  return (
    <Box maxWidth="120rem" paddingBlock="4 0" borderWidth="1 0 0 0">
      <VStack gap="6">
        <VStack gap="3">
          <Heading size="medium" level="2">
            Vurdering av aktivitetsplikt
          </Heading>
          <HjemmelLenke
            tittel="Folketrygdloven § 17-7"
            lenke="https://lovdata.no/pro/#document/NL/lov/1997-02-28-19/%C2%A717-7"
          />
          {!!vurdering && (
            <HarBrukerVarigUnntak behandling={behandling} doedsdato={doedsdato} redigerbar={redigerbar} />
          )}
        </VStack>

        {vurdering &&
          !harVarigUnntak(vurdering!!) &&
          (vurdering.aktivitet.length > 0 || vurdering.unntak.length > 0) && (
            <>
              <AktivitetsgradOgUnntakTabellBehandling
                unntak={vurdering.unntak}
                aktiviteter={vurdering.aktivitet}
                behandling={behandling}
                typeVurdering={typeVurdering6eller12MndVurdering}
              />
              {redigerbar && (
                <VurderAktivitetspliktWrapperBehandling
                  doedsdato={doedsdato}
                  behandling={behandling}
                  defaultOpen={false}
                  varigeUnntak={vurdering.unntak.filter(
                    (unntak) => unntak.unntak === AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                  )}
                />
              )}
            </>
          )}
      </VStack>
    </Box>
  )
}

export function typeVurderingFraDoedsdato(doedsdato: Date): AktivitetspliktOppgaveVurderingType {
  if (isBefore(doedsdato, subMonths(Date.now(), 10))) {
    return AktivitetspliktOppgaveVurderingType.TOLV_MAANEDER
  } else {
    return AktivitetspliktOppgaveVurderingType.SEKS_MAANEDER
  }
}
