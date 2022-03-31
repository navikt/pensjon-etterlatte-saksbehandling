export const handlinger = {
  START: { id: 'start', navn: 'Start vilkårsvurdering' },
  NESTE: { id: 'neste', navn: 'Neste' },
  TILBAKE: { id: 'tilbake', navn: 'Tilbake' },

  VILKAARSVURDERING: {
    BEREGNE: { id: 'beregne', navn: 'Beregne og fatte vedtak' },
    AVSLAG: { id: 'avslag', navn: 'Gå til avslag' },
    VENT: { id: 'vent', navn: 'Sett saken på vent' },
  },
  ATTESTERING: { id: 'beregne', navn: 'Send til attestering' },
  AVBRYT: { id: 'avbryt', navn: 'Avbryt behandling' },

  AVBRYT_MODAL: {
    JA: { id: 'ja', navn: 'Ja, avbryt behandling' },
    NEI: { id: 'nei', navn: 'Nei, fortsett behandling' },
  },

  ATTESTERING_MODAL: {
    JA: { id: 'ja', navn: 'Ja, send til attestering' },
    NEI: { id: 'nei', navn: 'Nei, avbryt' },
  },
}
