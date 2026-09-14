import type { User } from "./user";

export type EtatSalon = "OUVERT" | "EN_COURS" | "TERMINE";

export interface Salon {
    id: number;
    code: string;
    createurId: number;
    etat: EtatSalon;
    quizId: number | null;
    maxPlayers: number;
    users: User[];
}