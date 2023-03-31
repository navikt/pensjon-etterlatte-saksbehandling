import { describe, expect, it } from 'vitest'
import { behandlingErUtfylt } from '~components/behandling/felles/utils'
import {
  IBehandlingStatus,
  IBehandlingsType,
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  IProsesstype,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { ISaksType } from '~components/behandling/fargetags/saksType'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { JaNei } from '~shared/types/ISvar'
import { KildeType } from '~shared/types/kilde'

describe('BARNEPENSJON: utfylt søknad er gyldig', () => {
  it('barnepensjon gyldig utfylt', () => {
    const behandling = opprettBehandling(
      ISaksType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.JA),
      mockVirkningstidspunkt(),
      mockGyldighetsprøving()
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeTruthy()
  })

  it('barnepensjon hvor kommerbarnettilgode=nei er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      ISaksType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.NEI),
      mockVirkningstidspunkt(),
      mockGyldighetsprøving()
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten kommerbarnettilgode er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      ISaksType.BARNEPENSJON,
      null,
      mockVirkningstidspunkt(),
      mockGyldighetsprøving()
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten virkningstidspunkt er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      ISaksType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.JA),
      null,
      mockGyldighetsprøving()
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten gyldighetsprøving er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      ISaksType.BARNEPENSJON,
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
      ISaksType.OMSTILLINGSSTOENAD,
      null,
      mockVirkningstidspunkt(),
      mockGyldighetsprøving()
    )

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeTruthy()
  })

  it('omstillingsstoenad uten virkningstidspunkt er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(ISaksType.OMSTILLINGSSTOENAD, null, null, mockGyldighetsprøving())

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('omstillingsstoenad uten gyldighetsprøving er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(ISaksType.OMSTILLINGSSTOENAD, null, mockVirkningstidspunkt(), undefined)

    const utfylt = behandlingErUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })
})

const opprettBehandling = (
  sakType: ISaksType,
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
    saksbehandlerId: 'Z123456',
    datoFattet: '',
    datoAttestert: '',
    soeknadMottattDato: '01-01-2023',
    virkningstidspunkt,
    status: IBehandlingStatus.OPPRETTET,
    hendelser: [],
    behandlingType: IBehandlingsType.FØRSTEGANGSBEHANDLING,
    prosesstype: IProsesstype.MANUELL,
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
