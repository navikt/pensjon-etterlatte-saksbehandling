export enum VilkaarStatus {
    OPPFYLT = "oppfylt",
    IKKE_OPPFYLT = "ikke oppfylt",
}

export enum Status {
    DONE = "done",
    NOT_DONE = "not done",
}

export enum VilkaarType{
    doedsfall = "doedsfall",
    doedsdato = "doedsdato",
    alderBarn = "alder_barn"
}

export interface IVilkaarProps {
    vilkaar: {
        vilkaarDone: Status;
        vilkaarType: VilkaarType;
        vilkaarStatus: VilkaarStatus;
        description?: string;
    };
}
