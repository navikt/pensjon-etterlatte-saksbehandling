import { BodyShort, Box, Button, Heading, HStack, Radio, RadioGroup, Textarea, VStack } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  AktivitetspliktOppgaveVurderingType,
  AktivitetspliktUnntakType,
  IAktivitetspliktVurderingNyDto,
  tekstAktivitetspliktUnntakType,
} from '~shared/types/Aktivitetsplikt'
import { hentAktivitspliktVurderingForBehandling, redigerUnntakForBehandling } from '~shared/api/aktivitetsplikt'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useDispatch } from 'react-redux'
import { isPending, isSuccess } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { UnntakTabellBehandling } from '~components/behandling/aktivitetsplikt/unntak/UnntakTabellBehandling'
import { AktivitetsgradTabellBehandling } from '~components/behandling/aktivitetsplikt/aktivitetsgrad/AktivitetsgradTabellBehandling'
import {
  setVurderingBehandling,
  useAktivitetspliktBehandlingState,
} from '~store/reducers/AktivitetspliktBehandlingReducer'
import { VurderAktivitetspliktWrapperBehandling } from '~components/behandling/aktivitetsplikt/VurderAktivitetspliktWrapperBehandling'
import { isBefore, subMonths } from 'date-fns'
import { JaNei } from '~shared/types/ISvar'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'

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

export const AktivitetspliktVurdering = ({
  behandling,
  resetManglerAktivitetspliktVurdering,
  doedsdato,
}: {
  behandling: IDetaljertBehandling
  resetManglerAktivitetspliktVurdering: () => void
  doedsdato: Date
}) => {
  const [vurdering, setVurdering] = useState<IAktivitetspliktVurderingNyDto>()
  const [manglerVurdering, setManglerVurdering] = useState<boolean>(false)
  const [harAktivitetsplikt, setHarAktivitetsplikt] = useState<JaNei | undefined>(undefined)
  const [beskrivelseVarigUnntak, setBeskrivelseVarigUnntak] = useState<string | undefined>()
  const [lagreUnntakVarigStatus, lagreUnntakVarig] = useApiCall(redigerUnntakForBehandling)
  const [hentetVurdering, hentVurdering] = useApiCall(hentAktivitspliktVurderingForBehandling)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const dispatch = useDispatch()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )
  const updated = useAktivitetspliktBehandlingState()
  useEffect(() => {
    if (!vurdering) {
      hentVurdering(
        { sakId: behandling.sakId, behandlingId: behandling.id },
        (result) => {
          dispatch(setVurderingBehandling(result))
          setVurdering(result)
          if (harVarigUnntak(result)) {
            setManglerVurdering(false)
            setHarAktivitetsplikt(JaNei.NEI)
          } else {
            if (vurderingHarInnhold(result)) {
              setManglerVurdering(false)
              setHarAktivitetsplikt(JaNei.JA)
              resetManglerAktivitetspliktVurdering()
            } else {
              setManglerVurdering(true)
            }
          }
        },
        (error) => {
          if (error.status === 404) {
            //NO-OP - error visning?
          }
        }
      )
    }
  }, [])

  useEffect(() => {
    setVurdering(updated)
    if (!vurderingHarInnhold(updated)) {
      setManglerVurdering(true)
    }
  }, [updated])

  const lagreVarigUnntak = () => {
    if (harAktivitetsplikt == JaNei.NEI) {
      lagreUnntakVarig(
        {
          sakId: behandling.sakId,
          behandlingId: behandling.id,
          request: {
            id: undefined,
            unntak: AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT,
            fom: new Date().toISOString(),
            tom: undefined,
            beskrivelse: beskrivelseVarigUnntak || '',
          },
        },
        (data) => {
          dispatch(setVurderingBehandling(data))
        }
      )
    } else {
      setManglerVurdering(false)
    }
  }
  const valgHarAktivitetsplikt = harAktivitetsplikt == JaNei.JA

  if (isPending(hentetVurdering)) {
    return <Spinner label="Henter aktivitetspliktsvurdering" />
  }

  console.log('valgHarAktivitetsplikt: ', valgHarAktivitetsplikt, ' harAktivitetsplikt: ', harAktivitetsplikt)

  return (
    <Box maxWidth="120rem" paddingBlock="4 0" borderWidth="1 0 0 0">
      <VStack gap="6">
        <VStack gap="3">
          <Heading size="medium">Vurdering av aktivitetsplikt</Heading>
          <BodyShort>Følgende vurderinger av aktiviteten er registrert.</BodyShort>
          {manglerVurdering && redigerbar && (
            <Box maxWidth="32rem">
              <RadioGroupWrapper>
                <RadioGroup
                  disabled={!redigerbar}
                  legend="Har bruker aktivitetsplikt"
                  size="small"
                  className="radioGroup"
                  onChange={(event) => setHarAktivitetsplikt(event as JaNei)}
                >
                  <HStack gap="4" wrap={false} justify="space-between">
                    <Radio size="small" value={JaNei.JA}>
                      Ja
                    </Radio>
                    <Radio size="small" value={JaNei.NEI}>
                      {
                        tekstAktivitetspliktUnntakType[
                          AktivitetspliktUnntakType.FOEDT_1963_ELLER_TIDLIGERE_OG_LAV_INNTEKT
                        ]
                      }
                    </Radio>
                  </HStack>
                </RadioGroup>
              </RadioGroupWrapper>
              {harAktivitetsplikt === JaNei.NEI && (
                <>
                  <Box maxWidth="60rem" paddingBlock="2 2">
                    <Textarea
                      label="Beskrivelse"
                      description="Beskriv hvordan du har vurdert brukers situasjon"
                      onChange={(event) => setBeskrivelseVarigUnntak(event.target.value)}
                    />
                  </Box>
                  <Button
                    loading={isPending(lagreUnntakVarigStatus)}
                    variant="primary"
                    type="button"
                    size="small"
                    onClick={lagreVarigUnntak}
                  >
                    Lagre varig unntak
                  </Button>
                </>
              )}
            </Box>
          )}
          {isSuccess(hentetVurdering) && vurdering && harVarigUnntak(vurdering) && (
            <UnntakTabellBehandling behandling={behandling} unntak={vurdering?.unntak} />
          )}
        </VStack>
        {valgHarAktivitetsplikt && isSuccess(hentetVurdering) && (
          <>
            {vurdering && (
              <>
                <AktivitetsgradTabellBehandling
                  behandling={behandling}
                  aktiviteter={vurdering?.aktivitet}
                  typeVurdering={typeVurderingFraDoedsdato(doedsdato)}
                />
                <UnntakTabellBehandling behandling={behandling} unntak={vurdering?.unntak} />
              </>
            )}
            {redigerbar && <VurderAktivitetspliktWrapperBehandling doedsdato={doedsdato} behandling={behandling} />}
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
