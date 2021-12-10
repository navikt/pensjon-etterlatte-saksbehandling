import { NavLink, Routes, Route } from "react-router-dom";
import styled from "styled-components";
import { Column, GridContainer } from "../../shared/styled";
import { Beregne } from "./beregne";
import { Brev } from "./brev";
import { Inngangsvilkaar } from "./inngangsvilkaar";
import { Personopplysninger } from "./personopplysninger";
import { Utbetalingsoversikt } from "./utbetalingsoversikt";
import { Vedtak } from "./vedtak";

export const Behandling = () => {
    return (
        <GridContainer>
            <Column>meny</Column>
            <Column>
                {/* stegmeny */}
                <StegMeny>
                    <li>
                        <NavLink to="personopplysninger">Personopplysninger</NavLink>
                    </li>
                    <li>
                        <NavLink to="inngangsvilkaar">Inngangsvilkår</NavLink>
                    </li>
                    <li>
                        <NavLink to="beregne">Beregne</NavLink>
                    </li>
                    <li>
                        <NavLink to="vedtak">Vedtak</NavLink>
                    </li>
                    <li>
                        <NavLink to="utbetalingsoversikt">Utbetalingsoversikt</NavLink>
                    </li>
                    <li>
                        <NavLink to="brev">Brev</NavLink>
                    </li>
                </StegMeny>

                {/* Subroutes for stegmeny feks */}
                <Routes>
                    <Route
                        path="personopplysninger"
                        element={<Personopplysninger />}
                    />
                    <Route
                        path="inngangsvilkaar"
                        element={<Inngangsvilkaar />}
                    />
                    <Route
                        path="beregne"
                        element={<Beregne />}
                    />
                    <Route
                        path="vedtak"
                        element={<Vedtak />}
                    />
                    <Route
                        path="utbetalingsoversikt"
                        element={<Utbetalingsoversikt />}
                    />
                    <Route
                        path="brev"
                        element={<Brev />}
                    />
                </Routes>
            </Column>
            <Column>Historikk</Column>
        </GridContainer>
    );
};

const StegMeny = styled.ul`
    height: 150px;
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    list-style: none;
    border-bottom: 1px solid #c6c2bf;
    padding: 1em 1em 0;

    li {
        a {
            display: block;
            padding: 1em 1em 2em;
            color: #78706a;
            text-decoration: none;
            border-bottom: 3px solid transparent;
            &:hover,&.active {
                color: #0067C5;
                border-bottom: 3px solid #0067C5;
            }
        }
    }
`;
