export interface Reponse {
    id: number;
    text: string;
    estBonne: boolean;
}

export interface Question {
    id: number;
    text: string;
    reponses: Reponse[];
}

export interface Quiz {
    id: number;
    titre: string;
    description: string;
    questions: Question[];
}