import React, { useState } from 'react'
import { Button, VStack } from '@navikt/ds-react'
import { SandboxIcon } from '@navikt/aksel-icons'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useAppDispatch } from '~store/Store'
import { oppdaterBehandlingsstatus, updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import {
  IVilkaarsvurdering,
  oppdaterTotalVurdering,
  VilkaarsvurderingResultat,
  VurderingsResultat,
  vurderVilkaar,
} from '~shared/api/vilkaarsvurdering'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'

/**
 * Kun for bruk i dev: fyller ut alle vilkår som oppfylt med en fast begrunnelse.
 */
export const FyllUtVilkaarsvurderingIDev = ({
  behandlingId,
  vilkaarsvurdering,
}: {
  behandlingId: string
  vilkaarsvurdering: IVilkaarsvurdering
}) => {
  const dispatch = useAppDispatch()
  const [lagrer, setLagrer] = useState(false)
  const [feilmelding, setFeilmelding] = useState<string | null>(null)

  const fyllUtAlleVilkaar = async () => {
    setLagrer(true)
    setFeilmelding(null)

    for (const vilkaar of vilkaarsvurdering.vilkaar) {
      const res = await vurderVilkaar({
        behandlingId,
        request: {
          vilkaarId: vilkaar.id,
          hovedvilkaar: {
            type: vilkaar.hovedvilkaar.type,
            resultat: VurderingsResultat.OPPFYLT,
          },
          kommentar: 'Begrunnelse',
        },
      })

      if (!res.ok) {
        setFeilmelding(res.detail || 'Klarte ikke å oppdatere vilkår')
        setLagrer(false)
        return
      }

      dispatch(updateVilkaarsvurdering(res.data))
    }

    const totalVurderingRes = await oppdaterTotalVurdering({
      behandlingId,
      resultat: VilkaarsvurderingResultat.OPPFYLT,
      kommentar: 'Begrunnelse',
    })

    if (!totalVurderingRes.ok) {
      setFeilmelding(totalVurderingRes.detail || 'Klarte ikke å oppdatere totalvurdering')
      setLagrer(false)
      return
    }

    dispatch(updateVilkaarsvurdering(totalVurderingRes.data))
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))

    setLagrer(false)
  }

  return (
    <VStack gap="space-8" align="end" style={{ marginInline: '0 4rem' }}>
      <Button
        variant="primary"
        size="small"
        icon={<SandboxIcon aria-hidden />}
        loading={lagrer}
        onClick={fyllUtAlleVilkaar}
      >
        Fyll ut vilkårsvurdering (kun i dev)
      </Button>
      {feilmelding && <ApiErrorAlert>{feilmelding}</ApiErrorAlert>}
    </VStack>
  )
}
