export const handlinger = {
  START: { id: 'start', navn: 'Start vilkårsvurdering' },
  NESTE: { id: 'neste', navn: 'Neste side' },
  TILBAKE: { id: 'tilbake', navn: 'Tilbake' },

  VILKAARSVURDERING: {
    BEREGNE: { id: 'beregne', navn: 'Gå videre' },
    AVSLAG: { id: 'avslag', navn: 'Avslå og gå til brev' },
    VENT: { id: 'vent', navn: 'Sett saken på vent' },
    GJENNOPPTA: { id: 'vent', navn: 'Gjennoppta saken' },
    OPPHOER: { id: 'opphoer', navn: 'Gå til beregning' },
  },
  ATTESTERING: { id: 'beregne', navn: 'Send til attestering' },
  AVBRYT: { id: 'avbryt', navn: 'Avbryt behandling og annuller saken' },
  AVBRYT_REVURDERING: { id: 'avbryt_revurdering', navn: 'Avbryt revurdering' },

  AVBRYT_MODAL: {
    JA: { id: 'ja', navn: 'Ja, avbryt behandling' },
    NEI: { id: 'nei', navn: 'Nei, fortsett behandling' },
  },

  ATTESTERING_MODAL: {
    JA: { id: 'ja', navn: 'Ja, send til attestering' },
    NEI: { id: 'nei', navn: 'Nei, avbryt' },
  },
}
