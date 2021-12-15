export enum VilkaarStatus {
    OPPFYLT = "oppfylt",
    IKKE_OPPFYLT = "ikke oppfylt",
}

export enum Status {
    DONE = "done",
    NOT_DONE = "not done",
}

export interface IVilkaarProps {
    vilkaar: {
        vilkaarDone: Status;
        vilkaarType: string;
        vilkaarStatus: VilkaarStatus;
    };
}
