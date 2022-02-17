export enum VilkaarStatus {
  OPPFYLT = 'oppfylt',
  IKKE_OPPFYLT = 'ikke oppfylt',
}

export enum Status {
  DONE = 'done',
  NOT_DONE = 'not done',
}

export enum OpplysningsType {
  doedsdato = 'avdoed_doedsfall:v1',
  soeker_foedselsdato = 'soeker_foedselsdato:v1',
  avdoedes_forutgaaende_medlemsskap = "avdoedes_forutgaaende_medlemsskap:v1"
}


export interface IVilkaarProps {
  vilkaar: {
    vilkaarDone: Status
    vilkaarType: OpplysningsType
    vilkaarStatus: VilkaarStatus
    grunnlag: any;
    description?: string
  }
}
