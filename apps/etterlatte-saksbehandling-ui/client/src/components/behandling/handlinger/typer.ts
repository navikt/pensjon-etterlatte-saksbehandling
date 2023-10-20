export const handlinger = {
  START: { id: 'start', navn: 'Start vilkårsvurdering' },
  NESTE: { id: 'neste', navn: 'Neste side' },
  TILBAKE: { id: 'tilbake', navn: 'Tilbake' },

  VILKAARSVURDERING: {
    BEREGNE: { id: 'beregne', navn: 'Gå videre' },
    AVSLAG: { id: 'avslag', navn: 'Avslå og gå til brev' },
    VENT: { id: 'vent', navn: 'Sett saken på vent' },
    GJENNOPPTA: { id: 'vent', navn: 'Gjennoppta saken' },
    OPPHOER: { id: 'opphoer', navn: 'Opphør og gå til brev' },
    OPPHOER_FATT_VEDTAK: { id: 'opphoer', navn: 'Fatt vedtak' },
  },
  ATTESTERING: { id: 'beregne', navn: 'Send til attestering' },

  ATTESTERING_MODAL: {
    JA: { id: 'ja', navn: 'Ja, send til attestering' },
    NEI: { id: 'nei', navn: 'Nei, avbryt' },
  },
}
