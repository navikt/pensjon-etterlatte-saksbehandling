import styled from "styled-components";
import { Content } from "../../../shared/styled";
import { Status, VilkaarStatus } from "./types";
import { Vilkaar } from "./vilkaar";

export const Inngangsvilkaar = () => {
    return (
        <Content>
            <VilkaarListe>
                <Vilkaar
                    vilkaar={{
                        vilkaarDone: Status.DONE,
                        vilkaarType: "Dødsfall",
                        vilkaarStatus: VilkaarStatus.OPPFYLT,
                    }}
                />
                <Vilkaar
                    vilkaar={{
                        vilkaarDone: Status.NOT_DONE,
                        vilkaarType: "Alder",
                        vilkaarStatus: VilkaarStatus.IKKE_OPPFYLT,
                    }}
                />
                <Vilkaar
                    vilkaar={{
                        vilkaarDone: Status.DONE,
                        vilkaarType: "Foreldreansvar",
                        vilkaarStatus: VilkaarStatus.OPPFYLT,
                    }}
                />
                <Vilkaar
                    vilkaar={{
                        vilkaarDone: Status.DONE,
                        vilkaarType: "Familieforhold",
                        vilkaarStatus: VilkaarStatus.IKKE_OPPFYLT,
                    }}
                />
                <Vilkaar
                    vilkaar={{
                        vilkaarDone: Status.NOT_DONE,
                        vilkaarType: "Bostedsadresse",
                        vilkaarStatus: VilkaarStatus.IKKE_OPPFYLT,
                    }}
                />
                <Vilkaar
                    vilkaar={{
                        vilkaarDone: Status.DONE,
                        vilkaarType: "Yrkesskade",
                        vilkaarStatus: VilkaarStatus.IKKE_OPPFYLT,
                    }}
                />
                <Vilkaar
                    vilkaar={{
                        vilkaarDone: Status.DONE,
                        vilkaarType: "Medlemsskap",
                        vilkaarStatus: VilkaarStatus.OPPFYLT,
                    }}
                />
            </VilkaarListe>
        </Content>
    );
};

const VilkaarListe = styled.div`
    padding: 0 2em;
`;
