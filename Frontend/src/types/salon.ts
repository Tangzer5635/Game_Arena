import type { User } from "./user";

export type EtatSalon = "OUVERT" | "EN_COURS";

export interface Salon {
    id: number;
    code: string;
    etat: EtatSalon;
    users: User[];
}