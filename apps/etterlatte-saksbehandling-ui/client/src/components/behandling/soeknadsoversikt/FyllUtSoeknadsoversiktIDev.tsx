import React, { useState } from 'react'
import { Button } from '@navikt/ds-react'
import { SandboxIcon } from '@navikt/aksel-icons'
import { useAppDispatch } from '~store/Store'
import {
  oppdaterBehandlingsstatus,
  oppdaterBoddEllerArbeidetUtlandet,
  oppdaterGyldighetsproeving,
  oppdaterKommerBarnetTilgode,
  oppdaterUtlandstilknytning,
  oppdaterVirkningstidspunkt,
} from '~store/reducers/BehandlingReducer'
import {
  fastsettVirkningstidspunkt,
  lagreBegrunnelseKommerBarnetTilgode,
  lagreBoddEllerArbeidetUtlandet,
  lagreGyldighetsproeving,
  lagreUtlandstilknytning,
} from '~shared/api/behandling'
import { IBehandlingStatus, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { JaNei } from '~shared/types/ISvar'
import { SakType } from '~shared/types/sak'
import { ApiErrorAlert } from '~ErrorBoundary'

const BEGRUNNELSE = 'Begrunnelse'

/**
 * Kun for bruk i dev: fyller ut søknadsoversikten med "normale" som f.eks svar (nasjonal sak, gyldig fremsatt,
 * virkningstidspunkt satt til inneværende måned, ikke bodd/arbeidet i utlandet).
 */
export const FyllUtSoeknadsoversiktIDev = ({ behandlingId, sakType }: { behandlingId: string; sakType: SakType }) => {
  const dispatch = useAppDispatch()
  const [lagrer, setLagrer] = useState(false)
  const [feilmelding, setFeilmelding] = useState<string | null>(null)

  const fyllUtSoeknadsoversikt = async () => {
    setLagrer(true)
    setFeilmelding(null)

    const utlandstilknytningRes = await lagreUtlandstilknytning({
      behandlingId,
      svar: UtlandstilknytningType.NASJONAL,
      begrunnelse: BEGRUNNELSE,
    })
    if (!utlandstilknytningRes.ok) {
      setFeilmelding(utlandstilknytningRes.detail || 'Klarte ikke å lagre utlandstilknytning')
      setLagrer(false)
      return
    }
    dispatch(oppdaterUtlandstilknytning(utlandstilknytningRes.data))
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))

    const gyldighetsproevingRes = await lagreGyldighetsproeving({
      behandlingId,
      svar: JaNei.JA,
      begrunnelse: BEGRUNNELSE,
    })
    if (!gyldighetsproevingRes.ok) {
      setFeilmelding(gyldighetsproevingRes.detail || 'Klarte ikke å lagre gyldig fremsatt søknad')
      setLagrer(false)
      return
    }
    dispatch(oppdaterGyldighetsproeving(gyldighetsproevingRes.data))
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))

    const naa = new Date()
    const inneværendeMaaned = new Date(naa.getFullYear(), naa.getMonth(), 1)
    const virkningstidspunktRes = await fastsettVirkningstidspunkt({
      id: behandlingId,
      dato: inneværendeMaaned,
      begrunnelse: BEGRUNNELSE,
      kravdato: null,
      overstyr: false,
    })
    if (!virkningstidspunktRes.ok) {
      setFeilmelding(virkningstidspunktRes.detail || 'Klarte ikke å fastsette virkningstidspunkt')
      setLagrer(false)
      return
    }
    dispatch(oppdaterVirkningstidspunkt(virkningstidspunktRes.data))
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))

    const boddEllerArbeidetUtlandetRes = await lagreBoddEllerArbeidetUtlandet({
      behandlingId,
      begrunnelse: BEGRUNNELSE,
      boddEllerArbeidetUtlandet: false,
      boddArbeidetIkkeEosEllerAvtaleland: false,
      boddArbeidetEosNordiskKonvensjon: false,
      boddArbeidetAvtaleland: false,
      vurdereAvdoedesTrygdeavtale: false,
      skalSendeKravpakke: false,
    })
    if (!boddEllerArbeidetUtlandetRes.ok) {
      setFeilmelding(boddEllerArbeidetUtlandetRes.detail || 'Klarte ikke å lagre bodd eller arbeidet i utlandet')
      setLagrer(false)
      return
    }
    dispatch(oppdaterBoddEllerArbeidetUtlandet(boddEllerArbeidetUtlandetRes.data))
    dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))

    if (sakType === SakType.BARNEPENSJON) {
      const kommerBarnetTilgodeRes = await lagreBegrunnelseKommerBarnetTilgode({
        behandlingId,
        svar: JaNei.JA,
        begrunnelse: BEGRUNNELSE,
      })
      if (!kommerBarnetTilgodeRes.ok) {
        setFeilmelding(kommerBarnetTilgodeRes.detail || 'Klarte ikke å lagre kommer barnet tilgode')
        setLagrer(false)
        return
      }
      dispatch(oppdaterKommerBarnetTilgode(kommerBarnetTilgodeRes.data))
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
    }

    setLagrer(false)
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'flex-end', marginInline: '0 4rem' }}>
      <Button
        variant="primary"
        size="small"
        icon={<SandboxIcon aria-hidden />}
        loading={lagrer}
        onClick={fyllUtSoeknadsoversikt}
      >
        Fyll ut søknadsoversikt (kun i dev)
      </Button>
      {feilmelding && <ApiErrorAlert>{feilmelding}</ApiErrorAlert>}
    </div>
  )
}
