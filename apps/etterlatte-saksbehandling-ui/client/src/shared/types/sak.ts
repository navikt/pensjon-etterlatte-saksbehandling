export interface ISak {
  id: number
  ident: string
  sakType: SakType
  enhet: string
}

export enum SakType {
  BARNEPENSJON = 'BARNEPENSJON',
  OMSTILLINGSSTOENAD = 'OMSTILLINGSSTOENAD',
}
