import React, { Dispatch, SetStateAction, useState } from 'react'
import styled from 'styled-components'
import { VilkaarsvurderingKnapper } from './VilkaarsvurderingKnapper'
import {
  IVilkaarsvurdering,
  oppdaterTotalVurdering,
  slettTotalVurdering,
  VilkaarsvurderingResultat,
} from '~shared/api/vilkaarsvurdering'
import { BodyShort, Box, Button, Heading, HStack, Radio, RadioGroup, Textarea, VStack } from '@navikt/ds-react'
import { svarTilTotalResultat, totalResultatTilSvar } from './utils'
import { PencilWritingIcon, TrashIcon } from '@navikt/aksel-icons'
import { StatusIcon } from '~shared/icons/statusIcon'
import { formaterDato } from '~utils/formatering/dato'
import { ISvar } from '~shared/types/ISvar'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useAppDispatch } from '~store/Store'
import { oppdaterBehandlingsstatus, updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { isPending } from '~shared/api/apiUtils'
import { OppdatertGrunnlagAlert } from '~components/behandling/trygdetid/Grunnlagopplysninger'
import { formaterSakstype } from '~utils/formatering/formatering'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'

type Props = {
  virkningstidspunktDato: string | undefined
  sakstype: SakType
  vilkaarsvurdering: IVilkaarsvurdering
  oppdaterVilkaar: (vilkaarsvurdering: IVilkaarsvurdering) => void
  behandlingId: string
  redigerbar: boolean
  behandlingstype: IBehandlingsType
  redigerTotalvurdering: boolean
  setRedigerTotalvurdering: Dispatch<SetStateAction<boolean>>
  revurderingsaarsak: Revurderingaarsak | null
}

export const Resultat = (props: Props) => {
  const {
    virkningstidspunktDato,
    sakstype,
    vilkaarsvurdering,
    oppdaterVilkaar,
    behandlingId,
    redigerbar,
    behandlingstype,
    setRedigerTotalvurdering,
    revurderingsaarsak,
  } = props
  const [svar, setSvar] = useState<ISvar>()
  const [radioError, setRadioError] = useState<string>()
  const [kommentar, setKommentar] = useState<string>('')
  const dispatch = useAppDispatch()
  const [totalVurderingStatus, oppdaterTotalVurderingCall] = useApiCall(oppdaterTotalVurdering)
  const [slettVurderingStatus, slettTotalVurderingCall] = useApiCall(slettTotalVurdering)
  const alleVilkaarErVurdert = !vilkaarsvurdering.vilkaar.some((vilkaar) => !vilkaar.vurdering)

  const slettVilkaarsvurderingResultat = () =>
    slettTotalVurderingCall(behandlingId, (res) => {
      oppdaterVilkaar(res)
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
    })

  const lagreVilkaarsvurderingResultat = () => {
    if (!(svar && [ISvar.JA, ISvar.NEI].includes(svar))) {
      setRadioError('Du må svare på om vilkårsvurderingen er oppfylt')
    } else {
      setRadioError(undefined)
    }

    if (radioError === undefined && svar !== undefined) {
      oppdaterTotalVurderingCall({ behandlingId, resultat: svarTilTotalResultat(svar), kommentar }, (res) => {
        oppdaterVilkaar(res)
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
      })
    }
  }

  const resultatTekst = () => {
    if (erRevurdering && !nySoeknad) {
      return vilkaarsvurdering.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT
        ? 'Fortsatt oppfylt'
        : 'Opphør av ytelse'
    }

    return vilkaarsvurdering.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT
      ? 'Ja, vilkår er oppfylt'
      : 'Nei, vilkår er ikke oppfylt'
  }

  const status = vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT ? 'success' : 'error'
  const virkningstidspunktSamsvarer = virkningstidspunktDato === vilkaarsvurdering.virkningstidspunkt
  const erRevurdering = behandlingstype === IBehandlingsType.REVURDERING
  const nySoeknad = revurderingsaarsak === Revurderingaarsak.NY_SOEKNAD

  return (
    <>
      <Box paddingBlock="12 4" paddingInline="16 14">
        <Heading size="small" level="2">
          {erRevurdering
            ? 'Utfall etter revurdering'
            : `Er vilkårene for ${formaterSakstype(sakstype).toLowerCase()} oppfylt?`}
        </Heading>

        {vilkaarsvurdering.resultat && (
          <VStack gap="2">
            <Box>
              <HStack gap="2" align="center">
                <StatusIcon status={status} />
                <BodyShort textColor="subtle">{resultatTekst()}</BodyShort>
              </HStack>
              {vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT && (
                <BodyShort textColor="subtle">
                  {erRevurdering
                    ? null
                    : `${formaterSakstype(sakstype)} er innvilget f.o.m ${formaterDato(
                        vilkaarsvurdering.virkningstidspunkt
                      )}`}
                </BodyShort>
              )}
            </Box>
            {vilkaarsvurdering?.resultat?.kommentar && (
              <Box>
                <Heading size="xsmall" level="3">
                  Begrunnelse
                </Heading>
                <ResultatKommentar>{vilkaarsvurdering.resultat.kommentar}</ResultatKommentar>
              </Box>
            )}
            <HStack gap="4">
              {redigerbar && (
                <Button
                  loading={isPending(slettVurderingStatus)}
                  onClick={() => {
                    slettVilkaarsvurderingResultat()
                    setRedigerTotalvurdering(false)
                  }}
                  variant="tertiary"
                  size="small"
                  icon={<TrashIcon aria-hidden />}
                >
                  Slett vurdering
                </Button>
              )}
              {redigerbar && (
                <Button
                  onClick={() => {
                    setKommentar(vilkaarsvurdering?.resultat?.kommentar || '')
                    if (vilkaarsvurdering.resultat?.utfall) {
                      setSvar(totalResultatTilSvar(vilkaarsvurdering.resultat?.utfall))
                    }
                    setRedigerTotalvurdering(true)
                    const vilkaarsvurderingUtenResultat = { ...vilkaarsvurdering, resultat: undefined }
                    dispatch(updateVilkaarsvurdering(vilkaarsvurderingUtenResultat))
                  }}
                  variant="tertiary"
                  size="small"
                  icon={<PencilWritingIcon aria-hidden />}
                  loading={isPending(totalVurderingStatus)}
                >
                  Rediger totalvurdering
                </Button>
              )}
            </HStack>
          </VStack>
        )}

        {!vilkaarsvurdering.resultat && !alleVilkaarErVurdert && (
          <BodyShort>Alle vilkår må vurderes før man kan gå videre</BodyShort>
        )}

        {!vilkaarsvurdering.resultat && alleVilkaarErVurdert && (
          <VurderAlleVilkaarBox>
            <VStack gap="4">
              <RadioGroup
                legend=""
                size="small"
                value={svar || ''}
                onChange={(event) => {
                  setSvar(ISvar[event as ISvar])
                  setRadioError(undefined)
                }}
                error={radioError ? radioError : false}
              >
                <Radio value={ISvar.JA}>{erRevurdering ? 'Fortsatt oppfylt' : 'Ja, vilkår er oppfylt'}</Radio>
                <Radio value={ISvar.NEI}>
                  {erRevurdering && !nySoeknad ? 'Opphør av ytelse' : 'Nei, vilkår er ikke oppfylt'}
                </Radio>
              </RadioGroup>
              <Textarea
                label="Begrunnelse"
                placeholder="Valgfritt"
                value={kommentar}
                onChange={(e) => setKommentar(e.target.value)}
                minRows={3}
                size="medium"
                autoComplete="off"
              />
              <Box>
                <Button
                  variant="primary"
                  size="small"
                  onClick={lagreVilkaarsvurderingResultat}
                  loading={isPending(totalVurderingStatus)}
                >
                  Lagre
                </Button>
              </Box>
            </VStack>
          </VurderAlleVilkaarBox>
        )}
      </Box>

      <Box paddingBlock="4 8" paddingInline="16 4">
        {vilkaarsvurdering.resultat && !virkningstidspunktSamsvarer && (
          <OppdatertGrunnlagAlert variant="warning">
            Virkningstidspunktet er endret og vilkårene må da vurderes på nytt. For å starte ny vurdering må du slette
            nåværende vilkårsvurdering
          </OppdatertGrunnlagAlert>
        )}
      </Box>

      <Box paddingBlock="4 0" borderWidth="1 0 0 0" borderColor="border-subtle">
        {redigerbar && vilkaarsvurdering.isGrunnlagUtdatert && (
          <OppdatertGrunnlagAlert variant="warning">
            OBS! Grunnlaget for vilkårsvurderingen har blitt oppdatert siden sist. <br />
            Du må se over vurderingene og sjekke at de fortsatt er riktige.
          </OppdatertGrunnlagAlert>
        )}
        {redigerbar ? <VilkaarsvurderingKnapper behandlingId={behandlingId} /> : <NesteOgTilbake />}
      </Box>
    </>
  )
}

const VurderAlleVilkaarBox = styled(Box)`
  width: 20rem;
`

const ResultatKommentar = styled(BodyShort)`
  white-space: pre-wrap;
`
