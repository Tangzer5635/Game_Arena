import api from "./api";

export interface UserStats {
    userId: number;
    username: string;
    partiesJouees: number;
    scoreCumule: number;
    meilleurScore: number;
    bonnesReponses: number;
    mauvaisesReponses: number;
    tauxReussitePct: number;
    meilleureStreak: number;
}

export const getUserStats = async (userId: number): Promise<UserStats> => {
    const res = await api.get<UserStats>(`/users/${userId}/stats/`);
    return res.data;
};
