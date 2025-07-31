export const sendHendelseTilUmami = (navn: string, data: object) => {
  if (typeof window !== 'undefined' && window.umami) {
    window.umami.track(navn, data)
  }
}
