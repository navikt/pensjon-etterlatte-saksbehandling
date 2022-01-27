import { useState } from "react";
import { NavLink, Routes, Route } from "react-router-dom";
import styled from "styled-components";
import { ClockIcon } from "../../shared/icons/clockIcon";
import { DialogIcon } from "../../shared/icons/dialogIcon";
import { FolderIcon } from "../../shared/icons/folderIcon";
import { Column, GridContainer } from "../../shared/styled";
import { BehandlingsStatus, IBehandlingsStatus } from "./behandlings-status";
import { Beregne } from "./beregne";
import { Brev } from "./brev";
import { Inngangsvilkaar } from "./inngangsvilkaar";
import { Personopplysninger } from "./personopplysninger";
import { Utbetalingsoversikt } from "./utbetalingsoversikt";
import { Vedtak } from "./vedtak";

export const Behandling = () => {
    return (
        <>
            <GridContainer>
                <Column>
                    <MenuHead>
                        <BehandlingsStatus status={IBehandlingsStatus.FORSTEGANG} />
                    </MenuHead>
                    {/* stegmeny */}
                    <StegMeny>
                        <li>
                            <NavLink to="personopplysninger"><span>1.</span> Personopplysninger</NavLink>
                        </li>
                        <li>
                            <NavLink to="inngangsvilkaar"><span>2.</span> Inngangsvilkår</NavLink>
                        </li>
                        <li>
                            <NavLink to="beregne"><span>3.</span> Beregne</NavLink>
                        </li>
                        <li>
                            <NavLink to="vedtak"><span>4.</span> Vedtak</NavLink>
                        </li>
                        <li>
                            <NavLink to="utbetalingsoversikt"><span>5.</span> Utbetalingsoversikt</NavLink>
                        </li>
                        <li>
                            <NavLink to="brev"><span>6.</span> Brev</NavLink>
                        </li>
                    </StegMeny>
                    {/* Subroutes for stegmeny feks */}
                </Column>
                <Column>
                    <Routes>
                        <Route path="personopplysninger" element={<Personopplysninger />} />
                        <Route path="inngangsvilkaar" element={<Inngangsvilkaar />} />
                        <Route path="beregne" element={<Beregne />} />
                        <Route path="vedtak" element={<Vedtak />} />
                        <Route path="utbetalingsoversikt" element={<Utbetalingsoversikt />} />
                        <Route path="brev" element={<Brev />} />
                    </Routes>
                </Column>
                <Column>
                    <Tab />
                </Column>
            </GridContainer>
        </>
    );
};

const Tab = () => {
    const [selected, setSelected] = useState("1");

    const select = (e: any) => {
        setSelected(e.currentTarget.dataset.value);
    };

    const isSelectedClass = (val: string): string => {
        if (selected === val) {
            return "active";
        }
        return "";
    };

    const renderSubElement = () => {
        switch (selected) {
            case "1":
                return <div>Hei</div>;
            case "2":
                return <div>på</div>;
            case "3":
                return <div>deg</div>;
        }
        return;
    };

    return (
        <>
            <MenuHead style={{ paddingTop: "14px", paddingLeft: 0, paddingRight: 0 }}>
                <Tabs>
                    <IconButton data-value="1" onClick={select} className={isSelectedClass("1")}>
                        <ClockIcon />
                    </IconButton>
                    <IconButton data-value="2" onClick={select} className={isSelectedClass("2")}>
                        <DialogIcon />
                    </IconButton>
                    <IconButton data-value="3" onClick={select} className={isSelectedClass("3")}>
                        <FolderIcon />
                    </IconButton>
                </Tabs>
            </MenuHead>
            {renderSubElement()}
        </>
    );
};

const Tabs = styled.div`
    display: flex;

    > div {
        width: 33.333%;
        height: 100%;
        justify-content: center;
        align-items: center;
        text-align: center;
    }
`;
const IconButton = styled.div`
    cursor: pointer;
    padding: 1em 1em 1.8em;

    &.active {
        border-bottom: 3px solid #0067c5;
    }
`;

const StegMeny = styled.ul`
    height: 100px;
    list-style: none;
    padding: 1em 0em 0;

    li {
        a {
            * {
                font-weight: bold;
                padding-right: 1em;
                padding-left: 1em;
            }
            display: block;
            padding: 0.5em 1em 0.5em;
            color: #78706a;
            text-decoration: none;
            border-bottom: 3px solid transparent;
            border-left: 6px solid transparent;
            &:hover {
                text-decoration: underline;
            }
            &.active {
                border-left: 6px solid #0067c5;
                background-color: #E7E9E9;
            }
        }
    }
`;

const MenuHead = styled.div`
    padding: 2em 1em;
    height: 100px;
    border-bottom: 1px solid #ccc;
`;
