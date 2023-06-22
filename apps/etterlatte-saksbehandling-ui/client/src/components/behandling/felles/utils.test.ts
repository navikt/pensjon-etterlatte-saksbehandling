import { describe, expect, it } from 'vitest'
import { behandlingErUtfylt, behandlingSkalSendeBrev } from '~components/behandling/felles/utils'
import {
  IBehandlingStatus,
  IBehandlingsType,
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  IProsesstype,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { JaNei } from '~shared/types/ISvar'
import { KildeType } from '~shared/types/kilde'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'

describe('BARNEPENSJON: utfylt søknad er gyldig', () => {
  it('barnepensjon gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.JA),
      mockVirkningstidspunkt(),
      mockGyldighetsprøving()
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeTruthy()
  })

  it('barnepensjon hvor kommerbarnettilgode=nei er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.NEI),
      mockVirkningstidspunkt(),
      mockGyldighetsprøving()
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten kommerbarnettilgode er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(SakType.BARNEPENSJON, null, mockVirkningstidspunkt(), mockGyldighetsprøving())

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten virkningstidspunkt er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.JA),
      null,
      mockGyldighetsprøving()
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten gyldighetsprøving er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.JA),
      mockVirkningstidspunkt(),
      undefined
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })
})

describe('OMSTILLINGSSTOENAD: utfylt søknad er gyldig', () => {
  it('omstillingsstoenad gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.OMSTILLINGSSTOENAD,
      null,
      mockVirkningstidspunkt(),
      mockGyldighetsprøving()
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeTruthy()
  })

  it('omstillingsstoenad uten virkningstidspunkt er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(SakType.OMSTILLINGSSTOENAD, null, null, mockGyldighetsprøving())

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('omstillingsstoenad uten gyldighetsprøving er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(SakType.OMSTILLINGSSTOENAD, null, mockVirkningstidspunkt(), undefined)

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })
})

describe('behandlingSkalSendeBrev', () => {
  const behandling = opprettBehandling(SakType.BARNEPENSJON, null, null, undefined)
  const revurdering = {
    ...behandling,
    behandlingType: IBehandlingsType.REVURDERING,
    revurderingsaarsak: Revurderingsaarsak.SOESKENJUSTERING,
  }
  const regulering = {
    ...revurdering,
    revurderingsaarsak: Revurderingsaarsak.REGULERING,
  }
  const manueltopphoer = { ...behandling, behandlingType: IBehandlingsType.MANUELT_OPPHOER }

  it('skal gi false for regulering og manuelt opphør', () => {
    expect(behandlingSkalSendeBrev(regulering)).toBeFalsy()
    expect(behandlingSkalSendeBrev(manueltopphoer)).toBeFalsy()
  })

  it('skal gi true for foerstegangsbehandling og revurderinger som ikke er regulering', () => {
    expect(behandlingSkalSendeBrev(behandling)).toBeTruthy()
    expect(behandlingSkalSendeBrev(revurdering)).toBeTruthy()
  })
})

const opprettBehandling = (
  sakType: SakType,
  kommerBarnetTilgode: IKommerBarnetTilgode | null,
  virkningstidspunkt: Virkningstidspunkt | null,
  gyldighetsprøving?: IGyldighetResultat
): IDetaljertBehandling => {
  return {
    id: 'id',
    sak: 1,
    sakType,
    gyldighetsprøving,
    kommerBarnetTilgode,
    soeknadMottattDato: '01-01-2023',
    virkningstidspunkt,
    utenlandstilsnitt: undefined,
    boddEllerArbeidetUtlandet: undefined,
    status: IBehandlingStatus.OPPRETTET,
    hendelser: [],
    behandlingType: IBehandlingsType.FØRSTEGANGSBEHANDLING,
    prosesstype: IProsesstype.MANUELL,
    revurderingsaarsak: null,
    revurderinginfo: null,
  }
}

const mockGyldighetsprøving = () => {
  return {
    resultat: VurderingsResultat.OPPFYLT,
    vurderinger: [],
    vurdertDato: '01-01-2023',
  }
}

const mockKommerBarnetTilgode = (svar: JaNei): IKommerBarnetTilgode => {
  return {
    svar,
    begrunnelse: 'en kort begrunnelse',
    kilde: {
      type: KildeType.saksbehandler,
      tidspunkt: '010101',
      ident: 'Z123456',
    },
  }
}

const mockVirkningstidspunkt = (): Virkningstidspunkt => {
  return {
    dato: '',
    kilde: {
      type: KildeType.saksbehandler,
      tidspunkt: '010101',
      ident: 'Z123456',
    },
    begrunnelse: '',
  }
}
