import { describe, expect, it } from 'vitest'
import { soeknadsoversiktErFerdigUtfylt } from '~components/behandling/felles/utils'
import {
  IBehandlingStatus,
  IBehandlingsType,
  IBoddEllerArbeidetUtlandet,
  IDetaljertBehandling,
  IGyldighetResultat,
  IKommerBarnetTilgode,
  Vedtaksloesning,
  Virkningstidspunkt,
} from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { JaNei } from '~shared/types/ISvar'
import { KildeType } from '~shared/types/kilde'

describe('BARNEPENSJON: utfylt søknad er gyldig', () => {
  it('barnepensjon gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.JA),
      mockVirkningstidspunkt(),
      mockBoddellerArbeidetIUtlandet(),
      mockGyldighetsprøving()
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeTruthy()
  })

  it('barnepensjon hvor kommerbarnettilgode=nei er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.NEI),
      mockVirkningstidspunkt(),
      mockBoddellerArbeidetIUtlandet(),
      mockGyldighetsprøving()
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten kommerbarnettilgode er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      null,
      mockVirkningstidspunkt(),
      mockBoddellerArbeidetIUtlandet(),
      mockGyldighetsprøving()
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten virkningstidspunkt er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.JA),
      null,
      mockBoddellerArbeidetIUtlandet(),
      mockGyldighetsprøving()
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten gyldighetsprøving er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.JA),
      mockVirkningstidspunkt(),
      mockBoddellerArbeidetIUtlandet(),
      undefined
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })
})

describe('OMSTILLINGSSTOENAD: utfylt søknad er gyldig', () => {
  it('omstillingsstoenad gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.OMSTILLINGSSTOENAD,
      null,
      mockVirkningstidspunkt(),
      mockBoddellerArbeidetIUtlandet(),
      mockGyldighetsprøving()
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeTruthy()
  })

  it('omstillingsstoenad uten virkningstidspunkt er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.OMSTILLINGSSTOENAD,
      null,
      null,
      mockBoddellerArbeidetIUtlandet(),
      mockGyldighetsprøving()
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('omstillingsstoenad uten gyldighetsprøving er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.OMSTILLINGSSTOENAD,
      null,
      mockVirkningstidspunkt(),
      mockBoddellerArbeidetIUtlandet(),
      undefined
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('omstillingsstoenad uten boddellerarbeidetiutlandet er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.OMSTILLINGSSTOENAD,
      null,
      mockVirkningstidspunkt(),
      null,
      mockGyldighetsprøving()
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })

  it('barnepensjon uten boddellerarbeidetiutlandet er ikke gyldig utfylt', () => {
    const behandling = opprettBehandling(
      SakType.BARNEPENSJON,
      mockKommerBarnetTilgode(JaNei.JA),
      mockVirkningstidspunkt(),
      null,
      mockGyldighetsprøving()
    )

    const utfylt = soeknadsoversiktErFerdigUtfylt(behandling)

    expect(utfylt).toBeFalsy()
  })
})

const opprettBehandling = (
  sakType: SakType,
  kommerBarnetTilgode: IKommerBarnetTilgode | null,
  virkningstidspunkt: Virkningstidspunkt | null,
  boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null,
  gyldighetsprøving?: IGyldighetResultat
): IDetaljertBehandling => {
  return {
    id: 'id',
    sakId: 1,
    sakType,
    sakEnhetId: '4808',
    gyldighetsprøving,
    kommerBarnetTilgode,
    soeknadMottattDato: '01-01-2023',
    virkningstidspunkt,
    boddEllerArbeidetUtlandet,
    status: IBehandlingStatus.OPPRETTET,
    hendelser: [],
    behandlingType: IBehandlingsType.FØRSTEGANGSBEHANDLING,
    revurderingsaarsak: null,
    revurderinginfo: null,
    begrunnelse: null,
    utlandstilknytning: null,
    kilde: Vedtaksloesning.GJENNY,
    sendeBrev: true,
    viderefoertOpphoer: null,
  }
}

const mockBoddellerArbeidetIUtlandet = (): IBoddEllerArbeidetUtlandet => {
  return {
    boddEllerArbeidetUtlandet: false,
    kilde: {
      type: KildeType.saksbehandler,
      tidspunkt: '01-01-2023',
      ident: 'Z123456',
    },
    begrunnelse: 'begrunelse',
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
    kravdato: null,
  }
}
