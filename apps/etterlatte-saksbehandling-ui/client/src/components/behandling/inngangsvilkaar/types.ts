export enum VilkaarStatus {
  OPPFYLT = 'oppfylt',
  IKKE_OPPFYLT = 'ikke oppfylt',
}

export enum Status {
  DONE = 'done',
  NOT_DONE = 'not done',
}

export enum VilkaarType {
  doedsdato = 'avdoed_doedsfall:v1',
  soeker_foedselsdato = 'soeker_foedselsdato:v1',
}

export interface IVilkaarProps {
  vilkaar: {
    vilkaarDone: Status
    vilkaarType: VilkaarType
    vilkaarStatus: VilkaarStatus
    description?: string
  }
}
